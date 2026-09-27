package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.SponsorBanner
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

@Composable
fun SponsorBannerEditorCard(
    currentBanner: SponsorBanner,
    onSaveBanner: (SponsorBanner) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isActive by remember(currentBanner) { mutableStateOf(currentBanner.isActive) }
    var title by remember(currentBanner) { mutableStateOf(currentBanner.title) }
    var imagePath by remember(currentBanner) { mutableStateOf(currentBanner.imagePath) }
    var actionType by remember(currentBanner) { mutableStateOf(currentBanner.actionType) }
    var actionValue by remember(currentBanner) { mutableStateOf(currentBanner.actionValue) }
    var expiryDate by remember(currentBanner) { mutableStateOf(currentBanner.expiryDate) }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Copy to local app internal storage for reliable persistence
                val destinationFile = File(context.filesDir, "sponsor_banner_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                imagePath = destinationFile.absolutePath
                Toast.makeText(context, "تم تحديد وتخزين صورة الإعلان بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                imagePath = uri.toString()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("sponsor_banner_editor_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Enable/Disable Global Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "إعلان البانر العلوي (Sponsorship Banner)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "تحكم في البانر الترويجي وأزرار الاتصال",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                // Global Switch: is_banner_active
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    ),
                    modifier = Modifier.testTag("banner_active_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Banner Image Live Preview & Upload
            Text(
                text = "معاينة صورة البانر (النسبة 16:5):",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 5f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imagePath.isNotBlank()) {
                    val imgModel: Any = when {
                        imagePath.startsWith("content://") || imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
                        imagePath.startsWith("/") -> File(imagePath)
                        else -> imagePath
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imgModel)
                            .crossfade(true)
                            .error(R.drawable.sponsor_banner_default)
                            .placeholder(R.drawable.sponsor_banner_default)
                            .build(),
                        contentDescription = "معاينة الإعلان",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.sponsor_banner_default),
                        contentDescription = "الصورة الافتراضية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay prompt to change image
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "انقر لتغيير الصورة من المعرض",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رفع صورة جديدة", style = MaterialTheme.typography.labelSmall)
                }

                if (imagePath.isNotBlank()) {
                    OutlinedButton(
                        onClick = { imagePath = "" },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الافتراضية", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sponsor Title / Name
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الجهة الراعية / المعلن") },
                placeholder = { Text("مثال: مجمع النور الطبي التخصصي") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Type Selector (Phone | WhatsApp | External Link)
            Text(
                text = "نوع إجراء النقر (Action Type):",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp
                FilterChip(
                    selected = actionType == SponsorBanner.ACTION_WHATSAPP,
                    onClick = { actionType = SponsorBanner.ACTION_WHATSAPP },
                    label = { Text("واتساب") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WhatsAppGreen,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Phone Call
                FilterChip(
                    selected = actionType == SponsorBanner.ACTION_PHONE,
                    onClick = { actionType = SponsorBanner.ACTION_PHONE },
                    label = { Text("اتصال هاتف") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // External Link
                FilterChip(
                    selected = actionType == SponsorBanner.ACTION_URL,
                    onClick = { actionType = SponsorBanner.ACTION_URL },
                    label = { Text("رابط موقع") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF028090),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target Phone Number or URL
            OutlinedTextField(
                value = actionValue,
                onValueChange = { actionValue = it },
                label = {
                    Text(
                        when (actionType) {
                            SponsorBanner.ACTION_PHONE -> "رقم الهاتف المستهدف للاتصال"
                            SponsorBanner.ACTION_WHATSAPP -> "رقم الواتساب المستهدف (+964...)"
                            else -> "رابط الموقع المستهدف (https://...)"
                        }
                    )
                },
                placeholder = {
                    Text(
                        when (actionType) {
                            SponsorBanner.ACTION_PHONE -> "مثال: 07875023922"
                            SponsorBanner.ACTION_WHATSAPP -> "مثال: +9647875023922"
                            else -> "مثال: https://zummar-medical.com"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = if (actionType == SponsorBanner.ACTION_URL) KeyboardOptions(keyboardType = KeyboardType.Uri) else KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_action_value_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Expiry Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("تاريخ انتهاء الإعلان (اختياري: YYYY-MM-DD)") },
                    placeholder = { Text("مثال: 2026-10-31") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("banner_expiry_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val mStr = (month + 1).toString().padStart(2, '0')
                                val dStr = day.toString().padStart(2, '0')
                                expiryDate = "$year-$mStr-$dStr"
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "اختيار التاريخ",
                        tint = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Banner Settings Button
            Button(
                onClick = {
                    val updated = SponsorBanner(
                        bannerId = currentBanner.bannerId.ifBlank { "sponsor_01" },
                        isActive = isActive,
                        imagePath = imagePath.trim(),
                        actionType = actionType,
                        actionValue = actionValue.trim(),
                        expiryDate = expiryDate.trim(),
                        title = title.trim()
                    )
                    onSaveBanner(updated)
                    Toast.makeText(context, "تم حفظ إعدادات إعلان البانر بنجاح", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_banner_settings_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ إعدادات البانر الترويجي", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
