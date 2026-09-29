package com.example.data.repository

import android.util.Log
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.google.firebase.database.DataSnapshot

/**
 * Robust parser for Firebase Realtime Database snapshots.
 * Defensively handles null, empty, unexpected types, JSON arrays, and maps to prevent
 * any ClassCastException or NullPointerException during runtime.
 */
object SafeFirebaseParser {

    private const val TAG = "SafeFirebaseParser"

    fun parsePharmacies(snapshot: DataSnapshot?): List<Pharmacy> {
        if (snapshot == null || !snapshot.exists()) return emptyList()
        val list = mutableListOf<Pharmacy>()
        try {
            if (snapshot.hasChildren()) {
                for (child in snapshot.children) {
                    parseSinglePharmacy(child.key, child.value)?.let { list.add(it) }
                }
            } else {
                when (val rawValue = snapshot.value) {
                    is List<*> -> {
                        rawValue.forEachIndexed { index, item ->
                            parseSinglePharmacy(index.toString(), item)?.let { list.add(it) }
                        }
                    }
                    is Map<*, *> -> {
                        parseSinglePharmacy(snapshot.key, rawValue)?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pharmacies: ${e.message}")
        }
        return list
    }

    fun parseSinglePharmacy(key: String?, raw: Any?): Pharmacy? {
        if (raw == null) return null
        return try {
            val map = raw as? Map<*, *> ?: return null
            val id = parseId(map["id"], key)
            val name = parseString(map["name"])
            if (name.isBlank()) return null

            val pharmacist = parseString(map["pharmacist"])
            val addressLandmark = parseString(map["addressLandmark"])
            val phoneNumbers = parseStringList(map["phoneNumbers"])
            val onCallDays = parseString(map["onCallDays"])
            val workingHours = parseString(map["workingHours"])
            val isOnDutyTonight = parseBoolean(map["isOnDutyTonight"])
            val notes = parseString(map["notes"])

            Pharmacy(
                id = id,
                name = name,
                pharmacist = pharmacist,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                onCallDays = onCallDays,
                workingHours = workingHours,
                isOnDutyTonight = isOnDutyTonight,
                notes = notes
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse single pharmacy (key=$key): ${e.message}")
            null
        }
    }

    fun parseLaboratories(snapshot: DataSnapshot?): List<Laboratory> {
        if (snapshot == null || !snapshot.exists()) return emptyList()
        val list = mutableListOf<Laboratory>()
        try {
            if (snapshot.hasChildren()) {
                for (child in snapshot.children) {
                    parseSingleLaboratory(child.key, child.value)?.let { list.add(it) }
                }
            } else {
                when (val rawValue = snapshot.value) {
                    is List<*> -> {
                        rawValue.forEachIndexed { index, item ->
                            parseSingleLaboratory(index.toString(), item)?.let { list.add(it) }
                        }
                    }
                    is Map<*, *> -> {
                        parseSingleLaboratory(snapshot.key, rawValue)?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing laboratories: ${e.message}")
        }
        return list
    }

    fun parseSingleLaboratory(key: String?, raw: Any?): Laboratory? {
        if (raw == null) return null
        return try {
            val map = raw as? Map<*, *> ?: return null
            val id = parseId(map["id"], key)
            val name = parseString(map["name"])
            if (name.isBlank()) return null

            val specialist = parseString(map["specialist"])
            val addressLandmark = parseString(map["addressLandmark"])
            val phoneNumbers = parseStringList(map["phoneNumbers"])
            val workingDays = parseString(map["workingDays"])
            val workingHours = parseString(map["workingHours"])
            val services = parseStringList(map["services"])
            val notes = parseString(map["notes"])

            Laboratory(
                id = id,
                name = name,
                specialist = specialist,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                workingDays = workingDays,
                workingHours = workingHours,
                services = services,
                notes = notes
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse single laboratory (key=$key): ${e.message}")
            null
        }
    }

    fun parseDoctors(snapshot: DataSnapshot?): List<Doctor> {
        if (snapshot == null || !snapshot.exists()) return emptyList()
        val list = mutableListOf<Doctor>()
        try {
            if (snapshot.hasChildren()) {
                for (child in snapshot.children) {
                    parseSingleDoctor(child.key, child.value)?.let { list.add(it) }
                }
            } else {
                when (val rawValue = snapshot.value) {
                    is List<*> -> {
                        rawValue.forEachIndexed { index, item ->
                            parseSingleDoctor(index.toString(), item)?.let { list.add(it) }
                        }
                    }
                    is Map<*, *> -> {
                        parseSingleDoctor(snapshot.key, rawValue)?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing doctors: ${e.message}")
        }
        return list
    }

    fun parseSingleDoctor(key: String?, raw: Any?): Doctor? {
        if (raw == null) return null
        return try {
            val map = raw as? Map<*, *> ?: return null
            val id = parseId(map["id"], key)
            val name = parseString(map["name"])
            if (name.isBlank()) return null

            val title = parseString(map["title"]).ifBlank { "طبيب اختصاص" }
            val specialty = parseString(map["specialty"])
            val days = parseStringList(map["days"])
            val startHour = parseInt(map["startHour"], 16)
            val startMinute = parseInt(map["startMinute"], 0)
            val endHour = parseInt(map["endHour"], 20)
            val endMinute = parseInt(map["endMinute"], 0)
            val workingHoursText = parseString(map["workingHoursText"])
            val addressLandmark = parseString(map["addressLandmark"])
            val phoneNumbers = parseStringList(map["phoneNumbers"])
            val notes = parseString(map["notes"])
            val isEmergencyAvailable = parseBoolean(map["isEmergencyAvailable"])
            val orderIndex = parseInt(map["orderIndex"], 0)

            Doctor(
                id = id,
                name = name,
                title = title,
                specialty = specialty,
                days = days,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                workingHoursText = workingHoursText,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                notes = notes,
                isEmergencyAvailable = isEmergencyAvailable,
                orderIndex = orderIndex
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse single doctor (key=$key): ${e.message}")
            null
        }
    }

    fun parseId(value: Any?, fallbackKey: String?): Long {
        when (value) {
            is Number -> {
                val num = value.toLong()
                if (num != 0L) return num
            }
            is String -> {
                val parsed = value.toLongOrNull()
                if (parsed != null && parsed != 0L) return parsed
            }
        }
        val fallbackParsed = fallbackKey?.toLongOrNull()
        if (fallbackParsed != null && fallbackParsed != 0L) return fallbackParsed

        // For string keys generated by Firebase RTDB push() (e.g. "-ODxyz123"), derive a positive Long ID
        if (!fallbackKey.isNullOrBlank()) {
            val hash = kotlin.math.abs(fallbackKey.hashCode().toLong())
            return if (hash == 0L) 1L else hash
        }
        if (value is String && value.isNotBlank()) {
            val hash = kotlin.math.abs(value.hashCode().toLong())
            return if (hash == 0L) 1L else hash
        }
        return kotlin.math.abs(System.identityHashCode(value).toLong()).let { if (it == 0L) 1L else it }
    }

    fun parseString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value.trim()
            else -> value.toString().trim()
        }
    }

    fun parseInt(value: Any?, default: Int): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }

    fun parseBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> {
                val s = value.trim().lowercase()
                s == "true" || s == "1" || s == "نعم"
            }
            else -> false
        }
    }

    fun parseStringList(value: Any?): List<String> {
        return when (value) {
            null -> emptyList()
            is List<*> -> {
                value.mapNotNull { item ->
                    when (item) {
                        null -> null
                        is String -> item.trim().ifEmpty { null }
                        else -> item.toString().trim().ifEmpty { null }
                    }
                }
            }
            is Map<*, *> -> {
                value.values.mapNotNull { item ->
                    when (item) {
                        null -> null
                        is String -> item.trim().ifEmpty { null }
                        else -> item.toString().trim().ifEmpty { null }
                    }
                }
            }
            is String -> {
                if (value.isBlank()) emptyList()
                else value.split(",", "،", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
            }
            is Number -> listOf(value.toString())
            else -> emptyList()
        }
    }
}
