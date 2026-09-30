package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
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
        if (actionInput.isBlank()) true else isPhoneNumber(actionInput, config.actionType)
    }
    val isWhatsapp = remember(actionInput, config.actionType) {
        config.actionType.equals("WHATSAPP", ignoreCase = true) || actionInput.contains("wa.me")
    }

    val icon = when {
        isWhatsapp -> Icons.Default.Chat
        isPhone -> Icons.Default.Call
        else -> Icons.Default.OpenInBrowser
    }

    val desc = when {
        isWhatsapp -> "تواصل مع الإدارة عبر واتساب"
        isPhone -> "اتصال مباشر بالإدارة"
        else -> "فتح الرابط التفاعلي"
    }

    FloatingActionButton(
        onClick = {
            executeSmartAction(
                context = context,
                actionInput = actionInput,
                actionType = config.actionType,
                defaultMessage = customMessage,
                targetKey = "fab_button"
            )
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
 * 2. If actionValue starts with "http://" or "https://", launch Intent.ACTION_VIEW (Crisp Chat / Web link).
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
            Toast.makeText(context, "لم يتم تحديد وسيلة التواصل في لوحة التحكم بعد", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Core Dynamic Router:
 * - "http://" or "https://" -> Intent.ACTION_VIEW
 * - numeric / phone -> Intent.ACTION_DIAL
 * - "wa.me" -> Intent.ACTION_VIEW
 */
fun routeDynamicAction(
    context: Context,
    rawInput: String,
    actionType: String = "AUTO",
    defaultMessage: String = ""
) {
    val input = rawInput.trim()
    if (input.isBlank()) {
        Toast.makeText(context, "لم يتم تحديد وسيلة التواصل", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
            // Web Link / Crisp Chat / External URL / WhatsApp link
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(input))
            context.startActivity(intent)
        } else if (input.contains("wa.me") || input.contains("whatsapp.com") || actionType.equals("WHATSAPP", ignoreCase = true)) {
            val cleanDigits = input.filter { it.isDigit() }
            val encodedMsg = if (defaultMessage.isNotBlank()) URLEncoder.encode(defaultMessage, StandardCharsets.UTF_8.toString()) else ""
            val waUrl = if (cleanDigits.isNotBlank()) {
                if (encodedMsg.isNotBlank()) "https://wa.me/$cleanDigits?text=$encodedMsg" else "https://wa.me/$cleanDigits"
            } else {
                if (input.startsWith("wa.me")) "https://$input" else input
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } else if (input.startsWith("tel:", ignoreCase = true)) {
            val cleanNumber = input.removePrefix("tel:").removePrefix("TEL:").trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
            context.startActivity(intent)
        } else if (isPhoneNumber(input, actionType)) {
            val cleanNumber = input.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
            context.startActivity(intent)
        } else {
            // URL / Web domain without scheme (e.g. crisp.chat, chat.example.com)
            val fullUrl = if (input.contains(".")) "https://$input" else "https://$input"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر تنفيذ الإجراء المطلوب", Toast.LENGTH_SHORT).show()
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
