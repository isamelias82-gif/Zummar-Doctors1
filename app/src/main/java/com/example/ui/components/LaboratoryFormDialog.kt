package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Laboratory
import com.example.ui.theme.TealPrimary

@Composable
fun LaboratoryFormDialog(
    initialLaboratory: Laboratory?,
    onDismiss: () -> Unit,
    onSave: (Laboratory) -> Unit
) {
    var name by remember { mutableStateOf(initialLaboratory?.name ?: "") }
    var specialist by remember { mutableStateOf(initialLaboratory?.specialist ?: "") }
    var addressLandmark by remember { mutableStateOf(initialLaboratory?.addressLandmark ?: "") }
    var phoneNumbersText by remember { mutableStateOf(initialLaboratory?.phoneNumbers?.joinToString(", ") ?: "") }
    var workingDays by remember { mutableStateOf(initialLaboratory?.workingDays ?: "") }
    var workingHours by remember { mutableStateOf(initialLaboratory?.workingHours ?: "") }
    var servicesText by remember { mutableStateOf(initialLaboratory?.services?.joinToString("\n") ?: "") }
    var notes by remember { mutableStateOf(initialLaboratory?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialLaboratory == null) "إضافة مختبر جديد" else "تعديل بيانات المختبر",
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
                    label = { Text("اسم المختبر *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = specialist,
                    onValueChange = { specialist = it },
                    label = { Text("اسم الأخصائي / المشرف") },
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
                    value = workingDays,
                    onValueChange = { workingDays = it },
                    label = { Text("أيام الدوام (مثال: السبت إلى الخميس)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("ساعات الدوام (مثال: 8:00 صباحاً – 8:30 مساءً)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = servicesText,
                    onValueChange = { servicesText = it },
                    label = { Text("الخدمات والفحوصات (كل سطر خدمة)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        errorMessage = "اسم المختبر مطلوب"
                    } else {
                        val phones = phoneNumbersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val services = servicesText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        val laboratory = Laboratory(
                            id = initialLaboratory?.id ?: 0L,
                            name = name.trim(),
                            specialist = specialist.trim(),
                            addressLandmark = addressLandmark.trim(),
                            phoneNumbers = phones,
                            workingDays = workingDays.trim(),
                            workingHours = workingHours.trim(),
                            services = services,
                            notes = notes.trim()
                        )
                        onSave(laboratory)
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
