package com.example.data.repository

import android.content.Context
import com.example.data.local.DoctorDao
import com.example.data.local.DefaultData
import com.example.data.model.Doctor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DoctorRepository(
    private val doctorDao: DoctorDao,
    private val context: Context
) {
    val allDoctors: Flow<List<Doctor>> = doctorDao.getAllDoctors()

    private val prefs = context.getSharedPreferences("zummar_prefs", Context.MODE_PRIVATE)

    suspend fun ensureDefaultDataLoaded() = withContext(Dispatchers.IO) {
        val count = doctorDao.getCount()
        if (count == 0) {
            doctorDao.insertAllDoctors(DefaultData.initialDoctors)
        }
    }

    suspend fun insertDoctor(doctor: Doctor): Long = withContext(Dispatchers.IO) {
        doctorDao.insertDoctor(doctor)
    }

    suspend fun updateDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.updateDoctor(doctor)
    }

    suspend fun deleteDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctor(doctor)
    }

    suspend fun deleteDoctorById(id: Long) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctorById(id)
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        doctorDao.clearAll()
        doctorDao.insertAllDoctors(DefaultData.initialDoctors)
    }

    // Admin PIN management
    fun getAdminPin(): String {
        val storedPin = prefs.getString("admin_pin", null)
        if (storedPin == null || storedPin == "1982") {
            setAdminPin("200120012001")
            return "200120012001"
        }
        return storedPin
    }

    fun setAdminPin(pin: String) {
        prefs.edit().putString("admin_pin", pin).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        return enteredPin == getAdminPin()
    }

    // Sponsor Banner Management
    fun getSponsorBanner(): com.example.data.model.SponsorBanner {
        val bannerJson = prefs.getString("sponsor_banner_json", null)
        if (bannerJson == null) {
            return com.example.data.model.SponsorBanner.defaultBanner
        }
        return try {
            val obj = JSONObject(bannerJson)
            com.example.data.model.SponsorBanner(
                bannerId = obj.optString("banner_id", "sponsor_01"),
                isActive = obj.optBoolean("is_active", true),
                imagePath = obj.optString("image_path", ""),
                actionType = obj.optString("action_type", "WHATSAPP"),
                actionValue = obj.optString("action_value", "+9647875023922"),
                expiryDate = obj.optString("expiry_date", "2026-12-31"),
                title = obj.optString("title", "مجمع النور الطبي التخصصي - زمار")
            )
        } catch (e: Exception) {
            com.example.data.model.SponsorBanner.defaultBanner
        }
    }

    fun saveSponsorBanner(banner: com.example.data.model.SponsorBanner) {
        val obj = JSONObject().apply {
            put("banner_id", banner.bannerId)
            put("is_active", banner.isActive)
            put("image_path", banner.imagePath)
            put("action_type", banner.actionType)
            put("action_value", banner.actionValue)
            put("expiry_date", banner.expiryDate)
            put("title", banner.title)
        }
        prefs.edit().putString("sponsor_banner_json", obj.toString()).apply()
    }

    // Secondary Sections (Pharmacies & Laboratories) Availability Control
    fun isPharmaciesEnabled(): Boolean {
        return prefs.getBoolean("is_pharmacies_enabled", false)
    }

    fun setPharmaciesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_pharmacies_enabled", enabled).apply()
    }

    fun isLaboratoriesEnabled(): Boolean {
        return prefs.getBoolean("is_laboratories_enabled", false)
    }

    fun setLaboratoriesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_laboratories_enabled", enabled).apply()
    }

    // Export database to JSON string
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val doctors = doctorDao.getAllDoctors().first()
        val jsonArray = JSONArray()

        for (doc in doctors) {
            val obj = JSONObject()
            obj.put("id", doc.id)
            obj.put("name", doc.name)
            obj.put("title", doc.title)
            obj.put("specialty", doc.specialty)
            obj.put("days", JSONArray(doc.days))
            obj.put("startHour", doc.startHour)
            obj.put("startMinute", doc.startMinute)
            obj.put("endHour", doc.endHour)
            obj.put("endMinute", doc.endMinute)
            obj.put("workingHoursText", doc.workingHoursText)
            obj.put("addressLandmark", doc.addressLandmark)
            obj.put("phoneNumbers", JSONArray(doc.phoneNumbers))
            obj.put("notes", doc.notes)
            obj.put("isEmergencyAvailable", doc.isEmergencyAvailable)
            obj.put("orderIndex", doc.orderIndex)
            jsonArray.put(obj)
        }

        val banner = getSponsorBanner()
        val bannerObj = JSONObject().apply {
            put("banner_id", banner.bannerId)
            put("is_active", banner.isActive)
            put("image_path", banner.imagePath)
            put("action_type", banner.actionType)
            put("action_value", banner.actionValue)
            put("expiry_date", banner.expiryDate)
            put("title", banner.title)
        }

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "أطباء زمار")
        root.put("doctors", jsonArray)
        root.put("sponsor_banner", bannerObj)
        root.put("is_pharmacies_enabled", isPharmaciesEnabled())
        root.put("is_laboratories_enabled", isLaboratoriesEnabled())
        root.toString(2)
    }

    // Import database from JSON string
    suspend fun importDatabaseFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("is_pharmacies_enabled")) {
                setPharmaciesEnabled(root.getBoolean("is_pharmacies_enabled"))
            }
            if (root.has("is_laboratories_enabled")) {
                setLaboratoriesEnabled(root.getBoolean("is_laboratories_enabled"))
            }
            if (root.has("sponsor_banner")) {
                val bObj = root.getJSONObject("sponsor_banner")
                val banner = com.example.data.model.SponsorBanner(
                    bannerId = bObj.optString("banner_id", "sponsor_01"),
                    isActive = bObj.optBoolean("is_active", true),
                    imagePath = bObj.optString("image_path", ""),
                    actionType = bObj.optString("action_type", "WHATSAPP"),
                    actionValue = bObj.optString("action_value", "+9647875023922"),
                    expiryDate = bObj.optString("expiry_date", ""),
                    title = bObj.optString("title", "مجمع النور الطبي التخصصي")
                )
                saveSponsorBanner(banner)
            }
            val jsonArray = root.getJSONArray("doctors")
            val importedDoctors = mutableListOf<Doctor>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val daysList = mutableListOf<String>()
                val daysArr = obj.optJSONArray("days")
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) {
                        daysList.add(daysArr.getString(j))
                    }
                }

                val phonesList = mutableListOf<String>()
                val phonesArr = obj.optJSONArray("phoneNumbers")
                if (phonesArr != null) {
                    for (j in 0 until phonesArr.length()) {
                        phonesList.add(phonesArr.getString(j))
                    }
                }

                val doc = Doctor(
                    id = if (obj.has("id")) obj.getLong("id") else 0,
                    name = obj.getString("name"),
                    title = obj.optString("title", "طبيب اختصاص"),
                    specialty = obj.getString("specialty"),
                    days = daysList,
                    startHour = obj.optInt("startHour", 16),
                    startMinute = obj.optInt("startMinute", 0),
                    endHour = obj.optInt("endHour", 20),
                    endMinute = obj.optInt("endMinute", 0),
                    workingHoursText = obj.optString("workingHoursText", ""),
                    addressLandmark = obj.optString("addressLandmark", ""),
                    phoneNumbers = phonesList,
                    notes = obj.optString("notes", ""),
                    isEmergencyAvailable = obj.optBoolean("isEmergencyAvailable", false),
                    orderIndex = obj.optInt("orderIndex", i + 1)
                )
                importedDoctors.add(doc)
            }

            if (importedDoctors.isNotEmpty()) {
                doctorDao.clearAll()
                doctorDao.insertAllDoctors(importedDoctors)
                Result.success(importedDoctors.size)
            } else {
                Result.failure(Exception("الملف لا يحتوي على أطباء"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
