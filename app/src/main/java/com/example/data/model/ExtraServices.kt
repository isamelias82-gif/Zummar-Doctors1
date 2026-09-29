package com.example.data.model

data class Pharmacy(
    val id: Long = 0L,
    val name: String = "",
    val pharmacist: String = "",
    val addressLandmark: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val onCallDays: String = "",
    val startTime: String = "08:00 صباحاً",
    val endTime: String = "11:00 مساءً",
    val workingHours: String = "من 08:00 صباحاً إلى 11:00 مساءً",
    val isOnDutyTonight: Boolean = false,
    val notes: String = ""
)

data class Laboratory(
    val id: Long = 0L,
    val name: String = "",
    val specialist: String = "",
    val addressLandmark: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val workingDays: String = "",
    val startTime: String = "07:00 صباحاً",
    val endTime: String = "09:00 مساءً",
    val workingHours: String = "من 07:00 صباحاً إلى 09:00 مساءً",
    val services: List<String> = emptyList(),
    val notes: String = ""
)
