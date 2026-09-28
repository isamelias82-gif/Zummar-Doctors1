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

    private val adminPasscodeRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("admin_passcode")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting adminPasscodeRef: ${e.message}")
            null
        }
    }

    private var cachedAdminPasscode: String = "200120012001"

    // Unified single sources of truth for pharmacies and laboratories
    private val pharmacyRepo: PharmacyRepository by lazy { PharmacyRepository.getInstance(context) }
    private val labRepo: LaboratoryRepository by lazy { LaboratoryRepository.getInstance(context) }

    val pharmaciesFlow: StateFlow<List<Pharmacy>> get() = pharmacyRepo.pharmaciesFlow
    val laboratoriesFlow: StateFlow<List<Laboratory>> get() = labRepo.laboratoriesFlow

    init {
        try {
            setupFirebaseListener()
            setupAdminPasscodeListener()
        } catch (e: Exception) {
            Log.e(TAG, "Safe init error caught: ${e.message}")
        }
    }

    fun addPharmacy(pharmacy: Pharmacy): Long = pharmacyRepo.addPharmacy(pharmacy)

    fun updatePharmacy(pharmacy: Pharmacy) {
        pharmacyRepo.updatePharmacy(pharmacy)
    }

    fun deletePharmacy(pharmacyId: Long) {
        pharmacyRepo.deletePharmacy(pharmacyId)
    }

    fun deletePharmacy(pharmacy: Pharmacy) {
        pharmacyRepo.deletePharmacy(pharmacy)
    }

    fun savePharmacies(list: List<Pharmacy>) {
        pharmacyRepo.savePharmacies(list)
    }

    fun addLaboratory(laboratory: Laboratory): Long = labRepo.addLaboratory(laboratory)

    fun updateLaboratory(laboratory: Laboratory) {
        labRepo.updateLaboratory(laboratory)
    }

    fun deleteLaboratory(laboratoryId: Long) {
        labRepo.deleteLaboratory(laboratoryId)
    }

    fun deleteLaboratory(laboratory: Laboratory) {
        labRepo.deleteLaboratory(laboratory)
    }

    fun saveLaboratories(list: List<Laboratory>) {
        labRepo.saveLaboratories(list)
    }

    suspend fun refreshPharmaciesAndLaboratories() = withContext(Dispatchers.IO) {
        try {
            pharmacyRepo.fetchFromServer()
            labRepo.fetchFromServer()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing pharmacies and labs: ${e.message}")
        }
    }

    private fun setupAdminPasscodeListener() {
        try {
            val ref = adminPasscodeRef ?: return
            try {
                ref.keepSynced(true)
            } catch (e: Exception) {
                Log.w(TAG, "keepSynced error on adminPasscodeRef: ${e.message}")
            }
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val passcode = snapshot.value?.toString()?.trim()
                        if (!passcode.isNullOrEmpty()) {
                            cachedAdminPasscode = passcode
                            prefs.edit().putString("admin_passcode", passcode).apply()
                        } else {
                            try {
                                ref.setValue("200120012001")
                            } catch (e: Exception) {}
                            cachedAdminPasscode = "200120012001"
                            prefs.edit().putString("admin_passcode", "200120012001").apply()
                        }
                    } catch (e: Exception) {
                        cachedAdminPasscode = "200120012001"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cachedAdminPasscode = prefs.getString("admin_passcode", "200120012001") ?: "200120012001"
                }
            })
        } catch (e: Exception) {
            cachedAdminPasscode = "200120012001"
        }
    }

    private fun setupFirebaseListener() {
        try {
            val ref = doctorsRef ?: return
            try {
                ref.keepSynced(true)
            } catch (e: Exception) {
                Log.w(TAG, "keepSynced error on doctorsRef: ${e.message}")
            }
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val remoteDoctors = SafeFirebaseParser.parseDoctors(snapshot)
                            if (remoteDoctors.isNotEmpty()) {
                                doctorDao.replaceAll(remoteDoctors)
                            } else if (snapshot.exists()) {
                                // Node exists but has 0 items (e.g. all doctors deleted)
                                doctorDao.clearAll()
                            } else {
                                // Node does not exist in Firebase RTDB yet -> seed defaults
                                val initial = DefaultData.initialDoctors
                                val map = initial.associateBy { it.id.toString() }
                                try {
                                    ref.setValue(map)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not seed doctors: ${e.message}")
                                }
                                doctorDao.replaceAll(initial)
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

    /**
     * Forces a direct fetch from the Firebase Realtime Database server,
     * bypassing local cache and updating Room database with remote changes and deletions.
     */
    suspend fun fetchDoctorsFromServer(): Boolean = withContext(Dispatchers.IO) {
        val ref = doctorsRef ?: return@withContext false
        try {
            val snapshot = SafeFirebaseParser.fetchDirectFromServer(ref)
            if (snapshot != null) {
                val remoteDoctors = SafeFirebaseParser.parseDoctors(snapshot)
                if (remoteDoctors.isNotEmpty()) {
                    doctorDao.replaceAll(remoteDoctors)
                } else if (snapshot.exists()) {
                    doctorDao.clearAll()
                } else {
                    val initial = DefaultData.initialDoctors
                    val map = initial.associateBy { it.id.toString() }
                    try {
                        ref.setValue(map)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not seed doctors: ${e.message}")
                    }
                    doctorDao.replaceAll(initial)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching doctors from server: ${e.message}")
            false
        }
    }

    suspend fun ensureDefaultDataLoaded() = withContext(Dispatchers.IO) {
        try {
            val fetched = fetchDoctorsFromServer()
            if (!fetched) {
                val count = doctorDao.getCount()
                if (count == 0) {
                    val initial = DefaultData.initialDoctors
                    doctorDao.replaceAll(initial)
                    try {
                        val map = initial.associateBy { it.id.toString() }
                        doctorsRef?.setValue(map)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error writing initial doctors: ${e.message}")
                    }
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
                val map = currentList.associateBy { it.id.toString() }
                doctorsRef?.setValue(map)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to Firebase: ${e.message}")
            }
        }
    }

    suspend fun insertDoctor(doctor: Doctor): Long = withContext(Dispatchers.IO) {
        val id = doctorDao.insertDoctor(doctor)
        val doctorWithId = doctor.copy(id = id)
        try {
            doctorsRef?.child(id.toString())?.setValue(doctorWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting doctor node $id: ${e.message}")
        }
        syncToFirebase()
        id
    }

    suspend fun updateDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.updateDoctor(doctor)
        try {
            doctorsRef?.child(doctor.id.toString())?.setValue(doctor)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating doctor node ${doctor.id}: ${e.message}")
        }
        syncToFirebase()
    }

    suspend fun deleteDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctor(doctor)
        try {
            doctorsRef?.child(doctor.id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing doctor node ${doctor.id}: ${e.message}")
        }
        syncToFirebase()
    }

    suspend fun deleteDoctorById(id: Long) = withContext(Dispatchers.IO) {
        doctorDao.deleteDoctorById(id)
        try {
            doctorsRef?.child(id.toString())?.removeValue()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing doctor node $id: ${e.message}")
        }
        syncToFirebase()
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        doctorDao.replaceAll(DefaultData.initialDoctors)
        syncToFirebase()
    }

    // Dynamic Admin Passcode management
    fun getAdminPasscode(): String {
        return cachedAdminPasscode.ifEmpty {
            prefs.getString("admin_passcode", "200120012001") ?: "200120012001"
        }
    }

    fun getAdminPin(): String = getAdminPasscode()

    fun setAdminPasscode(passcode: String) {
        val trimmed = passcode.trim()
        try {
            adminPasscodeRef?.setValue(trimmed)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting admin passcode: ${e.message}")
        }
        prefs.edit().putString("admin_passcode", trimmed).apply()
        cachedAdminPasscode = trimmed
    }

    fun setAdminPin(pin: String) = setAdminPasscode(pin)

    fun verifyPasscode(enteredPasscode: String): Boolean {
        return enteredPasscode.trim() == getAdminPasscode()
    }

    fun verifyPin(enteredPin: String): Boolean = verifyPasscode(enteredPin)

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
