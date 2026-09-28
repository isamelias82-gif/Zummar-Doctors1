package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WhatsAppGreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun WhatsAppFab(
    modifier: Modifier = Modifier,
    customMessage: String = "السلام عليكم إدارة تطبيق أطباء زمار، أود الاستفسار بخصوص الدليل أو طلب إضافة طبيب..."
) {
    val context = LocalContext.current

    FloatingActionButton(
        onClick = {
            openWhatsAppAdmin(context, customMessage)
        },
        shape = CircleShape,
        containerColor = WhatsAppGreen,
        contentColor = Color.White,
        modifier = modifier
            .size(56.dp)
            .testTag("whatsapp_admin_fab")
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "تواصل مع الإدارة عبر واتساب",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

fun openWhatsAppAdmin(context: Context, message: String) {
    try {
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val url = "https://wa.me/9647875023922?text=$encodedMessage"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}
