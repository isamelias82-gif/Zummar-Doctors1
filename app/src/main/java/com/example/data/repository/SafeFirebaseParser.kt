package com.example.data.repository

import android.util.Log
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.model.SponsorBanner
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
            val startTime = parseString(map["startTime"]).ifBlank { "08:00 صباحاً" }
            val endTime = parseString(map["endTime"]).ifBlank { "11:00 مساءً" }
            val workingHours = parseString(map["workingHours"]).ifBlank { "من $startTime إلى $endTime" }
            val isOnDutyTonight = parseBoolean(map["isOnDutyTonight"])
            val notes = parseString(map["notes"])

            Pharmacy(
                id = id,
                name = name,
                pharmacist = pharmacist,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                onCallDays = onCallDays,
                startTime = startTime,
                endTime = endTime,
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
            val startTime = parseString(map["startTime"]).ifBlank { "07:00 صباحاً" }
            val endTime = parseString(map["endTime"]).ifBlank { "09:00 مساءً" }
            val workingHours = parseString(map["workingHours"]).ifBlank { "من $startTime إلى $endTime" }
            val services = parseStringList(map["services"])
            val notes = parseString(map["notes"])

            Laboratory(
                id = id,
                name = name,
                specialist = specialist,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                workingDays = workingDays,
                startTime = startTime,
                endTime = endTime,
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
            val startTime = parseString(map["startTime"]).ifBlank { "04:00 مساءً" }
            val endTime = parseString(map["endTime"]).ifBlank { "08:00 مساءً" }
            val workingHoursText = parseString(map["workingHoursText"]).ifBlank { "من $startTime إلى $endTime" }
            val addressLandmark = parseString(map["addressLandmark"])
            val phoneNumbers = parseStringList(map["phoneNumbers"])
            val notes = parseString(map["notes"])
            val showConsultationFee = parseBoolean(map["showConsultationFee"])
            val consultationFee = parseString(map["consultationFee"])
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
                startTime = startTime,
                endTime = endTime,
                workingHoursText = workingHoursText,
                addressLandmark = addressLandmark,
                phoneNumbers = phoneNumbers,
                notes = notes,
                showConsultationFee = showConsultationFee,
                consultationFee = consultationFee,
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

    fun jsonObjectToMap(obj: org.json.JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            if (value is org.json.JSONArray) {
                val list = mutableListOf<Any?>()
                for (i in 0 until value.length()) {
                    list.add(value.opt(i))
                }
                map[key] = list
            } else {
                map[key] = value
            }
        }
        return map
    }

    fun parseDoctorsFromJson(jsonString: String): List<Doctor> {
        val list = mutableListOf<Doctor>()
        try {
            if (jsonString.isBlank() || jsonString == "null") return emptyList()
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                val root = org.json.JSONObject(trimmed)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val childObj = root.optJSONObject(key)
                    if (childObj != null) {
                        parseSingleDoctor(key, jsonObjectToMap(childObj))?.let { list.add(it) }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        parseSingleDoctor(i.toString(), jsonObjectToMap(item))?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing doctors from JSON: ${e.message}")
        }
        return list
    }

    fun parsePharmaciesFromJson(jsonString: String): List<Pharmacy> {
        val list = mutableListOf<Pharmacy>()
        try {
            if (jsonString.isBlank() || jsonString == "null") return emptyList()
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                val root = org.json.JSONObject(trimmed)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val childObj = root.optJSONObject(key)
                    if (childObj != null) {
                        parseSinglePharmacy(key, jsonObjectToMap(childObj))?.let { list.add(it) }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        parseSinglePharmacy(i.toString(), jsonObjectToMap(item))?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pharmacies from JSON: ${e.message}")
        }
        return list
    }

    fun parseLaboratoriesFromJson(jsonString: String): List<Laboratory> {
        val list = mutableListOf<Laboratory>()
        try {
            if (jsonString.isBlank() || jsonString == "null") return emptyList()
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                val root = org.json.JSONObject(trimmed)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val childObj = root.optJSONObject(key)
                    if (childObj != null) {
                        parseSingleLaboratory(key, jsonObjectToMap(childObj))?.let { list.add(it) }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        parseSingleLaboratory(i.toString(), jsonObjectToMap(item))?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing laboratories from JSON: ${e.message}")
        }
        return list
    }

    fun parseSponsorBanner(snapshot: DataSnapshot?): SponsorBanner? {
        if (snapshot == null || !snapshot.exists()) return null
        return try {
            val map = snapshot.value as? Map<*, *> ?: return null
            parseSingleSponsorBanner(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sponsor banner snapshot: ${e.message}")
            null
        }
    }

    fun parseSingleSponsorBanner(map: Map<*, *>): SponsorBanner {
        val bannerId = parseString(map["bannerId"]).ifBlank { parseString(map["banner_id"]).ifBlank { "sponsor_01" } }
        val isActive = parseBoolean(map["isActive"] ?: map["is_active"] ?: true)
        val imagePath = parseString(map["imagePath"] ?: map["image_path"])
        val actionType = parseString(map["actionType"] ?: map["action_type"]).ifBlank { "PHONE" }
        val actionValue = parseString(map["actionValue"] ?: map["action_value"])
        val actionLink = parseString(map["actionLink"] ?: map["action_link"])
        val expiryDate = parseString(map["expiryDate"] ?: map["expiry_date"])
        val title = parseString(map["title"]).ifBlank { "مجمع النور الطبي التخصصي - زمار" }
        val description = parseString(map["description"])

        return SponsorBanner(
            bannerId = bannerId,
            isActive = isActive,
            imagePath = imagePath,
            actionType = actionType,
            actionValue = actionValue,
            expiryDate = expiryDate,
            title = title,
            description = description,
            actionLink = actionLink
        )
    }

    fun parseSponsorBannerFromJson(jsonString: String): SponsorBanner? {
        return try {
            if (jsonString.isBlank() || jsonString == "null") return null
            val obj = org.json.JSONObject(jsonString)
            val map = jsonObjectToMap(obj)
            parseSingleSponsorBanner(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sponsor banner from JSON: ${e.message}")
            null
        }
    }

    fun parseActionButtonConfig(raw: Any?, defaultInput: String = "+9647875023922"): com.example.data.model.ActionButtonConfig {
        if (raw == null) return com.example.data.model.ActionButtonConfig(isActive = true, actionInput = defaultInput, actionType = "AUTO")
        if (raw is Map<*, *>) {
            val isActive = parseBoolean(raw["isActive"] ?: raw["is_active"] ?: raw["active"] ?: true)
            val actionInput = parseString(raw["actionInput"] ?: raw["action_input"] ?: raw["actionValue"] ?: raw["action_value"] ?: raw["phone"] ?: raw["url"] ?: raw["link"]).ifBlank { defaultInput }
            val actionType = parseString(raw["actionType"] ?: raw["action_type"]).ifBlank { "AUTO" }
            return com.example.data.model.ActionButtonConfig(isActive = isActive, actionInput = actionInput, actionType = actionType)
        }
        return com.example.data.model.ActionButtonConfig(isActive = true, actionInput = parseString(raw).ifBlank { defaultInput }, actionType = "AUTO")
    }

    fun parseAppSettings(snapshot: DataSnapshot?): com.example.data.model.AppSettings {
        if (snapshot == null || !snapshot.exists()) return com.example.data.model.AppSettings()
        return try {
            val map = snapshot.value as? Map<*, *> ?: return com.example.data.model.AppSettings()
            parseSingleAppSettings(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AppSettings snapshot: ${e.message}")
            com.example.data.model.AppSettings()
        }
    }

    fun parseSingleAppSettings(map: Map<*, *>): com.example.data.model.AppSettings {
        val pharmEnabled = parseBoolean(map["pharmacies_enabled"] ?: map["pharmaciesEnabled"] ?: true)
        val labEnabled = parseBoolean(map["laboratories_enabled"] ?: map["laboratoriesEnabled"] ?: true)

        // FAB Button
        val fabRaw = map["fabButton"] ?: map["fab_button"] ?: map["fab"]
        val fabConfig = if (fabRaw != null) {
            parseActionButtonConfig(fabRaw, "+9647875023922")
        } else {
            val isActive = parseBoolean(map["fabActive"] ?: map["fab_active"] ?: true)
            val action = parseString(map["fabAction"] ?: map["fab_action"] ?: map["fabPhone"] ?: map["fab_phone"]).ifBlank { "+9647875023922" }
            val type = parseString(map["fabActionType"] ?: map["fab_action_type"]).ifBlank { "AUTO" }
            com.example.data.model.ActionButtonConfig(isActive = isActive, actionInput = action, actionType = type)
        }

        // Report Problem
        val reportRaw = map["reportProblem"] ?: map["report_problem"] ?: map["report"]
        val reportConfig = if (reportRaw != null) {
            parseActionButtonConfig(reportRaw, "+9647875023922")
        } else {
            val isActive = parseBoolean(map["reportActive"] ?: map["report_active"] ?: true)
            val action = parseString(map["reportAction"] ?: map["report_action"] ?: map["reportPhone"] ?: map["report_link"]).ifBlank { "+9647875023922" }
            val type = parseString(map["reportActionType"] ?: map["report_action_type"]).ifBlank { "AUTO" }
            com.example.data.model.ActionButtonConfig(isActive = isActive, actionInput = action, actionType = type)
        }

        // Contact Us
        val contactRaw = map["contactUs"] ?: map["contact_us"] ?: map["contact"]
        val contactConfig = if (contactRaw != null) {
            parseActionButtonConfig(contactRaw, "+9647875023922")
        } else {
            val isActive = parseBoolean(map["contactActive"] ?: map["contact_active"] ?: true)
            val action = parseString(map["contactAction"] ?: map["contact_action"] ?: map["contactPhone"] ?: map["contact_link"]).ifBlank { "+9647875023922" }
            val type = parseString(map["contactActionType"] ?: map["contact_action_type"]).ifBlank { "AUTO" }
            com.example.data.model.ActionButtonConfig(isActive = isActive, actionInput = action, actionType = type)
        }

        return com.example.data.model.AppSettings(
            pharmaciesEnabled = pharmEnabled,
            laboratoriesEnabled = labEnabled,
            fabButton = fabConfig,
            reportProblem = reportConfig,
            contactUs = contactConfig
        )
    }

    fun parseAppSettingsFromJson(jsonString: String): com.example.data.model.AppSettings? {
        return try {
            if (jsonString.isBlank() || jsonString == "null") return null
            val obj = org.json.JSONObject(jsonString)
            val map = jsonObjectToMap(obj)
            parseSingleAppSettings(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing app settings from JSON: ${e.message}")
            null
        }
    }
}
