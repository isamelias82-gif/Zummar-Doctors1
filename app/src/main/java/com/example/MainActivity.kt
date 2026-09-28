package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdminPinDialog
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.DoctorDirectoryScreen
import com.example.ui.screens.LaboratoriesScreen
import com.example.ui.screens.PharmaciesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.DoctorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DoctorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Enforce Right-to-Left (RTL) for Arabic primary language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ZummarDoctorsApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ZummarDoctorsApp(viewModel: DoctorViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val filteredDoctors by viewModel.filteredDoctors.collectAsStateWithLifecycle()
    val allDoctors by viewModel.doctors.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedSpecialty by viewModel.selectedSpecialty.collectAsStateWithLifecycle()
    val sponsorBanner by viewModel.sponsorBanner.collectAsStateWithLifecycle()
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
                        pharmacies = viewModel.pharmacies
                    )
                }

                AppTab.LABORATORIES -> {
                    LaboratoriesScreen(
                        laboratories = viewModel.laboratories
                    )
                }

                AppTab.ADMIN -> {
                    AdminPortalScreen(
                        doctors = allDoctors,
                        sponsorBanner = sponsorBanner,
                        isPharmaciesEnabled = isPharmaciesEnabled,
                        isLaboratoriesEnabled = isLaboratoriesEnabled,
                        onAddDoctor = { viewModel.addDoctor(it) },
                        onUpdateDoctor = { viewModel.updateDoctor(it) },
                        onDeleteDoctor = { viewModel.deleteDoctor(it) },
                        onResetDefaults = { viewModel.resetToDefaults() },
                        onExportJson = { viewModel.exportJson() },
                        onImportJson = { viewModel.importJson(it) },
                        onUpdatePin = { viewModel.updateAdminPin(it) },
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
