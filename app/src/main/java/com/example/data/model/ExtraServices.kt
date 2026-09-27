package com.example.data.model

data class Pharmacy(
    val id: Long,
    val name: String,
    val pharmacist: String,
    val addressLandmark: String,
    val phoneNumbers: List<String>,
    val onCallDays: String,
    val workingHours: String,
    val isOnDutyTonight: Boolean = false,
    val notes: String = ""
)

data class Laboratory(
    val id: Long,
    val name: String,
    val specialist: String,
    val addressLandmark: String,
    val phoneNumbers: List<String>,
    val workingDays: String,
    val workingHours: String,
    val services: List<String>,
    val notes: String = ""
)
