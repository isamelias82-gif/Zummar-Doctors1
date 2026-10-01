package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.ui.theme.MedicalCardBorder
import com.example.ui.theme.StatusClosed
import com.example.ui.theme.StatusClosedContainer
import com.example.ui.theme.StatusOpen
import com.example.ui.theme.StatusOpenContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorCard(
    doctor: Doctor,
    currentDayArabic: String,
    selectedDay: String? = null,
    shareTemplate: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDay = selectedDay ?: currentDayArabic
    val isAvailableOnActiveDay = doctor.isAvailableOnDay(activeDay)
    val isToday = (selectedDay == null || selectedDay == currentDayArabic)
    val isOpen = isToday && doctor.isOpenNow()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("doctor_card_${doctor.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MedicalCardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Avatar/Icon, Name, Specialty, and Live/Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = doctor.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${doctor.title} - ${doctor.specialty}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Status Badge (Live if today, or working status on selected day)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isToday) {
                        if (isOpen) StatusOpenContainer else StatusClosedContainer
                    } else {
                        if (isAvailableOnActiveDay) StatusOpenContainer else StatusClosedContainer
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isToday) {
                                        if (isOpen) StatusOpen else StatusClosed
                                    } else {
                                        if (isAvailableOnActiveDay) StatusOpen else StatusClosed
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isToday) {
                                if (isOpen) "مفتوح الآن" else "مغلق حالياً"
                            } else {
                                if (isAvailableOnActiveDay) "متاح في $activeDay" else "غير متاح اليوم"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) {
                                    if (isOpen) Color(0xFF065F46) else Color(0xFF475569)
                                } else {
                                    if (isAvailableOnActiveDay) Color(0xFF065F46) else Color(0xFF475569)
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Working Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "أيام التواجد: ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        if (isAvailableOnActiveDay) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = if (isToday) "متواجد اليوم" else "متواجد في $activeDay",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusClosedContainer,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "غير متاح اليوم",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusClosed,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = doctor.days.joinToString("، "),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Working Hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ساعات الدوام: ${doctor.workingHoursText.ifEmpty { "من ${doctor.startHour}:00 إلى ${doctor.endHour}:00" }}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address / Landmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doctor.addressLandmark,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            // Notes / Features if present
            if (doctor.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ملاحظات: ${doctor.notes}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Consultation Fee if enabled by admin
            if (doctor.showConsultationFee && doctor.consultationFee.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سعر الكشفية: ${doctor.consultationFee}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Numbers section: Multiple booking numbers with native dialer intent
            Text(
                text = "أرقام الحجز والاتصال المباشر:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                doctor.phoneNumbers.forEach { phone ->
                    FilledTonalButton(
                        onClick = {
                            dialPhoneNumber(context, phone)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("call_btn_${phone}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Share Button and Report Issue Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Button (100% Dynamic Template from Admin Panel)
                OutlinedButton(
                    onClick = {
                        shareDoctorCard(context, doctor, shareTemplate)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_btn_${doctor.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "مشاركة", style = MaterialTheme.typography.labelMedium)
                }

                // Report Error Button (WhatsApp to admin)
                OutlinedButton(
                    onClick = {
                        reportIssueToAdmin(context, doctor)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("report_btn_${doctor.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "إبلاغ عن خطأ",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إبلاغ عن خطأ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

/**
 * Manager to format doctor share message with 100% fidelity to Web Admin Panel settings.
 */
object DoctorShareManager {
    @Volatile
    var cachedTemplate: String = ""
    @Volatile
    var cachedFooterText: String = ""

    const val DEFAULT_DOCTOR_DETAILS = """🩺 *بطاقة طبيب - دليل أطباء زمار* 🩺
----------------------------
👨‍⚕️ *الاسم:* {doctor_name}
📋 *الاختصاص:* {specialty}
📍 *العنوان:* {address}
📞 *أرقام الحجز:* {phone}"""

    const val DEFAULT_FOOTER_TEXT = """📲 للتواصل وحجز المواعيد، تحميل تطبيق أطباء زمار:
https://chat.crisp.chat/l/50ac8743-e9cf-4f46-a2f1-888d6724bd72"""

    fun formatMessage(template: String, doctor: Doctor): String {
        val phoneStr = doctor.phoneNumbers.filter { it.isNotBlank() }.joinToString(" | ")
        val specialtyStr = if (doctor.title.isNotBlank() && !doctor.specialty.contains(doctor.title)) {
            "${doctor.title} - ${doctor.specialty}"
        } else {
            doctor.specialty
        }
        val appLink = "https://chat.crisp.chat/l/50ac8743-e9cf-4f46-a2f1-888d6724bd72"

        val rawTemplate = if (template.isNotBlank() && template.contains("{doctor_name}")) {
            template
        } else if (cachedTemplate.isNotBlank() && cachedTemplate.contains("{doctor_name}")) {
            cachedTemplate
        } else {
            DEFAULT_DOCTOR_DETAILS
        }

        val doctorDetails = rawTemplate
            .replace("{doctor_name}", doctor.name)
            .replace("{specialty}", specialtyStr)
            .replace("{phone}", phoneStr)
            .replace("{address}", doctor.addressLandmark)
            .replace("{app_link}", appLink)
            .replace("{days}", doctor.days.joinToString("، "))
            .replace("{working_hours}", doctor.workingHoursText)
            .replace("{hours}", doctor.workingHoursText)
            .replace("{notes}", doctor.notes)

        val footer = if (cachedFooterText.isNotBlank()) {
            cachedFooterText
        } else {
            DEFAULT_FOOTER_TEXT
        }

        return "$doctorDetails\n\n$footer"
    }
}

/**
 * Opens native dialer prefilled with number
 */
fun dialPhoneNumber(context: Context, phoneNumber: String) {
    try {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Shares doctor card text formatted cleanly from Admin Panel template
 */
fun shareDoctorCard(context: Context, doctor: Doctor, template: String = "") {
    val shareBody = DoctorShareManager.formatMessage(template, doctor)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "معلومات الطبيب: ${doctor.name}")
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة بطاقة الطبيب عبر"))
}

/**
 * Reports doctor information issue dynamically to admin via in-app Crisp chat (Custom Tabs / WebView)
 * using the URL fetched dynamically from the Admin Panel.
 */
fun reportIssueToAdmin(context: Context, doctor: Doctor) {
    val dynamicUrl = com.example.util.SupportChatManager.cachedReportUrl.ifBlank {
        com.example.util.SupportChatManager.cachedCrispUrl
    }.ifBlank {
        com.example.util.SupportChatManager.DEFAULT_CRISP_URL
    }
    com.example.util.SupportChatManager.openInAppChat(context, dynamicUrl)
}
