package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.example.ui.theme.WhatsAppGreen
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

    val actionInput = config.effectiveInput.ifBlank { "+9647875023922" }
    val isPhone = remember(actionInput, config.actionType) {
        isPhoneNumber(actionInput, config.actionType)
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
                defaultMessage = customMessage
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
 * Universal Smart Action Dispatcher:
 * - Phone Number -> Intent.ACTION_DIAL
 * - URL / WhatsApp -> Intent.ACTION_VIEW
 */
fun executeSmartAction(
    context: Context,
    actionInput: String,
    actionType: String = "AUTO",
    defaultMessage: String = "السلام عليكم إدارة تطبيق أطباء زمار..."
) {
    val input = actionInput.trim()
    if (input.isBlank()) {
        Toast.makeText(context, "الرقم أو الرابط غير محدد", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        if (actionType.equals("WHATSAPP", ignoreCase = true) || input.contains("wa.me") || input.contains("whatsapp.com")) {
            val cleanDigits = input.filter { it.isDigit() }
            val encodedMsg = URLEncoder.encode(defaultMessage, StandardCharsets.UTF_8.toString())
            val waUrl = if (input.startsWith("http://") || input.startsWith("https://")) {
                input
            } else {
                "https://wa.me/$cleanDigits?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } else if (isPhoneNumber(input, actionType)) {
            val cleanNumber = input.removePrefix("tel:").trim()
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
            }
            context.startActivity(intent)
        } else {
            // URL / Web Link
            var url = input
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر تنفيذ الإجراء المطلوب", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsAppAdmin(context: Context, message: String) {
    executeSmartAction(context, "+9647875023922", "WHATSAPP", message)
}
