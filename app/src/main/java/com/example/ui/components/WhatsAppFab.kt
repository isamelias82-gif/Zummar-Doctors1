package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    Surface(
        onClick = {
            openWhatsAppAdmin(context, customMessage)
        },
        shape = RoundedCornerShape(28.dp),
        color = WhatsAppGreen,
        shadowElevation = 6.dp,
        modifier = modifier
            .testTag("whatsapp_admin_fab")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "واتساب الإدارة",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تواصل مع الإدارة",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
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
