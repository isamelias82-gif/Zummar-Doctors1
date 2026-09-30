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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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

    private val settingsRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting settingsRef: ${e.message}")
            null
        }
    }

    fun getAppSettingsFlow(): Flow<AppSettings> = callbackFlow {
        // Fast REST fetch & background polling engine
        val restJob = launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val settings = FirebaseRestHelper.fetchAppSettings()
                    if (settings != null) {
                        trySend(settings)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Settings REST poll error: ${e.message}")
                }
                delay(8000)
            }
        }

        val ref1 = appSettingsRef
        val ref2 = settingsRef
        val footerRef = firebaseDatabase?.getReference("settings/doctor_share_footer_text")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val settings = SafeFirebaseParser.parseAppSettings(snapshot)
                    trySend(settings)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing app_settings: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "appSettings listener cancelled: ${error.message}")
            }
        }

        val footerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val text = SafeFirebaseParser.parseString(snapshot.value)
                if (text.isNotBlank()) {
                    com.example.ui.components.DoctorShareManager.cachedFooterText = text
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        try {
            ref1?.keepSynced(true)
            ref1?.addValueEventListener(listener)
            ref2?.keepSynced(true)
            ref2?.addValueEventListener(listener)
            footerRef?.keepSynced(true)
            footerRef?.addValueEventListener(footerListener)
        } catch (e: Exception) {
            Log.w(TAG, "Listener attachment exception: ${e.message}")
        }

        awaitClose {
            restJob.cancel()
            try {
                ref1?.removeEventListener(listener)
                ref2?.removeEventListener(listener)
                footerRef?.removeEventListener(footerListener)
            } catch (e: Exception) {}
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

    init {
    }

    /**
     * Live Kotlin callbackFlow streaming real-time updates directly from Firebase Realtime Database
     * using addValueEventListener alongside direct HTTPS REST fallback for 100% reliability on release APKs.
     */
    fun getDoctorsFlow(): Flow<List<Doctor>> = callbackFlow {
        // 1. Emit initial local cache immediately for instantaneous startup
        val initialCacheJob = launch(Dispatchers.IO) {
            try {
                val localDoctors = doctorDao.getAllDoctors().first()
                if (localDoctors.isNotEmpty()) {
                    trySend(localDoctors)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Initial local cache read error: ${e.message}")
            }
        }

        // 2. Direct HTTPS REST fetch and periodic fallback poll
        val restSyncJob = launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val restDoctors = FirebaseRestHelper.fetchDoctors()
                    if (restDoctors != null) {
                        if (restDoctors.isNotEmpty()) {
                            val sorted = restDoctors.sortedBy { it.id }
                            trySend(sorted)
                            doctorDao.replaceAll(sorted)
                        } else {
                            trySend(emptyList())
                            doctorDao.clearAll()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "REST doctors fetch error: ${e.message}")
                }
                delay(8000)
            }
        }

        val ref = doctorsRef
        if (ref == null) {
            val job = launch {
                doctorDao.getAllDoctors().collect { trySend(it) }
            }
            awaitClose {
                initialCacheJob.cancel()
                restSyncJob.cancel()
                job.cancel()
            }
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
                        // Node not found yet or offline: fallback to Room cache without clearing
                        CoroutineScope(Dispatchers.IO).launch {
                            val local = doctorDao.getAllDoctors().first()
                            if (local.isNotEmpty()) {
                                trySend(local)
                            }
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
            initialCacheJob.cancel()
            restSyncJob.cancel()
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
    private val sponsorBannerRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("sponsor_banner")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sponsorBannerRef: ${e.message}")
            null
        }
    }

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
                actionType = obj.optString("action_type", "AUTO"),
                actionValue = obj.optString("action_value", ""),
                expiryDate = obj.optString("expiry_date", "2026-12-31"),
                title = obj.optString("title", "مجمع النور الطبي التخصصي - زمار"),
                description = obj.optString("description", "أوقات الدوام وخدمات العيادات الاستشارية"),
                actionLink = obj.optString("action_link", "")
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
                put("description", banner.description)
                put("action_link", banner.actionLink)
            }
            prefs.edit().putString("sponsor_banner_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sponsor banner: ${e.message}")
        }
    }

    fun getSponsorBannerFlow(): Flow<SponsorBanner> = callbackFlow {
        // 1. Emit cached sponsor banner first
        trySend(getSponsorBanner())

        // 2. Direct HTTPS REST fetch and periodic fallback poll
        val restJob = launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val banner = FirebaseRestHelper.fetchSponsorBanner()
                    if (banner != null) {
                        trySend(banner)
                        saveSponsorBanner(banner)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sponsor banner REST poll error: ${e.message}")
                }
                delay(8000)
            }
        }

        val ref = sponsorBannerRef
        if (ref == null) {
            awaitClose { restJob.cancel() }
            return@callbackFlow
        }

        try {
            ref.keepSynced(true)
        } catch (e: Exception) {
            Log.w(TAG, "keepSynced error on sponsorBannerRef: ${e.message}")
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val banner = SafeFirebaseParser.parseSponsorBanner(snapshot)
                    if (banner != null) {
                        trySend(banner)
                        saveSponsorBanner(banner)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sponsor banner listener: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Sponsor banner listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            restJob.cancel()
            ref.removeEventListener(listener)
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
