package com.example.data.model

data class Pharmacy(
    val id: Long = 0L,
    val name: String = "",
    val pharmacist: String = "",
    val addressLandmark: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val onCallDays: String = "",
    val workingHours: String = "",
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
    val workingHours: String = "",
    val services: List<String> = emptyList(),
    val notes: String = ""
)
