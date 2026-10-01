package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.Doctor
import com.example.data.model.SponsorBanner
import com.example.data.model.StaticBanner
import com.example.ui.components.DaySelectorBar
import com.example.ui.components.DoctorCard
import com.example.ui.components.SpecialtyFilterRow
import com.example.ui.components.TopSponsorshipBanner
import com.example.ui.components.WhatsAppFab
import com.example.ui.components.executeSmartAction
import com.example.ui.theme.MintCyan
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDirectoryScreen(
    doctors: List<Doctor>,
    sponsorBanner: SponsorBanner,
    staticBanner: StaticBanner,
    appSettings: AppSettings = AppSettings(),
    searchQuery: String,
    selectedDay: String?,
    selectedSpecialty: String?,
    currentDayArabic: String,
    onSearchChanged: (String) -> Unit,
    onDaySelected: (String?) -> Unit,
    onSpecialtySelected: (String?) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var isSpecialtyMenuExpanded by remember { mutableStateOf(false) }
    var isDayMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    onRefresh()
                    kotlinx.coroutines.delay(600L)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Header Hero with Medical Branding & Quick Dynamic Action Chips
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(TealPrimaryDark, TealPrimary)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Top right: Rounded Teal Stethoscope Badge
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0F766E).copy(alpha = 0.55f),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("app_header_logo")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_stethoscope),
                                            contentDescription = "شعار أطباء زمار - سماعة طبية",
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "أطباء زمار",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "الدليل الطبي الشامل لناحية زمار",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MintCyan
                                        )
                                    )
                                }
                            }

                            // Top Sponsorship Banner (Position: Fixed between header and search bar)
                            if (sponsorBanner.isCurrentlyActive()) {
                                TopSponsorshipBanner(
                                    banner = sponsorBanner,
                                    modifier = Modifier.padding(top = 14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Search Bar Inside Header
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChanged,
                                placeholder = {
                                    Text(
                                        text = "ابحث بالاسم أو الاختصاص أو القسم (بحث فوري)...",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث",
                                        tint = TealPrimary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChanged("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "مسح",
                                                tint = TextMuted
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_bar")
                            )
                        }
                    }
                }

                // Banner Image (Static Banner under Search)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clickable {
                                if (staticBanner.actionLink.isNotBlank()) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(staticBanner.actionLink)))
                                    } catch (e: Exception) { /* Handle error */ }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (staticBanner.imagePath.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = staticBanner.imagePath,
                                contentDescription = "بانر",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.zummar_medical_banner),
                                contentDescription = "شعار مجمع زمار الطبي",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Primary Filter Buttons Section ("كافة الاختصاصات" & "يوم الأسبوع")
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. "كافة الاختصاصات" (All Specialties) Primary Filter Button with Dropdown Menu
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { isSpecialtyMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("filter_spec_primary_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selectedSpecialty != null) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        contentColor = if (selectedSpecialty != null) TealPrimaryDark else TextPrimary
                                    ),
                                    border = BorderStroke(1.5.dp, if (selectedSpecialty != null) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TealPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedSpecialty ?: "كافة الاختصاصات",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    Icon(
                                        imageVector = if (isSpecialtyMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isSpecialtyMenuExpanded,
                                    onDismissRequest = { isSpecialtyMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("كافة الاختصاصات", fontWeight = if (selectedSpecialty == null) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onSpecialtySelected(null)
                                            isSpecialtyMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (selectedSpecialty == null) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                            }
                                        }
                                    )
                                    Doctor.ALL_SPECIALTIES.forEach { specialty ->
                                        val isSelected = selectedSpecialty == specialty
                                        DropdownMenuItem(
                                            text = { Text(specialty, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                onSpecialtySelected(specialty)
                                                isSpecialtyMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. "يوم الأسبوع" (Day of the Week) Primary Filter Button with DropdownMenu
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { isDayMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("filter_day_primary_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selectedDay != null) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        contentColor = if (selectedDay != null) TealPrimaryDark else TextPrimary
                                    ),
                                    border = BorderStroke(1.5.dp, if (selectedDay != null) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TealPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedDay != null) "يوم: $selectedDay" else "يوم الأسبوع",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    Icon(
                                        imageVector = if (isDayMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isDayMenuExpanded,
                                    onDismissRequest = { isDayMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("جميع الأيام", fontWeight = if (selectedDay == null) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onDaySelected(null)
                                            isDayMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (selectedDay == null) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                            }
                                        }
                                    )
                                    Doctor.ALL_DAYS.forEach { day ->
                                        val isSelected = selectedDay == day
                                        val isToday = day == currentDayArabic
                                        DropdownMenuItem(
                                            text = { Text(if (isToday) "$day (اليوم)" else day, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                onDaySelected(day)
                                                isDayMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Results count badge & dynamic sorting note
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TealPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "بحث مباشر",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TealPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (searchQuery.isNotBlank()) "نتائج البحث: ${doctors.size} طبيب" else "عدد الأطباء المعروضين: ${doctors.size}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "الأطباء المتاحون أولاً ⚡",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Doctors List or Empty State
                if (doctors.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "لا توجد نتائج تطابق \"$searchQuery\"" else "لا يوجد أطباء مطابقون لمعايير البحث الحالية",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "تأكد من كتابة حرف أو حرفين من الاسم أو الاختصاص أو القسم الطبي" else "جرّب تغيير يوم الفلترة أو الاختصاص أو مسح نص البحث",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(doctors, key = { it.id }) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            currentDayArabic = currentDayArabic,
                            selectedDay = selectedDay,
                            shareTemplate = appSettings.doctorShareText,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Bottom Help & Feedback Card (Dynamic Visibility)
                if (appSettings.contactUs.isActive || appSettings.reportProblem.isActive) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "هل تحتاج إلى مساعدة أو تود إضافة طبيب؟",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "فريق الإدارة جاهز للرد على استفساراتكم وتحديث البيانات فوراً",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (appSettings.contactUs.isActive) {
                                        FilledTonalButton(
                                            onClick = {
                                                val dynamicUrl = appSettings.contactUs.effectiveInput.ifBlank {
                                                    com.example.util.SupportChatManager.cachedContactUrl
                                                }
                                                com.example.util.SupportChatManager.openInAppChat(context, dynamicUrl)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_bottom_contact_us"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HeadsetMic,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تواصل معنا", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }

                                    if (appSettings.reportProblem.isActive) {
                                        OutlinedButton(
                                            onClick = {
                                                val dynamicUrl = appSettings.reportProblem.effectiveInput.ifBlank {
                                                    com.example.util.SupportChatManager.cachedReportUrl
                                                }
                                                com.example.util.SupportChatManager.openInAppChat(context, dynamicUrl)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_bottom_report_problem"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReportProblem,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إبلاغ عن مشكلة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fixed Floating Action Button (FAB) anchored at bottom-start (bottom-left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            WhatsAppFab(config = appSettings.fabButton)
        }
    }
}
