package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.model.SponsorBanner
import com.example.ui.components.CategoryTabButton
import com.example.ui.components.DoctorFormDialog
import com.example.ui.components.LaboratoryFormDialog
import com.example.ui.components.PharmacyFormDialog
import com.example.ui.components.SectionsControlCard
import com.example.ui.components.SponsorBannerEditorCard
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class AdminSection {
    DOCTORS,
    PHARMACIES,
    LABORATORIES
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    doctors: List<Doctor>,
    pharmacies: List<Pharmacy> = emptyList(),
    laboratories: List<Laboratory> = emptyList(),
    sponsorBanner: SponsorBanner,
    isPharmaciesEnabled: Boolean,
    isLaboratoriesEnabled: Boolean,
    onAddDoctor: (Doctor) -> Unit,
    onUpdateDoctor: (Doctor) -> Unit,
    onDeleteDoctor: (Doctor) -> Unit,
    onAddPharmacy: (Pharmacy) -> Unit = {},
    onUpdatePharmacy: (Pharmacy) -> Unit = {},
    onDeletePharmacy: (Pharmacy) -> Unit = {},
    onAddLaboratory: (Laboratory) -> Unit = {},
    onUpdateLaboratory: (Laboratory) -> Unit = {},
    onDeleteLaboratory: (Laboratory) -> Unit = {},
    onResetDefaults: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Int>,
    onUpdatePin: (String) -> Unit,
    verifyPin: (String) -> Boolean,
    onUpdateSponsorBanner: (SponsorBanner) -> Unit,
    onTogglePharmacies: (Boolean) -> Unit,
    onToggleLaboratories: (Boolean) -> Unit,
    onClosePortal: () -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    var adminSection by remember { mutableStateOf(AdminSection.DOCTORS) }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingDoctor by remember { mutableStateOf<Doctor?>(null) }
    var doctorToDelete by remember { mutableStateOf<Doctor?>(null) }

    var showPharmacyDialog by remember { mutableStateOf(false) }
    var editingPharmacy by remember { mutableStateOf<Pharmacy?>(null) }
    var pharmacyToDelete by remember { mutableStateOf<Pharmacy?>(null) }

    var showLabDialog by remember { mutableStateOf(false) }
    var editingLab by remember { mutableStateOf<Laboratory?>(null) }
    var labToDelete by remember { mutableStateOf<Laboratory?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPasswordText by remember { mutableStateOf("") }
    var newPasswordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(800L)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(TealPrimaryDark, TealPrimary)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onClosePortal,
                                    modifier = Modifier.testTag("admin_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "بوابة الإدارة المركزية",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "إدارة دليل أطباء زمار والنسخ الاحتياطي",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            isRefreshing = true
                                            onRefresh()
                                            delay(800L)
                                            isRefreshing = false
                                            Toast.makeText(context, "تم تحديث البيانات من خادم Firebase", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("admin_refresh_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحديث من الخادم",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.testTag("change_pin_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "تغيير الرمز",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Database Actions Card (Backup / Restore / Reset)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "أدوات النسخ الاحتياطي وإدارة البيانات:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    scope.launch {
                                        exportedJsonText = onExportJson()
                                        showExportDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير نسخة", style = MaterialTheme.typography.labelSmall)
                            }

                            ElevatedButton(
                                onClick = {
                                    importJsonText = ""
                                    showImportDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد بيانات", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_defaults_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة بيانات دليل زمار الافتراضية")
                        }
                    }
                }
            }

            // Security & Admin Passcode Card (Change Passcode option)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "أمان بوابة الإدارة:",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك تغيير كلمة المرور لحماية لوحة الإدارة من التعديل غير المصرح به. يتطلب التغيير التحقق من كلمة المرور الحالية أولاً.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showChangePinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_passcode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تغيير كلمة المرور")
                        }
                    }
                }
            }

            // Secondary Sections Availability Control (Coming Soon & Disable Taps)
            item {
                SectionsControlCard(
                    isPharmaciesEnabled = isPharmaciesEnabled,
                    isLaboratoriesEnabled = isLaboratoriesEnabled,
                    onTogglePharmacies = onTogglePharmacies,
                    onToggleLaboratories = onToggleLaboratories
                )
            }

            // Top Sponsorship Banner Management Controls
            item {
                SponsorBannerEditorCard(
                    currentBanner = sponsorBanner,
                    onSaveBanner = onUpdateSponsorBanner
                )
            }

            // Admin Section Selector (Doctors, Pharmacies, Laboratories)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryTabButton(
                        title = "إدارة الأطباء",
                        icon = Icons.Default.MedicalServices,
                        selected = adminSection == AdminSection.DOCTORS,
                        onClick = { adminSection = AdminSection.DOCTORS },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryTabButton(
                        title = "إدارة الصيدليات",
                        icon = Icons.Default.LocalPharmacy,
                        selected = adminSection == AdminSection.PHARMACIES,
                        onClick = { adminSection = AdminSection.PHARMACIES },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryTabButton(
                        title = "إدارة المختبرات",
                        icon = Icons.Default.Biotech,
                        selected = adminSection == AdminSection.LABORATORIES,
                        onClick = { adminSection = AdminSection.LABORATORIES },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when (adminSection) {
                AdminSection.DOCTORS -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة الأطباء المسجلين (${doctors.size}):",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    editingDoctor = null
                                    showFormDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_doctor_header_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة طبيب", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(doctors, key = { it.id }) { doctor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("admin_doctor_item_${doctor.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doctor.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${doctor.specialty} • ${doctor.days.size} أيام",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = doctor.phoneNumbers.joinToString(" - "),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TealPrimary)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingDoctor = doctor
                                            showFormDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_doc_${doctor.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل",
                                            tint = TealPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { doctorToDelete = doctor },
                                        modifier = Modifier.testTag("delete_doc_${doctor.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.PHARMACIES -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة الصيدليات الخافرة (${pharmacies.size}):",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    editingPharmacy = null
                                    showPharmacyDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_pharmacy_header_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة صيدلية", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(pharmacies, key = { it.id }) { pharmacy ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("admin_pharmacy_item_${pharmacy.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pharmacy.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "الإشراف: ${pharmacy.pharmacist} • ${if (pharmacy.isOnDutyTonight) "خافرة الليلة 🌙" else "غير خافرة"}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = pharmacy.workingHours,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TealPrimary)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingPharmacy = pharmacy
                                            showPharmacyDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_pharmacy_${pharmacy.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل",
                                            tint = TealPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { pharmacyToDelete = pharmacy },
                                        modifier = Modifier.testTag("delete_pharmacy_${pharmacy.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.LABORATORIES -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة المختبرات الطبية (${laboratories.size}):",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    editingLab = null
                                    showLabDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_lab_header_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة مختبر", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    items(laboratories, key = { it.id }) { lab ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("admin_lab_item_${lab.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lab.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "المشرف: ${lab.specialist}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = lab.workingHours,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TealPrimary)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingLab = lab
                                            showLabDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_lab_${lab.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل",
                                            tint = TealPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { labToDelete = lab },
                                        modifier = Modifier.testTag("delete_lab_${lab.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Dynamic FAB to Add Doctor / Pharmacy / Laboratory
        FloatingActionButton(
            onClick = {
                when (adminSection) {
                    AdminSection.DOCTORS -> {
                        editingDoctor = null
                        showFormDialog = true
                    }
                    AdminSection.PHARMACIES -> {
                        editingPharmacy = null
                        showPharmacyDialog = true
                    }
                    AdminSection.LABORATORIES -> {
                        editingLab = null
                        showLabDialog = true
                    }
                }
            },
            containerColor = TealPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag(
                    when (adminSection) {
                        AdminSection.DOCTORS -> "admin_fab_add_doctor"
                        AdminSection.PHARMACIES -> "admin_fab_add_pharmacy"
                        AdminSection.LABORATORIES -> "admin_fab_add_lab"
                    }
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = when (adminSection) {
                    AdminSection.DOCTORS -> "إضافة طبيب جديد"
                    AdminSection.PHARMACIES -> "إضافة صيدلية جديدة"
                    AdminSection.LABORATORIES -> "إضافة مختبر جديد"
                }
            )
        }
    }

    // Dialog: Add/Edit Doctor
    if (showFormDialog) {
        DoctorFormDialog(
            initialDoctor = editingDoctor,
            onDismiss = {
                showFormDialog = false
                editingDoctor = null
            },
            onSave = { doc ->
                if (editingDoctor == null) {
                    onAddDoctor(doc)
                    Toast.makeText(context, "تمت إضافة الطبيب بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    onUpdateDoctor(doc)
                    Toast.makeText(context, "تم تحديث بيانات الطبيب", Toast.LENGTH_SHORT).show()
                }
                showFormDialog = false
                editingDoctor = null
            }
        )
    }

    // Dialog: Confirm Delete Doctor
    if (doctorToDelete != null) {
        AlertDialog(
            onDismissRequest = { doctorToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("تأكيد حذف الطبيب") },
            text = { Text("هل أنت متأكد من حذف ${doctorToDelete?.name} نهائياً من الدليل؟") },
            confirmButton = {
                Button(
                    onClick = {
                        doctorToDelete?.let { onDeleteDoctor(it) }
                        doctorToDelete = null
                        Toast.makeText(context, "تم حذف الطبيب", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_doctor_btn")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { doctorToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Add/Edit Pharmacy
    if (showPharmacyDialog) {
        PharmacyFormDialog(
            initialPharmacy = editingPharmacy,
            onDismiss = {
                showPharmacyDialog = false
                editingPharmacy = null
            },
            onSave = { pharmacy ->
                if (editingPharmacy == null) {
                    onAddPharmacy(pharmacy)
                    Toast.makeText(context, "تمت إضافة الصيدلية بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    // Preserves the existing ID when updating
                    onUpdatePharmacy(pharmacy)
                    Toast.makeText(context, "تم تحديث بيانات الصيدلية بنجاح", Toast.LENGTH_SHORT).show()
                }
                showPharmacyDialog = false
                editingPharmacy = null
            }
        )
    }

    // Dialog: Confirm Delete Pharmacy
    if (pharmacyToDelete != null) {
        val target = pharmacyToDelete!!
        AlertDialog(
            onDismissRequest = { pharmacyToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("تأكيد حذف الصيدلية") },
            text = { Text("هل أنت متأكد من حذف \"${target.name}\" نهائياً من قاعدة البيانات؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePharmacy(target)
                        pharmacyToDelete = null
                        Toast.makeText(context, "تم حذف الصيدلية بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_pharmacy_btn")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pharmacyToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Add/Edit Laboratory
    if (showLabDialog) {
        LaboratoryFormDialog(
            initialLaboratory = editingLab,
            onDismiss = {
                showLabDialog = false
                editingLab = null
            },
            onSave = { lab ->
                if (editingLab == null) {
                    onAddLaboratory(lab)
                    Toast.makeText(context, "تمت إضافة المختبر بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    // Preserves the existing ID when updating
                    onUpdateLaboratory(lab)
                    Toast.makeText(context, "تم تحديث بيانات المختبر بنجاح", Toast.LENGTH_SHORT).show()
                }
                showLabDialog = false
                editingLab = null
            }
        )
    }

    // Dialog: Confirm Delete Laboratory
    if (labToDelete != null) {
        val target = labToDelete!!
        AlertDialog(
            onDismissRequest = { labToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("تأكيد حذف المختبر") },
            text = { Text("هل أنت متأكد من حذف \"${target.name}\" نهائياً من قاعدة البيانات؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLaboratory(target)
                        labToDelete = null
                        Toast.makeText(context, "تم حذف المختبر بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_lab_btn")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { labToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Export Database JSON
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("نسخة احتياطية من قاعدة البيانات") },
            text = {
                Column {
                    Text(
                        text = "تم إنشاء كود النسخة الاحتياطية بنجاح. يمكنك نسخه أو مشاركته لحفظ البيانات من الضياع:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Zummar Doctors Backup", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ كود النسخة الاحتياطية للحافظة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("نسخ الكود")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية أطباء زمار")
                            putExtra(Intent.EXTRA_TEXT, exportedJsonText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة ملف النسخة"))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }
        )
    }

    // Dialog: Import Database JSON
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد قاعدة البيانات") },
            text = {
                Column {
                    Text(
                        text = "الصق كود JSON للنسخة الاحتياطية هنا لاستعادة قائمة الأطباء:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("الصق كود النسخة الاحتياطية هنا...") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            scope.launch {
                                val result = onImportJson(importJsonText.trim())
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "تم استيراد ${result.getOrNull()} طبيب بنجاح",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showImportDialog = false
                                } else {
                                    Toast.makeText(
                                        context,
                                        "خطأ في الاستيراد: ${result.exceptionOrNull()?.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("استيراد واستبدال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Reset Defaults Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("تأكيد استعادة الدليل الأصلي") },
            text = { Text("سيتم مسح أي تعديلات واستعادة القائمة الافتراضية لأطباء زمار. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "تم استرجاع الدليل الافتراضي لزمار", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Change Password / PIN
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                currentPasswordText = ""
                newPasswordText = ""
                confirmPasswordText = ""
                pinErrorMessage = null
            },
            title = { Text("تغيير كلمة المرور") },
            text = {
                Column {
                    Text(
                        text = "أدخل كلمة المرور الحالية والجديدة لبوابة الإدارة:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentPasswordText,
                        onValueChange = { currentPasswordText = it; pinErrorMessage = null },
                        label = { Text("كلمة المرور الحالية") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("current_password_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasswordText,
                        onValueChange = { newPasswordText = it; pinErrorMessage = null },
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPasswordText,
                        onValueChange = { confirmPasswordText = it; pinErrorMessage = null },
                        label = { Text("تأكيد كلمة المرور الجديدة") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinErrorMessage != null,
                        supportingText = {
                            if (pinErrorMessage != null) {
                                Text(text = pinErrorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentValid = verifyPin(currentPasswordText.trim())
                        if (!currentValid) {
                            pinErrorMessage = "كلمة المرور الحالية غير صحيحة"
                        } else if (newPasswordText.isBlank()) {
                            pinErrorMessage = "كلمة المرور الجديدة لا يمكن أن تكون فارغة"
                        } else if (newPasswordText != confirmPasswordText) {
                            pinErrorMessage = "كلمة المرور الجديدة وتأكيدها غير متطابقين"
                        } else {
                            onUpdatePin(newPasswordText.trim())
                            showChangePinDialog = false
                            currentPasswordText = ""
                            newPasswordText = ""
                            confirmPasswordText = ""
                            pinErrorMessage = null
                            Toast.makeText(context, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("save_password_btn")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    currentPasswordText = ""
                    newPasswordText = ""
                    confirmPasswordText = ""
                    pinErrorMessage = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
