package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.SponsorBanner
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun TopSponsorshipBanner(
    banner: SponsorBanner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Check if banner is active and not expired
    if (!banner.isCurrentlyActive()) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("top_sponsorship_banner"),
        shape = RoundedCornerShape(10.dp), // 8-12dp as specified
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 5f) // 16:5 ratio as specified
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    handleBannerAction(context, banner.actionType, banner.actionValue)
                }
        ) {
            // Banner Image
            if (banner.imagePath.isNotBlank()) {
                val imageModel: Any = when {
                    banner.imagePath.startsWith("content://") || banner.imagePath.startsWith("http://") || banner.imagePath.startsWith("https://") -> banner.imagePath
                    banner.imagePath.startsWith("/") -> File(banner.imagePath)
                    else -> banner.imagePath
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .error(R.drawable.sponsor_banner_default)
                        .placeholder(R.drawable.sponsor_banner_default)
                        .build(),
                    contentDescription = banner.title.ifBlank { "إعلان رعاية" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Default sponsor banner
                Image(
                    painter = painterResource(id = R.drawable.sponsor_banner_default),
                    contentDescription = banner.title.ifBlank { "إعلان رعاية" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle dark gradient scrim at the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            startY = 60f
                        )
                    )
            )

            // Sponsored Tag Badge (Top-Start)
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(13.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "إعلان رعاية",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Action Indicator Badge (Bottom-End)
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp),
                color = when (banner.actionType) {
                    SponsorBanner.ACTION_WHATSAPP -> Color(0xFF25D366)
                    SponsorBanner.ACTION_PHONE -> Color(0xFF006D77)
                    else -> Color(0xFF028090)
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (banner.actionType) {
                        SponsorBanner.ACTION_WHATSAPP -> Icons.Default.Chat
                        SponsorBanner.ACTION_PHONE -> Icons.Default.Call
                        else -> Icons.Default.OpenInBrowser
                    }
                    val label = when (banner.actionType) {
                        SponsorBanner.ACTION_WHATSAPP -> "مراسلة واتساب"
                        SponsorBanner.ACTION_PHONE -> "اتصال مباشر"
                        else -> "زيارة الموقع"
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

/**
 * Handles banner tap deep-linking actions:
 * - Phone Call: opens native dialer via tel:
 * - WhatsApp: launches WhatsApp via https://wa.me/
 * - External Link: opens default browser via intent
 */
fun handleBannerAction(context: Context, actionType: String, actionValue: String) {
    try {
        when (actionType) {
            SponsorBanner.ACTION_PHONE -> {
                val cleanNumber = actionValue.replace(" ", "").replace("-", "")
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                }
                context.startActivity(intent)
            }

            SponsorBanner.ACTION_WHATSAPP -> {
                val cleanNumber = actionValue.replace(" ", "").replace("-", "").replace("+", "")
                val defaultMsg = "السلام عليكم، بخصوص الإعلان في تطبيق أطباء زمار..."
                val encodedMsg = URLEncoder.encode(defaultMsg, StandardCharsets.UTF_8.toString())
                val waUrl = "https://wa.me/$cleanNumber?text=$encodedMsg"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                context.startActivity(intent)
            }

            SponsorBanner.ACTION_URL -> {
                var url = actionValue.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }

            else -> {
                // Fallback dialer
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$actionValue")
                }
                context.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر تنفيذ الإجراء المطلوب للإعلان", Toast.LENGTH_SHORT).show()
    }
}
