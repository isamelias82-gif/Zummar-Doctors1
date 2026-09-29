package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DoctorDao
import com.example.data.model.AppSettings
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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

    private val prefs = context.getSharedPreferences("zummar_prefs", Context.MODE_PRIVATE)

    // Single sources of truth for pharmacies and laboratories
    private val pharmacyRepo: PharmacyRepository by lazy { PharmacyRepository.getInstance(context) }
    private val labRepo: LaboratoryRepository by lazy { LaboratoryRepository.getInstance(context) }

    val pharmaciesFlow: StateFlow<List<Pharmacy>> get() = pharmacyRepo.pharmaciesFlow
    val laboratoriesFlow: StateFlow<List<Laboratory>> get() = labRepo.laboratoriesFlow

    fun getPharmaciesFlow(): Flow<List<Pharmacy>> = pharmacyRepo.getPharmaciesFlow()
    fun getLaboratoriesFlow(): Flow<List<Laboratory>> = labRepo.getLaboratoriesFlow()

    private val firebaseDatabase: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Failed custom RTDB, falling back to default: ${e.message}")
            try {
                FirebaseDatabase.getInstance()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed default RTDB: ${e2.message}")
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

    private val appSettingsRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("app_settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting appSettingsRef: ${e.message}")
            null
        }
    }

    /**
     * Live Kotlin callbackFlow streaming section toggle feature flags (/app_settings) from Firebase.
     */
    fun getAppSettingsFlow(): Flow<AppSettings> = callbackFlow {
        val initialCached = AppSettings(
            pharmaciesEnabled = prefs.getBoolean("pharmacies_enabled", true),
            laboratoriesEnabled = prefs.getBoolean("laboratories_enabled", true)
        )
        trySend(initialCached)

        val ref = appSettingsRef
        if (ref == null) {
            awaitClose { }
            return@callbackFlow
        }

        try {
            ref.keepSynced(true)
        } catch (e: Exception) {
            Log.w(TAG, "keepSynced error on appSettingsRef: ${e.message}")
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val pharmChild = snapshot.child("pharmacies_enabled")
                    val labChild = snapshot.child("laboratories_enabled")

                    val pharmaciesEnabled = if (pharmChild.exists()) {
                        SafeFirebaseParser.parseBoolean(pharmChild.value)
                    } else {
                        true
                    }

                    val laboratoriesEnabled = if (labChild.exists()) {
                        SafeFirebaseParser.parseBoolean(labChild.value)
                    } else {
                        true
                    }

                    prefs.edit()
                        .putBoolean("pharmacies_enabled", pharmaciesEnabled)
                        .putBoolean("laboratories_enabled", laboratoriesEnabled)
                        .apply()

                    trySend(
                        AppSettings(
                            pharmaciesEnabled = pharmaciesEnabled,
                            laboratoriesEnabled = laboratoriesEnabled
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing app_settings: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "AppSettings listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun isPharmaciesEnabled(): Boolean = prefs.getBoolean("pharmacies_enabled", true)
    fun isLaboratoriesEnabled(): Boolean = prefs.getBoolean("laboratories_enabled", true)

    fun setPharmaciesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pharmacies_enabled", enabled).apply()
        try {
            appSettingsRef?.child("pharmacies_enabled")?.setValue(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting pharmacies_enabled in remote: ${e.message}")
        }
    }

    fun setLaboratoriesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("laboratories_enabled", enabled).apply()
        try {
            appSettingsRef?.child("laboratories_enabled")?.setValue(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting laboratories_enabled in remote: ${e.message}")
        }
    }

    init {
    }

    /**
     * Live Kotlin callbackFlow streaming real-time updates directly from Firebase Realtime Database
     * using addValueEventListener. Replaces all single-fetch get() calls and local-only caching.
     */
    fun getDoctorsFlow(): Flow<List<Doctor>> = callbackFlow {
        val ref = doctorsRef
        if (ref == null) {
            val job = launch {
                doctorDao.getAllDoctors().collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        try {
            ref.keepSynced(true)
        } catch (e: Exception) {
            Log.w(TAG, "keepSynced error on doctorsRef: ${e.message}")
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val remoteDoctors = SafeFirebaseParser.parseDoctors(snapshot)
                    if (remoteDoctors.isNotEmpty()) {
                        val sorted = remoteDoctors.sortedBy { it.id }
                        trySend(sorted)
                        CoroutineScope(Dispatchers.IO).launch {
                            doctorDao.replaceAll(sorted)
                        }
                    } else if (snapshot.exists()) {
                        // Node exists in Firebase but has 0 items (e.g. all doctors deleted)
                        trySend(emptyList())
                        CoroutineScope(Dispatchers.IO).launch {
                            doctorDao.clearAll()
                        }
                    } else {
                        // Node does not exist at all in Firebase RTDB
                        trySend(emptyList())
                        CoroutineScope(Dispatchers.IO).launch {
                            doctorDao.clearAll()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Safe fallback in doctors listener: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Doctors listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)


        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    val allDoctors: Flow<List<Doctor>> get() = getDoctorsFlow()

    fun reconnectRealtime() {
        try {
            firebaseDatabase?.goOnline()
        } catch (e: Exception) {
            Log.w(TAG, "goOnline error: ${e.message}")
        }
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
        root.toString(2)
    }

    // Import database from JSON string
    suspend fun importDatabaseFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Import disabled in read-only mode"))
        }
}
