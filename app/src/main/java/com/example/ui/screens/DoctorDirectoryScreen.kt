package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Doctor
import com.example.data.model.SponsorBanner
import com.example.ui.components.DaySelectorBar
import com.example.ui.components.DoctorCard
import com.example.ui.components.SpecialtyFilterRow
import com.example.ui.components.TopSponsorshipBanner
import com.example.ui.components.WhatsAppFab
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
    searchQuery: String,
    selectedDay: String?,
    selectedSpecialty: String?,
    currentDayArabic: String,
    onSearchChanged: (String) -> Unit,
    onDaySelected: (String?) -> Unit,
    onSpecialtySelected: (String?) -> Unit,
    onAdminTriggered: () -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Hidden Admin Trigger State: exactly 10 consecutive taps on header logo
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var isRefreshing by remember { mutableStateOf(false) }
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
            // Header Hero with Medical Branding & 10-tap Logo trigger
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
                            // Top right: Rounded Teal Stethoscope Badge (10 taps triggers Admin)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F766E).copy(alpha = 0.55f),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("app_header_logo")
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val now = System.currentTimeMillis()
                                        // Reset count if delay between consecutive clicks exceeds 2.5 seconds
                                        if (now - lastTapTime > 2500L) {
                                            tapCount = 1
                                        } else {
                                            tapCount++
                                        }
                                        lastTapTime = now

                                        // Opens when clicking exactly 10 times consecutively without any hints or toasts
                                        if (tapCount == 10) {
                                            tapCount = 0
                                            onAdminTriggered()
                                        }
                                    }
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Top Sponsorship Banner (Position: Fixed directly above the main doctor list / search bar)
                        if (sponsorBanner.isCurrentlyActive()) {
                            TopSponsorshipBanner(
                                banner = sponsorBanner,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

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

            // Banner Image
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
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

            // Day Selector Bar (Top Navigation Bar)
            item {
                Text(
                    text = "فلترة حسب يوم التواجد:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DaySelectorBar(
                    selectedDay = selectedDay,
                    todayArabic = currentDayArabic,
                    onDaySelected = onDaySelected
                )
            }

            // Specialty Filter Row
            item {
                Text(
                    text = "فلترة حسب الاختصاص الطبي:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                SpecialtyFilterRow(
                    selectedSpecialty = selectedSpecialty,
                    onSpecialtySelected = onSpecialtySelected
                )
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
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        }

        // Fixed Floating WhatsApp Support Button anchored at bottom-start (bottom-left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            WhatsAppFab()
        }
    }
}
