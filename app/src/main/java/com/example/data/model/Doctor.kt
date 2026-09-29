package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val title: String = "طبيب اختصاص",
    val specialty: String,
    val days: List<String> = emptyList(),
    val startHour: Int = 16,        // 24-hr format (e.g. 16 for 4:00 PM)
    val startMinute: Int = 0,
    val endHour: Int = 20,          // 24-hr format (e.g. 20 for 8:00 PM)
    val endMinute: Int = 0,
    val startTime: String = "04:00 مساءً",
    val endTime: String = "08:00 مساءً",
    val workingHoursText: String = "من 04:00 مساءً إلى 08:00 مساءً",
    val addressLandmark: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val notes: String = "",
    val showConsultationFee: Boolean = false,
    val consultationFee: String = "",
    val isEmergencyAvailable: Boolean = false,
    val orderIndex: Int = 0
) {
    /**
     * Determines if the doctor is currently open based on system time and days.
     */
    fun isOpenNow(calendar: Calendar = Calendar.getInstance()): Boolean {
        val currentDayArabic = getCurrentDayArabic(calendar)
        if (!days.contains(currentDayArabic)) return false

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute

        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (endMinutes >= startMinutes) {
            currentTimeMinutes in startMinutes until endMinutes
        } else {
            // Over midnight shift
            currentTimeMinutes >= startMinutes || currentTimeMinutes < endMinutes
        }
    }

    /**
     * Checks whether doctor works on a given Arabic day name.
     */
    fun isAvailableOnDay(dayArabic: String): Boolean {
        return days.contains(dayArabic)
    }

    companion object {
        fun getCurrentDayArabic(calendar: Calendar = Calendar.getInstance()): String {
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "السبت"
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الإثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                else -> "السبت"
            }
        }

        val ALL_DAYS = listOf(
            "السبت",
            "الأحد",
            "الإثنين",
            "الثلاثاء",
            "الأربعاء",
            "الخميس",
            "الجمعة"
        )

        val ALL_SPECIALTIES = listOf(
            "طب الأطفال",
            "النسائية والتوليد",
            "الباطنية والقلبية",
            "طب وجراحة الأسنان",
            "جراحة العظام والمفاصل",
            "الجراحة العامة والناظورية",
            "الأنف والأذن والحنجرة",
            "العيون وجراحتها",
            "الجلدية والتجميل",
            "المسالك البولية والكلى",
            "الأشعة والسونار"
        )
    }
}
