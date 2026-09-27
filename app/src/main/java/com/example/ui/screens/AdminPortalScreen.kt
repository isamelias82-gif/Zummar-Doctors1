package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.data.model.SponsorBanner
import com.example.ui.components.DoctorFormDialog
import com.example.ui.components.SectionsControlCard
import com.example.ui.components.SponsorBannerEditorCard
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AdminPortalScreen(
    doctors: List<Doctor>,
    sponsorBanner: SponsorBanner,
    isPharmaciesEnabled: Boolean,
    isLaboratoriesEnabled: Boolean,
    onAddDoctor: (Doctor) -> Unit,
    onUpdateDoctor: (Doctor) -> Unit,
    onDeleteDoctor: (Doctor) -> Unit,
    onResetDefaults: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Int>,
    onUpdatePin: (String) -> Unit,
    onUpdateSponsorBanner: (SponsorBanner) -> Unit,
    onTogglePharmacies: (Boolean) -> Unit,
    onToggleLaboratories: (Boolean) -> Unit,
    onClosePortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingDoctor by remember { mutableStateOf<Doctor?>(null) }
    var doctorToDelete by remember { mutableStateOf<Doctor?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinText by remember { mutableStateOf("") }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

            // Doctors List Header with count
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

            // Doctor Items for editing & deletion
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

        // FAB to Add Doctor
        FloatingActionButton(
            onClick = {
                editingDoctor = null
                showFormDialog = true
            },
            containerColor = TealPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("admin_fab_add_doctor")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة طبيب جديد")
        }
    }

    // Dialog: Add/Edit Doctor
    if (showFormDialog) {
        DoctorFormDialog(
            initialDoctor = editingDoctor,
            onDismiss = { showFormDialog = false },
            onSave = { doc ->
                if (editingDoctor == null) {
                    onAddDoctor(doc)
                    Toast.makeText(context, "تمت إضافة الطبيب بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    onUpdateDoctor(doc)
                    Toast.makeText(context, "تم تحديث بيانات الطبيب", Toast.LENGTH_SHORT).show()
                }
                showFormDialog = false
            }
        )
    }

    // Dialog: Confirm Delete
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    // Dialog: Change PIN
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("تغيير رمز المرور (PIN)") },
            text = {
                Column {
                    Text(
                        text = "أدخل رمز المرور الجديد لبوابة الإدارة (أرقام):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 32) newPinText = it },
                        label = { Text("الرمز الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText.isNotBlank()) {
                            onUpdatePin(newPinText.trim())
                            showChangePinDialog = false
                            Toast.makeText(context, "تم حفظ رمز المرور الجديد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
