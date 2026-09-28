package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdminPinDialog
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.DoctorDirectoryScreen
import com.example.ui.screens.LaboratoriesScreen
import com.example.ui.screens.PharmaciesScreen
import com.example.ui.theme.MintCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.DoctorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DoctorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            setContent {
                MyApplicationTheme {
                    // Enforce Right-to-Left (RTL) for Arabic primary language
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ZummarDoctorsApp(viewModel = viewModel)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Compose startup caught error: ${e.message}", e)
        }
    }
}

@Composable
fun ZummarDoctorsApp(viewModel: DoctorViewModel) {
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()

    if (!isReady) {
        AppStartupLoadingScreen()
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val filteredDoctors by viewModel.filteredDoctors.collectAsStateWithLifecycle()
    val allDoctors by viewModel.doctors.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedSpecialty by viewModel.selectedSpecialty.collectAsStateWithLifecycle()
    val sponsorBanner by viewModel.sponsorBanner.collectAsStateWithLifecycle()
    val pharmacies by viewModel.pharmacies.collectAsStateWithLifecycle()
    val laboratories by viewModel.laboratories.collectAsStateWithLifecycle()
    val isPharmaciesEnabled by viewModel.isPharmaciesEnabled.collectAsStateWithLifecycle()
    val isLaboratoriesEnabled by viewModel.isLaboratoriesEnabled.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }

    // BackHandler to handle custom state switching back navigation
    if (currentTab != AppTab.DOCTORS) {
        BackHandler {
            if (currentTab == AppTab.ADMIN) {
                viewModel.logoutAdmin()
            } else {
                viewModel.setTab(AppTab.DOCTORS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentTab != AppTab.ADMIN) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.DOCTORS,
                        onClick = { viewModel.setTab(AppTab.DOCTORS) },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stethoscope),
                                contentDescription = "أطباء زمار",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("أطباء زمار") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_doctors")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.PHARMACIES,
                        onClick = {
                            if (isPharmaciesEnabled) {
                                viewModel.setTab(AppTab.PHARMACIES)
                            }
                        },
                        enabled = isPharmaciesEnabled,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (!isPharmaciesEnabled) {
                                        Badge(
                                            containerColor = Color(0xFFE65100),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "قريباً",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    lineHeight = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = "الصيدليات الخافرة"
                                )
                            }
                        },
                        label = { Text("الصيدليات الخافرة") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.testTag("tab_pharmacies")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.LABORATORIES,
                        onClick = {
                            if (isLaboratoriesEnabled) {
                                viewModel.setTab(AppTab.LABORATORIES)
                            }
                        },
                        enabled = isLaboratoriesEnabled,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (!isLaboratoriesEnabled) {
                                        Badge(
                                            containerColor = Color(0xFFE65100),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "قريباً",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    lineHeight = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Biotech,
                                    contentDescription = "المختبرات"
                                )
                            }
                        },
                        label = { Text("المختبرات الطبية") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.testTag("tab_laboratories")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DOCTORS -> {
                    DoctorDirectoryScreen(
                        doctors = filteredDoctors,
                        sponsorBanner = sponsorBanner,
                        searchQuery = searchQuery,
                        selectedDay = selectedDay,
                        selectedSpecialty = selectedSpecialty,
                        currentDayArabic = viewModel.currentDayArabic,
                        onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                        onDaySelected = { viewModel.onDaySelected(it) },
                        onSpecialtySelected = { viewModel.onSpecialtySelected(it) },
                        onAdminTriggered = { showPinDialog = true },
                        onRefresh = { viewModel.refreshDoctors() }
                    )
                }

                AppTab.PHARMACIES -> {
                    PharmaciesScreen(
                        pharmacies = pharmacies
                    )
                }

                AppTab.LABORATORIES -> {
                    LaboratoriesScreen(
                        laboratories = laboratories
                    )
                }

                AppTab.ADMIN -> {
                    AdminPortalScreen(
                        doctors = allDoctors,
                        pharmacies = pharmacies,
                        laboratories = laboratories,
                        sponsorBanner = sponsorBanner,
                        isPharmaciesEnabled = isPharmaciesEnabled,
                        isLaboratoriesEnabled = isLaboratoriesEnabled,
                        onAddDoctor = { viewModel.addDoctor(it) },
                        onUpdateDoctor = { viewModel.updateDoctor(it) },
                        onDeleteDoctor = { viewModel.deleteDoctor(it) },
                        onAddPharmacy = { viewModel.addPharmacy(it) },
                        onUpdatePharmacy = { viewModel.updatePharmacy(it) },
                        onDeletePharmacy = { viewModel.deletePharmacy(it) },
                        onAddLaboratory = { viewModel.addLaboratory(it) },
                        onUpdateLaboratory = { viewModel.updateLaboratory(it) },
                        onDeleteLaboratory = { viewModel.deleteLaboratory(it) },
                        onResetDefaults = { viewModel.resetToDefaults() },
                        onExportJson = { viewModel.exportJson() },
                        onImportJson = { viewModel.importJson(it) },
                        onUpdatePin = { viewModel.updateAdminPin(it) },
                        verifyPin = { viewModel.verifyPin(it) },
                        onUpdateSponsorBanner = { viewModel.updateSponsorBanner(it) },
                        onTogglePharmacies = { viewModel.setPharmaciesEnabled(it) },
                        onToggleLaboratories = { viewModel.setLaboratoriesEnabled(it) },
                        onClosePortal = { viewModel.logoutAdmin() }
                    )
                }
            }
        }
    }

    // Secret Admin PIN Dialog
    if (showPinDialog) {
        AdminPinDialog(
            onDismiss = { showPinDialog = false },
            onSuccess = {
                showPinDialog = false
                viewModel.setTab(AppTab.ADMIN)
            },
            verifyPin = { pin -> viewModel.verifyPin(pin) }
        )
    }
}

@Composable
fun AppStartupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TealPrimaryDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F766E).copy(alpha = 0.55f),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                shadowElevation = 6.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stethoscope),
                        contentDescription = "شعار أطباء زمار",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "أطباء زمار",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "الدليل الطبي الشامل لناحية زمار",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MintCyan
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = MintCyan,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "جاري تحميل الدليل الطبي...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}
