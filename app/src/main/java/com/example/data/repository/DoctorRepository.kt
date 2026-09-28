package com.example.data.repository

import android.content.Context
import com.example.data.local.DoctorDao
import com.example.data.local.DefaultData
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
    val allDoctors: Flow<List<Doctor>> = doctorDao.getAllDoctors()

    private val prefs = context.getSharedPreferences("zummar_prefs", Context.MODE_PRIVATE)

    private val firebaseDatabase = FirebaseDatabase.getInstance("https://zummar-doctors-default-rtdb.firebaseio.com")
    private val doctorsRef = firebaseDatabase.getReference("doctors")
    private val pharmaciesRef = firebaseDatabase.getReference("pharmacies")
    private val laboratoriesRef = firebaseDatabase.getReference("laboratories")
    private val adminPinRef = firebaseDatabase.getReference("settings/admin_pin")
    private var cachedAdminPin: String = "1234"

    private val _pharmaciesFlow = MutableStateFlow<List<Pharmacy>>(DefaultData.initialPharmacies)
    val pharmaciesFlow: StateFlow<List<Pharmacy>> = _pharmaciesFlow

    private val _laboratoriesFlow = MutableStateFlow<List<Laboratory>>(DefaultData.initialLaboratories)
    val laboratoriesFlow: StateFlow<List<Laboratory>> = _laboratoriesFlow

    init {
        setupFirebaseListener()
        setupAdminPinListener()
        setupPharmaciesListener()
        setupLaboratoriesListener()
    }

    private fun setupPharmaciesListener() {
        try {
            pharmaciesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val remoteList = mutableListOf<Pharmacy>()
                            if (snapshot.exists() && snapshot.childrenCount > 0) {
                                for (child in snapshot.children) {
                                    try {
                                        val map = child.value as? Map<*, *>
                                        if (map != null) {
                                            val id = (map["id"] as? Number)?.toLong() ?: 0L
                                            val name = map["name"] as? String ?: ""
                                            val pharmacist = map["pharmacist"] as? String ?: ""
                                            val addressLandmark = map["addressLandmark"] as? String ?: ""
                                            val phonesObj = map["phoneNumbers"]
                                            val phoneNumbers = when (phonesObj) {
                                                is List<*> -> phonesObj.filterIsInstance<String>()
                                                else -> emptyList()
                                            }
                                            val onCallDays = map["onCallDays"] as? String ?: ""
                                            val workingHours = map["workingHours"] as? String ?: ""
                                            val isOnDutyTonight = (map["isOnDutyTonight"] as? Boolean) ?: false
                                            val notes = map["notes"] as? String ?: ""

                                            remoteList.add(
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
                                            )
                                        }
                                    } catch (e: Exception) {}
                                }
                                if (remoteList.isNotEmpty()) {
                                    _pharmaciesFlow.value = remoteList
                                } else {
                                    _pharmaciesFlow.value = DefaultData.initialPharmacies
                                }
                            } else {
                                pharmaciesRef.setValue(DefaultData.initialPharmacies)
                                _pharmaciesFlow.value = DefaultData.initialPharmacies
                            }
                        } catch (e: Exception) {
                            _pharmaciesFlow.value = DefaultData.initialPharmacies
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            _pharmaciesFlow.value = DefaultData.initialPharmacies
        }
    }

    private fun setupLaboratoriesListener() {
        try {
            laboratoriesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val remoteList = mutableListOf<Laboratory>()
                            if (snapshot.exists() && snapshot.childrenCount > 0) {
                                for (child in snapshot.children) {
                                    try {
                                        val map = child.value as? Map<*, *>
                                        if (map != null) {
                                            val id = (map["id"] as? Number)?.toLong() ?: 0L
                                            val name = map["name"] as? String ?: ""
                                            val specialist = map["specialist"] as? String ?: ""
                                            val addressLandmark = map["addressLandmark"] as? String ?: ""
                                            val phonesObj = map["phoneNumbers"]
                                            val phoneNumbers = when (phonesObj) {
                                                is List<*> -> phonesObj.filterIsInstance<String>()
                                                else -> emptyList()
                                            }
                                            val workingDays = map["workingDays"] as? String ?: ""
                                            val workingHours = map["workingHours"] as? String ?: ""
                                            val servicesObj = map["services"]
                                            val services = when (servicesObj) {
                                                is List<*> -> servicesObj.filterIsInstance<String>()
                                                else -> emptyList()
                                            }
                                            val notes = map["notes"] as? String ?: ""

                                            remoteList.add(
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
                                            )
                                        }
                                    } catch (e: Exception) {}
                                }
                                if (remoteList.isNotEmpty()) {
                                    _laboratoriesFlow.value = remoteList
                                } else {
                                    _laboratoriesFlow.value = DefaultData.initialLaboratories
                                }
                            } else {
                                laboratoriesRef.setValue(DefaultData.initialLaboratories)
                                _laboratoriesFlow.value = DefaultData.initialLaboratories
                            }
                        } catch (e: Exception) {
                            _laboratoriesFlow.value = DefaultData.initialLaboratories
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            _laboratoriesFlow.value = DefaultData.initialLaboratories
        }
    }

    fun savePharmacies(list: List<Pharmacy>) {
        _pharmaciesFlow.value = list
        try {
            pharmaciesRef.setValue(list)
        } catch (e: Exception) {}
    }

    fun saveLaboratories(list: List<Laboratory>) {
        _laboratoriesFlow.value = list
        try {
            laboratoriesRef.setValue(list)
        } catch (e: Exception) {}
    }

    private fun setupAdminPinListener() {
        try {
            adminPinRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val pinObj = snapshot.value
                        val pin = when (pinObj) {
                            is String -> pinObj
                            is Number -> pinObj.toString()
                            else -> pinObj?.toString()
                        }
                        if (!pin.isNullOrEmpty()) {
                            cachedAdminPin = pin
                            prefs.edit().putString("admin_pin", pin).apply()
                        } else {
                            adminPinRef.setValue("1234")
                            cachedAdminPin = "1234"
                            prefs.edit().putString("admin_pin", "1234").apply()
                        }
                    } catch (e: Exception) {
                        cachedAdminPin = "1234"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            cachedAdminPin = "1234"
        }
    }

    private fun setupFirebaseListener() {
        try {
            doctorsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val remoteDoctors = mutableListOf<Doctor>()
                            if (snapshot.exists() && snapshot.childrenCount > 0) {
                                for (child in snapshot.children) {
                                    try {
                                        val map = child.value as? Map<*, *>
                                        if (map != null) {
                                            val id = (map["id"] as? Number)?.toLong() ?: 0L
                                            val name = map["name"] as? String ?: ""
                                            val title = map["title"] as? String ?: "طبيب اختصاص"
                                            val specialty = map["specialty"] as? String ?: ""

                                            val daysObj = map["days"]
                                            val daysList = when (daysObj) {
                                                is List<*> -> daysObj.filterIsInstance<String>()
                                                else -> emptyList()
                                            }

                                            val startHour = (map["startHour"] as? Number)?.toInt() ?: 16
                                            val startMinute = (map["startMinute"] as? Number)?.toInt() ?: 0
                                            val endHour = (map["endHour"] as? Number)?.toInt() ?: 20
                                            val endMinute = (map["endMinute"] as? Number)?.toInt() ?: 0
                                            val workingHoursText = map["workingHoursText"] as? String ?: ""
                                            val addressLandmark = map["addressLandmark"] as? String ?: ""

                                            val phonesObj = map["phoneNumbers"]
                                            val phoneNumbers = when (phonesObj) {
                                                is List<*> -> phonesObj.filterIsInstance<String>()
                                                else -> emptyList()
                                            }

                                            val notes = map["notes"] as? String ?: ""
                                            val isEmergencyAvailable = (map["isEmergencyAvailable"] as? Boolean) ?: false
                                            val orderIndex = (map["orderIndex"] as? Number)?.toInt() ?: 0

                                            val doc = Doctor(
                                                id = id,
                                                name = name,
                                                title = title,
                                                specialty = specialty,
                                                days = daysList,
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
                                            remoteDoctors.add(doc)
                                        }
                                    } catch (e: Exception) {
                                        // Ignore malformed children
                                    }
                                }
                                if (remoteDoctors.isNotEmpty()) {
                                    doctorDao.clearAll()
                                    doctorDao.insertAllDoctors(remoteDoctors)
                                }
                            } else {
                                // Database is empty (null): seed initial doctor records for "Zummar Doctors"
                                val localCount = doctorDao.getCount()
                                val seedList = if (localCount > 0) doctorDao.getAllDoctors().first() else DefaultData.initialDoctors
                                doctorsRef.setValue(seedList)
                                if (localCount == 0) {
                                    doctorDao.insertAllDoctors(seedList)
                                }
                            }
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle cancellation
                }
            })
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    suspend fun ensureDefaultDataLoaded() = withContext(Dispatchers.IO) {
        val count = doctorDao.getCount()
        if (count == 0) {
            val initial = DefaultData.initialDoctors
            doctorDao.insertAllDoctors(initial)
            doctorsRef.setValue(initial)
        }
    }

    private fun syncToFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentList = doctorDao.getAllDoctors().first()
            doctorsRef.setValue(currentList)
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
            prefs.getString("admin_pin", "1234") ?: "1234"
        }
    }

    fun setAdminPin(pin: String) {
        adminPinRef.setValue(pin)
        prefs.edit().putString("admin_pin", pin).apply()
        cachedAdminPin = pin
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
