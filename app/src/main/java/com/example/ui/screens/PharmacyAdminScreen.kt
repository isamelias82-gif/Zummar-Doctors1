package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Pharmacy
import com.example.ui.components.PharmacyFormDialog
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PharmacyViewModel

@Composable
fun PharmacyAdminScreen(
    viewModel: PharmacyViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pharmacies by viewModel.pharmacies.collectAsStateWithLifecycle()

    PharmacyAdminScreen(
        pharmacies = pharmacies,
        onAddPharmacy = { viewModel.addPharmacy(it) },
        onUpdatePharmacy = { viewModel.updatePharmacy(it) },
        onDeletePharmacy = { viewModel.deletePharmacy(it) },
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun PharmacyAdminScreen(
    pharmacies: List<Pharmacy>,
    onAddPharmacy: (Pharmacy) -> Unit,
    onUpdatePharmacy: (Pharmacy) -> Unit,
    onDeletePharmacy: (Pharmacy) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFormDialog by remember { mutableStateOf(false) }
    var editingPharmacy by remember { mutableStateOf<Pharmacy?>(null) }
    var pharmacyToDelete by remember { mutableStateOf<Pharmacy?>(null) }

    BackHandler {
        onBack()
    }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("pharmacy_admin_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "إدارة الصيدليات الخافرة",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "إضافة وتعديل وحذف الصيدليات وجداول الخفارة",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${pharmacies.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle & Header Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الصيدليات (${pharmacies.size}):",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            editingPharmacy = null
                            showFormDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_pharmacy_header_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة صيدلية", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Empty State
            if (pharmacies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد صيدليات مسجلة حالياً.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Pharmacy Items
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pharmacy.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (pharmacy.isOnDutyTonight) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "خافرة الليلة 🌙",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (pharmacy.pharmacist.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "المشرف: ${pharmacy.pharmacist}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }

                                if (pharmacy.workingHours.isNotBlank() || pharmacy.onCallDays.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = listOf(pharmacy.workingHours, pharmacy.onCallDays).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TealPrimary)
                                    )
                                }

                                if (pharmacy.addressLandmark.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = pharmacy.addressLandmark,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                if (pharmacy.phoneNumbers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = TealPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = pharmacy.phoneNumbers.joinToString(" - "),
                                            style = MaterialTheme.typography.labelSmall.copy(color = TealPrimary)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingPharmacy = pharmacy
                                        showFormDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_pharmacy_${pharmacy.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الصيدلية",
                                        tint = TealPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { pharmacyToDelete = pharmacy },
                                    modifier = Modifier.testTag("delete_pharmacy_${pharmacy.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف الصيدلية",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to Add Pharmacy
        FloatingActionButton(
            onClick = {
                editingPharmacy = null
                showFormDialog = true
            },
            containerColor = TealPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_pharmacy_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة صيدلية جديدة")
        }
    }

    // Dialog: Add / Edit Pharmacy Form
    if (showFormDialog) {
        PharmacyFormDialog(
            initialPharmacy = editingPharmacy,
            onDismiss = {
                showFormDialog = false
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
                showFormDialog = false
                editingPharmacy = null
            }
        )
    }

    // Dialog: Delete Confirmation
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
            text = { Text("هل أنت متأكد من حذف \"${target.name}\" نهائياً من قاعدة بيانات الصيدليات؟") },
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
}
