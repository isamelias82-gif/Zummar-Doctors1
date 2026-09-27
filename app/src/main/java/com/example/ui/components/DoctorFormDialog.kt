package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorFormDialog(
    initialDoctor: Doctor? = null,
    onDismiss: () -> Unit,
    onSave: (Doctor) -> Unit
) {
    var name by remember { mutableStateOf(initialDoctor?.name ?: "") }
    var title by remember { mutableStateOf(initialDoctor?.title ?: "طبيب اختصاص") }
    var specialty by remember { mutableStateOf(initialDoctor?.specialty ?: Doctor.ALL_SPECIALTIES.first()) }
    var customSpecialty by remember { mutableStateOf("") }
    var isCustomSpecialty by remember { mutableStateOf(false) }

    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            addAll(initialDoctor?.days ?: listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس"))
        }
    }

    var startHour by remember { mutableIntStateOf(initialDoctor?.startHour ?: 16) }
    var endHour by remember { mutableIntStateOf(initialDoctor?.endHour ?: 20) }
    var workingHoursText by remember {
        mutableStateOf(initialDoctor?.workingHoursText ?: "من 4:00 عصراً إلى 8:00 مساءً")
    }
    var addressLandmark by remember { mutableStateOf(initialDoctor?.addressLandmark ?: "") }

    val phoneList = remember {
        mutableStateListOf<String>().apply {
            if (initialDoctor?.phoneNumbers.isNullOrEmpty()) {
                add("078")
            } else {
                addAll(initialDoctor!!.phoneNumbers)
            }
        }
    }

    var notes by remember { mutableStateOf(initialDoctor?.notes ?: "") }
    var isEmergencyAvailable by remember { mutableStateOf(initialDoctor?.isEmergencyAvailable ?: false) }

    var newPhoneInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDoctor == null) "إضافة طبيب جديد" else "تعديل بيانات الطبيب",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل (مع اللقب د.)") },
                    placeholder = { Text("مثال: د. أحمد يونس الجبوري") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_doc_name")
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("الصفة / اللقب العلمي") },
                    placeholder = { Text("مثال: استشاري / أخصائي") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_doc_title")
                )

                // Specialty selection
                Text(
                    text = "الاختصاص الطبي:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Doctor.ALL_SPECIALTIES.forEach { spec ->
                        FilterChip(
                            selected = specialty == spec && !isCustomSpecialty,
                            onClick = {
                                specialty = spec
                                isCustomSpecialty = false
                            },
                            label = { Text(spec, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Days Selection
                Text(
                    text = "أيام التواجد في العيادة:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Doctor.ALL_DAYS.forEach { day ->
                        val selected = selectedDays.contains(day)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedDays.remove(day) else selectedDays.add(day)
                            },
                            label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Hours
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour.toString(),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) startHour = h } },
                        label = { Text("ساعة البدء (24h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endHour.toString(),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) endHour = h } },
                        label = { Text("ساعة الانتهاء") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = workingHoursText,
                    onValueChange = { workingHoursText = it },
                    label = { Text("نص أوقات الدوام الواضح") },
                    placeholder = { Text("مثال: من 4:00 عصراً إلى 8:00 مساءً") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Address & Landmark
                OutlinedTextField(
                    value = addressLandmark,
                    onValueChange = { addressLandmark = it },
                    label = { Text("العنوان والمعلم الدال في زمار") },
                    placeholder = { Text("مثال: الشارع الرئيسي - مقابل صيدلية الشفاء") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_doc_address")
                )

                // Phone Numbers
                Text(
                    text = "أرقام الحجز والاتصال:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                phoneList.forEachIndexed { index, phone ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phoneList[index] = it },
                            label = { Text("رقم الهاتف ${index + 1}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                        if (phoneList.size > 1) {
                            IconButton(onClick = { phoneList.removeAt(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف الرقم",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { phoneList.add("") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة رقم هاتف آخر")
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية (أجهزة، حجز...)") },
                    placeholder = { Text("مثال: يتوفر جهاز سونار، الحجز مسبقاً") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Emergency Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isEmergencyAvailable,
                        onCheckedChange = { isEmergencyAvailable = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "متاح للحالات الطارئة والولادات خارج الدوام",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val validPhones = phoneList.map { it.trim() }.filter { it.isNotEmpty() }
                        val doc = (initialDoctor ?: Doctor(name = name, specialty = specialty)).copy(
                            name = name.trim(),
                            title = title.trim(),
                            specialty = if (isCustomSpecialty && customSpecialty.isNotBlank()) customSpecialty.trim() else specialty,
                            days = selectedDays.toList(),
                            startHour = startHour,
                            endHour = endHour,
                            workingHoursText = workingHoursText.trim(),
                            addressLandmark = addressLandmark.trim(),
                            phoneNumbers = if (validPhones.isEmpty()) listOf("07875023922") else validPhones,
                            notes = notes.trim(),
                            isEmergencyAvailable = isEmergencyAvailable
                        )
                        onSave(doc)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_doctor_btn")
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
