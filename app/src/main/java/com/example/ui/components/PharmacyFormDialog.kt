package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Pharmacy
import com.example.ui.theme.TealPrimary

@Composable
fun PharmacyFormDialog(
    initialPharmacy: Pharmacy?,
    onDismiss: () -> Unit,
    onSave: (Pharmacy) -> Unit
) {
    var name by remember { mutableStateOf(initialPharmacy?.name ?: "") }
    var pharmacist by remember { mutableStateOf(initialPharmacy?.pharmacist ?: "") }
    var addressLandmark by remember { mutableStateOf(initialPharmacy?.addressLandmark ?: "") }
    var phoneNumbersText by remember { mutableStateOf(initialPharmacy?.phoneNumbers?.joinToString(", ") ?: "") }
    var onCallDays by remember { mutableStateOf(initialPharmacy?.onCallDays ?: "") }
    var workingHours by remember { mutableStateOf(initialPharmacy?.workingHours ?: "") }
    var isOnDutyTonight by remember { mutableStateOf(initialPharmacy?.isOnDutyTonight ?: false) }
    var notes by remember { mutableStateOf(initialPharmacy?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPharmacy == null) "إضافة صيدلية جديدة" else "تعديل بيانات الصيدلية",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("اسم الصيدلية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pharmacist,
                    onValueChange = { pharmacist = it },
                    label = { Text("اسم الصيدلاني / المشرف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = addressLandmark,
                    onValueChange = { addressLandmark = it },
                    label = { Text("العنوان والمعلم القريب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneNumbersText,
                    onValueChange = { phoneNumbersText = it },
                    label = { Text("أرقام الهاتف (افصل بينها بفاصلة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = onCallDays,
                    onValueChange = { onCallDays = it },
                    label = { Text("أيام الخفارة (مثال: خافرة يومي الجمعة والسبت)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("ساعات الدوام (مثال: 8:00 صباحاً – 12:00 منتصف الليل)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isOnDutyTonight,
                        onCheckedChange = { isOnDutyTonight = it }
                    )
                    Text("خافرة الليلة (isOnDutyTonight)")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "اسم الصيدلية مطلوب"
                    } else {
                        val phones = phoneNumbersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val pharmacy = Pharmacy(
                            id = initialPharmacy?.id ?: 0L,
                            name = name.trim(),
                            pharmacist = pharmacist.trim(),
                            addressLandmark = addressLandmark.trim(),
                            phoneNumbers = phones,
                            onCallDays = onCallDays.trim(),
                            workingHours = workingHours.trim(),
                            isOnDutyTonight = isOnDutyTonight,
                            notes = notes.trim()
                        )
                        onSave(pharmacy)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
