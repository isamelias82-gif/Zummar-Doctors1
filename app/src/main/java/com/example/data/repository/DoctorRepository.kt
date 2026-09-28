package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DefaultData
import com.example.data.local.DoctorDao
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.model.SponsorBanner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DoctorRepository(
    private val doctorDao: DoctorDao,
    private val context: Context
) {
    companion object {
        private const val TAG = "DoctorRepository"
        private const val RTDB_URL = "https://zummar-doctors-default-rtdb.firebaseio.com"
    }

    val allDoctors: Flow<List<Doctor>> = doctorDao.getAllDoctors()

    private val prefs = context.getSharedPreferences("zummar_prefs", Context.MODE_PRIVATE)

    // Lazily and safely retrieve Firebase references without risking crashes
    private val firebaseDatabase: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get custom RTDB instance, falling back to default: ${e.message}")
            try {
                FirebaseDatabase.getInstance()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to get default RTDB instance: ${e2.message}")
                null
            }
        }
    }

    private val doctorsRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("doctors")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doctorsRef: ${e.message}")
            null
        }
    }

    private val pharmaciesRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("pharmacies")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pharmaciesRef: ${e.message}")
            null
        }
    }

    private val laboratoriesRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("laboratories")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting laboratoriesRef: ${e.message}")
            null
        }
    }

    private val adminPinRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("settings/admin_pin")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting adminPinRef: ${e.message}")
            null
        }
    }

    private var cachedAdminPin: String = "200120012001"

    private val _pharmaciesFlow = MutableStateFlow<List<Pharmacy>>(DefaultData.initialPharmacies)
    val pharmaciesFlow: StateFlow<List<Pharmacy>> = _pharmaciesFlow

    private val _laboratoriesFlow = MutableStateFlow<List<Laboratory>>(DefaultData.initialLaboratories)
    val laboratoriesFlow: StateFlow<List<Laboratory>> = _laboratoriesFlow

    init {
        try {
            setupFirebaseListener()
            setupAdminPinListener()
            setupPharmaciesListener()
            setupLaboratoriesListener()
        } catch (e: Exception) {
            Log.e(TAG, "Safe init error caught: ${e.message}")
        }
    }

    private fun setupPharmaciesListener() {
        try {
            val ref = pharmaciesRef ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val parsed = SafeFirebaseParser.parsePharmacies(snapshot)
                            if (parsed.isNotEmpty()) {
                                _pharmaciesFlow.value = parsed
                            } else {
                                // If database is truly empty, seed defaults
                                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                                    try {
                                        ref.setValue(DefaultData.initialPharmacies)
                                    } catch (se: Exception) {
                                        Log.w(TAG, "Cannot seed pharmacies: ${se.message}")
                                    }
                                }
                                _pharmaciesFlow.value = DefaultData.initialPharmacies
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing pharmacies: ${e.message}")
                            _pharmaciesFlow.value = DefaultData.initialPharmacies
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Pharmacies listener cancelled: ${error.message}")
                    _pharmaciesFlow.value = DefaultData.initialPharmacies
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach pharmacies listener: ${e.message}")
            _pharmaciesFlow.value = DefaultData.initialPharmacies
        }
    }

    private fun setupLaboratoriesListener() {
        try {
            val ref = laboratoriesRef ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val parsed = SafeFirebaseParser.parseLaboratories(snapshot)
                            if (parsed.isNotEmpty()) {
                                _laboratoriesFlow.value = parsed
                            } else {
                                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                                    try {
                                        ref.setValue(DefaultData.initialLaboratories)
                                    } catch (se: Exception) {
                                        Log.w(TAG, "Cannot seed laboratories: ${se.message}")
                                    }
                                }
                                _laboratoriesFlow.value = DefaultData.initialLaboratories
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing laboratories: ${e.message}")
                            _laboratoriesFlow.value = DefaultData.initialLaboratories
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Laboratories listener cancelled: ${error.message}")
                    _laboratoriesFlow.value = DefaultData.initialLaboratories
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach laboratories listener: ${e.message}")
            _laboratoriesFlow.value = DefaultData.initialLaboratories
        }
    }

    fun savePharmacies(list: List<Pharmacy>) {
        _pharmaciesFlow.value = list
        try {
            pharmaciesRef?.setValue(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving pharmacies: ${e.message}")
        }
    }

    fun saveLaboratories(list: List<Laboratory>) {
        _laboratoriesFlow.value = list
        try {
            laboratoriesRef?.setValue(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving laboratories: ${e.message}")
        }
    }

    private fun setupAdminPinListener() {
        try {
            val ref = adminPinRef ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val pin = snapshot.value?.toString()?.trim()
                        if (!pin.isNullOrEmpty()) {
                            cachedAdminPin = pin
                            prefs.edit().putString("admin_pin", pin).apply()
                        } else {
                            try {
                                ref.setValue("200120012001")
                            } catch (e: Exception) {}
                            cachedAdminPin = "200120012001"
                            prefs.edit().putString("admin_pin", "200120012001").apply()
                        }
                    } catch (e: Exception) {
                        cachedAdminPin = "200120012001"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cachedAdminPin = prefs.getString("admin_pin", "200120012001") ?: "200120012001"
                }
            })
        } catch (e: Exception) {
            cachedAdminPin = "200120012001"
        }
    }

    private fun setupFirebaseListener() {
        try {
            val ref = doctorsRef ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val remoteDoctors = SafeFirebaseParser.parseDoctors(snapshot)
                            if (remoteDoctors.isNotEmpty()) {
                                doctorDao.clearAll()
                                doctorDao.insertAllDoctors(remoteDoctors)
                            } else {
                                val localCount = doctorDao.getCount()
                                val seedList = if (localCount > 0) doctorDao.getAllDoctors().first() else DefaultData.initialDoctors
                                try {
                                    ref.setValue(seedList)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not seed doctors: ${e.message}")
                                }
                                if (localCount == 0) {
                                    doctorDao.insertAllDoctors(seedList)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Safe fallback in doctors listener: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Doctors listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach doctors listener: ${e.message}")
        }
    }

    suspend fun ensureDefaultDataLoaded() = withContext(Dispatchers.IO) {
        try {
            val count = doctorDao.getCount()
            if (count == 0) {
                val initial = DefaultData.initialDoctors
                doctorDao.insertAllDoctors(initial)
                try {
                    doctorsRef?.setValue(initial)
                } catch (e: Exception) {
                    Log.w(TAG, "Error writing initial doctors: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring default data loaded: ${e.message}")
        }
    }

    private fun syncToFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentList = doctorDao.getAllDoctors().first()
                doctorsRef?.setValue(currentList)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to Firebase: ${e.message}")
            }
        }
    }

    suspend fun insertDoctor(doctor: Doctor): Long = withContext(Dispatchers.IO) {
        val id = doctorDao.insertDoctor(doctor)
        syncToFirebase()
        id
    }

    suspend fun updateDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.updateDoctor(doctor)
        syncToFirebase()
    }

    suspend fun deleteDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctor(doctor)
        syncToFirebase()
    }

    suspend fun deleteDoctorById(id: Long) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctorById(id)
        syncToFirebase()
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        doctorDao.clearAll()
        doctorDao.insertAllDoctors(DefaultData.initialDoctors)
        syncToFirebase()
    }

    // Admin PIN management
    fun getAdminPin(): String {
        return cachedAdminPin.ifEmpty {
            prefs.getString("admin_pin", "200120012001") ?: "200120012001"
        }
    }

    fun setAdminPin(pin: String) {
        try {
            adminPinRef?.setValue(pin)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting admin PIN: ${e.message}")
        }
        prefs.edit().putString("admin_pin", pin).apply()
        cachedAdminPin = pin
    }

    fun verifyPin(enteredPin: String): Boolean {
        return enteredPin == getAdminPin()
    }

    // Sponsor Banner Management
    fun getSponsorBanner(): SponsorBanner {
        val bannerJson = prefs.getString("sponsor_banner_json", null)
        if (bannerJson == null) {
            return SponsorBanner.defaultBanner
        }
        return try {
            val obj = JSONObject(bannerJson)
            SponsorBanner(
                bannerId = obj.optString("banner_id", "sponsor_01"),
                isActive = obj.optBoolean("is_active", true),
                imagePath = obj.optString("image_path", ""),
                actionType = obj.optString("action_type", "WHATSAPP"),
                actionValue = obj.optString("action_value", "+9647875023922"),
                expiryDate = obj.optString("expiry_date", "2026-12-31"),
                title = obj.optString("title", "مجمع النور الطبي التخصصي - زمار")
            )
        } catch (e: Exception) {
            SponsorBanner.defaultBanner
        }
    }

    fun saveSponsorBanner(banner: SponsorBanner) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sponsor banner: ${e.message}")
        }
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
                val banner = SponsorBanner(
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
                syncToFirebase()
                Result.success(importedDoctors.size)
            } else {
                Result.failure(Exception("الملف لا يحتوي على أطباء"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
