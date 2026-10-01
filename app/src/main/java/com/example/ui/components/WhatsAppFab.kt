package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.example.ui.activities.SupportWebActivity
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActionButtonConfig
import com.example.data.repository.FirebaseRestHelper
import com.example.ui.theme.WhatsAppGreen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun WhatsAppFab(
    modifier: Modifier = Modifier,
    config: ActionButtonConfig = ActionButtonConfig(),
    customMessage: String = "السلام عليكم إدارة تطبيق أطباء زمار، أود الاستفسار بخصوص الدليل أو طلب إضافة طبيب..."
) {
    val context = LocalContext.current

    // Dynamic Visibility based on Active/Inactive state
    if (!config.isActive) {
        return
    }

    val actionInput = config.effectiveInput
    val isPhone = remember(actionInput, config.actionType) {
        if (actionInput.isBlank()) false else isPhoneNumber(actionInput, config.actionType)
    }
    val isWhatsapp = remember(actionInput, config.actionType) {
        config.actionType.equals("WHATSAPP", ignoreCase = true) || actionInput.contains("wa.me")
    }

    val icon = when {
        isPhone -> Icons.Default.Call
        else -> Icons.AutoMirrored.Filled.Chat
    }

    val desc = when {
        isPhone -> "اتصال مباشر بالإدارة"
        isWhatsapp -> "تواصل مع الإدارة عبر واتساب"
        else -> "المحادثة الفورية والدعم الفني (Crisp Chat)"
    }

    FloatingActionButton(
        onClick = {
            if (isPhone && actionInput.isNotBlank()) {
                val cleanNumber = actionInput.filter { it.isDigit() || it == '+' }
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")))
                } catch (_: Exception) {}
            } else {
                val targetUrl = com.example.util.SupportChatManager.resolveDynamicChatUrl(
                    actionInput.ifBlank { com.example.util.SupportChatManager.cachedFabUrl }
                )
                com.example.util.SupportChatManager.openInAppChat(context, targetUrl)
            }
        },
        shape = CircleShape,
        containerColor = WhatsAppGreen,
        contentColor = Color.White,
        modifier = modifier
            .size(56.dp)
            .testTag("whatsapp_admin_fab")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Universal Smart Action Dispatcher with Dynamic On-Click Fetch from Firebase Realtime Database:
 * 1. Checks given actionInput or fetches latest value from Firebase RTDB (/app_settings, /settings, /sponsor_banner).
 * 2. If actionValue starts with "http://" or "https://", launch in-app Crisp Chat / Custom Tabs.
 * 3. If actionValue is numeric/phone, launch Intent.ACTION_DIAL (Phone Dialer).
 * 4. No hardcoded fallback phone number is ever used.
 */
fun executeSmartAction(
    context: Context,
    actionInput: String? = null,
    actionType: String = "AUTO",
    defaultMessage: String = "السلام عليكم إدارة تطبيق أطباء زمار...",
    targetKey: String = "contact_us"
) {
    val currentInput = actionInput?.trim().orEmpty()

    if (currentInput.isNotBlank()) {
        routeDynamicAction(context, currentInput, actionType, defaultMessage)
        return
    }

    // If initial string is empty, dynamically query Firebase RTDB on click
    CoroutineScope(Dispatchers.Main).launch {
        var fetchedValue: String? = null

        // 1. Try direct RTDB snapshot read
        try {
            fetchedValue = fetchLatestActionFromFirebase(targetKey)
        } catch (e: Exception) {
            // fallback to REST
        }

        // 2. Try REST fallback if needed
        if (fetchedValue.isNullOrBlank()) {
            try {
                fetchedValue = FirebaseRestHelper.fetchLatestActionValue(targetKey)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val finalValue = fetchedValue?.trim().orEmpty()
        if (finalValue.isNotBlank()) {
            routeDynamicAction(context, finalValue, actionType, defaultMessage)
        } else {
            // Default to Crisp chat interface in in-app Custom Tabs / WebView
            val dynamicUrl = com.example.util.SupportChatManager.cachedCrispUrl
            com.example.util.SupportChatManager.openInAppChat(context, dynamicUrl)
        }
    }
}

/**
 * Opens web links inside in-app Custom Tabs or SupportWebActivity (with full JS, DOM Storage & Database enabled)
 */
fun openWebUrl(context: Context, url: String) {
    com.example.util.SupportChatManager.openInAppChat(context, url)
}

/**
 * Core Dynamic Router:
 * - "http://" or "https://" -> in-app WebView / Custom Tabs (Crisp Chat / Web link)
 * - numeric / phone -> Intent.ACTION_DIAL (Phone Dialer)
 * - "wa.me" -> External WhatsApp or in-app web fallback
 */
fun routeDynamicAction(
    context: Context,
    rawInput: String,
    actionType: String = "AUTO",
    defaultMessage: String = ""
) {
    val input = rawInput.trim()
    val targetUrl = if (input.isBlank()) {
        SupportWebActivity.CRISP_URL
    } else {
        input
    }

    try {
        if (targetUrl.startsWith("http://", ignoreCase = true) || targetUrl.startsWith("https://", ignoreCase = true)) {
            // Web Link / Crisp Chat / External URL / WhatsApp link
            openWebUrl(context, targetUrl)
        } else if (targetUrl.contains("wa.me") || targetUrl.contains("whatsapp.com") || actionType.equals("WHATSAPP", ignoreCase = true)) {
            val cleanDigits = targetUrl.filter { it.isDigit() }
            val encodedMsg = if (defaultMessage.isNotBlank()) URLEncoder.encode(defaultMessage, StandardCharsets.UTF_8.toString()) else ""
            val waUrl = if (cleanDigits.isNotBlank()) {
                if (encodedMsg.isNotBlank()) "https://wa.me/$cleanDigits?text=$encodedMsg" else "https://wa.me/$cleanDigits"
            } else {
                if (targetUrl.startsWith("wa.me")) "https://$targetUrl" else targetUrl
            }
            openWebUrl(context, waUrl)
        } else if (targetUrl.startsWith("tel:", ignoreCase = true)) {
            val cleanNumber = targetUrl.removePrefix("tel:").removePrefix("TEL:").trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
            context.startActivity(intent)
        } else if (isPhoneNumber(targetUrl, actionType)) {
            val cleanNumber = targetUrl.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
            context.startActivity(intent)
        } else {
            // URL / Web domain without scheme (e.g. crisp.chat, chat.example.com)
            val fullUrl = if (targetUrl.contains(".")) "https://$targetUrl" else "https://$targetUrl"
            openWebUrl(context, fullUrl)
        }
    } catch (e: Exception) {
        SupportWebActivity.start(context, SupportWebActivity.CRISP_URL)
    }
}

/**
 * Direct async fetch from Firebase Realtime Database
 */
suspend fun fetchLatestActionFromFirebase(targetKey: String): String? = withContext(Dispatchers.IO) {
    try {
        val database = FirebaseDatabase.getInstance()
        val paths = listOf("app_settings", "settings", "sponsor_banner")

        for (path in paths) {
            val fetched = kotlinx.coroutines.suspendCancellableCoroutine<String?> { continuation ->
                val ref = database.getReference(path)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!continuation.isActive) return
                        if (snapshot.exists()) {
                            val map = snapshot.value as? Map<*, *>
                            if (map != null) {
                                val value = when (targetKey) {
                                    "fab_button", "fab", "fabButton" -> {
                                        val obj = map["fab_button"] ?: map["fabButton"] ?: map["fab"]
                                        extractActionValue(obj) ?: map["fab_action"] ?: map["fab_phone"] ?: map["fabAction"]
                                    }
                                    "report_problem", "report", "reportProblem" -> {
                                        val obj = map["report_problem"] ?: map["reportProblem"] ?: map["report"]
                                        extractActionValue(obj) ?: map["report_action"] ?: map["report_phone"] ?: map["reportAction"] ?: map["report_link"]
                                    }
                                    else -> {
                                        val obj = map["contact_us"] ?: map["contactUs"] ?: map["contact"]
                                        extractActionValue(obj) ?: map["contact_action"] ?: map["contact_phone"] ?: map["contactAction"] ?: map["actionValue"] ?: map["actionLink"] ?: map["action_value"]
                                    }
                                }
                                val str = value?.toString()?.trim()
                                if (!str.isNullOrBlank() && str != "null") {
                                    continuation.resume(str) { _, _, _ -> }
                                    return
                                }
                            }
                        }
                        continuation.resume(null) { _, _, _ -> }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) {
                            continuation.resume(null) { _, _, _ -> }
                        }
                    }
                }
                ref.addListenerForSingleValueEvent(listener)
                continuation.invokeOnCancellation {
                    ref.removeEventListener(listener)
                }
            }

            if (!fetched.isNullOrBlank()) {
                return@withContext fetched
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

private fun extractActionValue(raw: Any?): String? {
    if (raw == null) return null
    if (raw is Map<*, *>) {
        val v = raw["actionValue"] ?: raw["action_value"] ?: raw["actionInput"] ?: raw["action_input"] ?: raw["actionLink"] ?: raw["action_link"] ?: raw["phone"] ?: raw["url"] ?: raw["link"]
        val str = v?.toString()?.trim()
        if (!str.isNullOrBlank() && str != "null") return str
    }
    val direct = raw.toString().trim()
    return if (direct.isNotBlank() && direct != "null") direct else null
}

fun openWhatsAppAdmin(context: Context, message: String) {
    executeSmartAction(
        context = context,
        actionInput = null,
        actionType = "WHATSAPP",
        defaultMessage = message,
        targetKey = "contact_us"
    )
}
