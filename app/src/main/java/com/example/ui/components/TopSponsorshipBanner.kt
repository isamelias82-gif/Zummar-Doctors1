package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
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

    val actionInput = remember(banner.actionLink, banner.actionValue) {
        banner.actionLink.ifBlank { banner.actionValue }.trim()
    }
    val isPhone = remember(actionInput, banner.actionType) {
        isPhoneNumber(actionInput, banner.actionType)
    }
    val isWhatsapp = remember(banner.actionType, actionInput) {
        banner.actionType.equals("WHATSAPP", ignoreCase = true) || actionInput.contains("wa.me")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("top_sponsorship_banner"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 5.2f)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    handleBannerAction(context, banner)
                }
        ) {
            // Banner Image
            if (banner.imagePath.isNotBlank()) {
                val imageModel: Any = remember(banner.imagePath) {
                    when {
                        banner.imagePath.startsWith("data:image") -> {
                            try {
                                val base64Data = banner.imagePath.substringAfter("base64,")
                                Base64.decode(base64Data, Base64.DEFAULT)
                            } catch (e: Exception) {
                                banner.imagePath
                            }
                        }
                        banner.imagePath.startsWith("http://") || banner.imagePath.startsWith("https://") || banner.imagePath.startsWith("content://") -> banner.imagePath
                        banner.imagePath.startsWith("/") -> File(banner.imagePath)
                        else -> banner.imagePath
                    }
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            startY = 40f
                        )
                    )
            )

            // Sponsored Tag Badge (Top-Start)
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "إعلان رعاية",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Title & Description (Bottom-Start)
            if (banner.title.isNotBlank() || banner.description.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 6.dp, start = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (banner.title.isNotBlank()) {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1
                            )
                        }
                        if (banner.description.isNotBlank()) {
                            Text(
                                text = banner.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Action Indicator Badge (Bottom-End)
            if (actionInput.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp),
                    color = when {
                        isWhatsapp -> Color(0xFF25D366)
                        isPhone -> Color(0xFF0D9488)
                        else -> Color(0xFF2563EB)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            isWhatsapp -> Icons.Default.Chat
                            isPhone -> Icons.Default.Call
                            else -> Icons.Default.OpenInBrowser
                        }
                        val label = when {
                            isWhatsapp -> "مراسلة واتساب"
                            isPhone -> "اتصال مباشر"
                            else -> "فتح الرابط"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smart detection whether an action input is a phone number or URL.
 */
fun isPhoneNumber(input: String, actionType: String = ""): Boolean {
    if (actionType.equals("PHONE", ignoreCase = true)) return true
    if (actionType.equals("URL", ignoreCase = true) || actionType.equals("WHATSAPP", ignoreCase = true)) return false

    val trimmed = input.trim()
    if (trimmed.startsWith("tel:", ignoreCase = true)) return true
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("www.", ignoreCase = true) ||
        trimmed.contains("://") ||
        trimmed.contains(".com", ignoreCase = true) ||
        trimmed.contains(".net", ignoreCase = true) ||
        trimmed.contains(".org", ignoreCase = true) ||
        trimmed.contains(".iq", ignoreCase = true) ||
        trimmed.contains(".site", ignoreCase = true) ||
        trimmed.contains("wa.me", ignoreCase = true)) {
        return false
    }

    val digitsOnly = trimmed.filter { it.isDigit() }
    val isDigitHeavy = trimmed.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
    return digitsOnly.length >= 6 && isDigitHeavy
}

/**
 * Handles banner click:
 * - If Phone Number -> Intent.ACTION_DIAL
 * - If URL -> Intent.ACTION_VIEW
 */
fun handleBannerAction(context: Context, banner: SponsorBanner) {
    val input = banner.effectiveActionInput
    if (input.isBlank()) {
        Toast.makeText(context, "لم يتم تحديد وسيلة التواصل للإعلان", Toast.LENGTH_SHORT).show()
        return
    }
    routeDynamicAction(
        context = context,
        rawInput = input,
        actionType = banner.actionType,
        defaultMessage = "السلام عليكم، بخصوص الإعلان في تطبيق أطباء زمار..."
    )
}
