package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DefaultData
import com.example.data.model.Pharmacy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class PharmacyRepository(private val context: Context) {

    companion object {
        private const val TAG = "PharmacyRepository"
        private const val RTDB_URL = "https://zummar-doctors-default-rtdb.firebaseio.com"

        @Volatile
        private var INSTANCE: PharmacyRepository? = null

        fun getInstance(context: Context): PharmacyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PharmacyRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val firebaseDatabase: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Custom RTDB fallback: ${e.message}")
            try {
                FirebaseDatabase.getInstance()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed default RTDB: ${e2.message}")
                null
            }
        }
    }

    private val pharmaciesRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("pharmacies")
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining pharmaciesRef: ${e.message}")
            null
        }
    }

    // Authoritative live state flow kept in sync with Firebase Realtime Database
    private val _livePharmacies = MutableStateFlow<List<Pharmacy>>(DefaultData.initialPharmacies)
    val pharmaciesFlow: StateFlow<List<Pharmacy>> get() = _livePharmacies

    init {
        setupBackgroundListener()
    }

    private fun setupBackgroundListener() {
        val ref = pharmaciesRef ?: return
        try {
            ref.keepSynced(true)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val parsed = SafeFirebaseParser.parsePharmacies(snapshot)
                        if (parsed.isNotEmpty()) {
                            _livePharmacies.value = parsed.sortedBy { it.id }
                        } else if (snapshot.exists()) {
                            _livePharmacies.value = emptyList()
                        } else {
                            seedDefaultPharmacies()
                            _livePharmacies.value = DefaultData.initialPharmacies
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in background listener: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Background listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "setupBackgroundListener error: ${e.message}")
        }
    }

    /**
     * Live Kotlin callbackFlow streaming real-time updates directly from Firebase Realtime Database
     * using addValueEventListener. Replaces all single-fetch get() calls.
     */
    fun getPharmaciesFlow(): Flow<List<Pharmacy>> = callbackFlow {
        trySend(_livePharmacies.value)

        val ref = pharmaciesRef
        var listener: ValueEventListener? = null
        if (ref != null) {
            try {
                ref.keepSynced(true)
            } catch (e: Exception) {
                Log.w(TAG, "keepSynced error: ${e.message}")
            }

            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val parsed = SafeFirebaseParser.parsePharmacies(snapshot)
                        if (parsed.isNotEmpty()) {
                            val sorted = parsed.sortedBy { it.id }
                            _livePharmacies.value = sorted
                            trySend(sorted)
                        } else if (snapshot.exists()) {
                            _livePharmacies.value = emptyList()
                            trySend(emptyList())
                        } else {
                            seedDefaultPharmacies()
                            _livePharmacies.value = DefaultData.initialPharmacies
                            trySend(DefaultData.initialPharmacies)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing pharmacies in callbackFlow: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Pharmacies callbackFlow listener cancelled: ${error.message}")
                }
            }

            ref.addValueEventListener(listener)
        }

        val localJob = launch {
            _livePharmacies.collect { trySend(it) }
        }

        awaitClose {
            localJob.cancel()
            if (ref != null && listener != null) {
                ref.removeEventListener(listener)
            }
        }
    }

    private fun seedDefaultPharmacies() {
        try {
            val ref = pharmaciesRef ?: return
            val map = mutableMapOf<String, Pharmacy>()
            DefaultData.initialPharmacies.forEach { p ->
                map[p.id.toString()] = p
            }
            ref.setValue(map)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot seed initial pharmacies: ${e.message}")
        }
    }

    /**
     * Add a pharmacy with key preservation directly at /pharmacies/$id
     */
    fun addPharmacy(pharmacy: Pharmacy): Long {
        val currentList = _livePharmacies.value
        val assignedId = if (pharmacy.id > 0) {
            pharmacy.id
        } else {
            val maxId = currentList.maxOfOrNull { it.id } ?: 0L
            maxId + 1L
        }
        val targetPharmacy = pharmacy.copy(id = assignedId)

        val updated = currentList.filterNot { it.id == assignedId } + targetPharmacy
        _livePharmacies.value = updated.sortedBy { it.id }

        try {
            pharmaciesRef?.child(assignedId.toString())?.setValue(targetPharmacy)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to write pharmacy $assignedId to Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing pharmacy $assignedId: ${e.message}")
        }
        return assignedId
    }

    /**
     * Update an existing pharmacy preserving its ID directly at /pharmacies/$id
     */
    fun updatePharmacy(pharmacy: Pharmacy) {
        val targetId = pharmacy.id
        val currentList = _livePharmacies.value
        val updated = currentList.map { if (it.id == targetId) pharmacy else it }
        _livePharmacies.value = updated

        try {
            pharmaciesRef?.child(targetId.toString())?.setValue(pharmacy)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to update pharmacy $targetId in Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pharmacy $targetId: ${e.message}")
        }
    }

    /**
     * Delete a pharmacy by ID directly removing the child at /pharmacies/$id
     */
    fun deletePharmacy(pharmacyId: Long) {
        val currentList = _livePharmacies.value
        _livePharmacies.value = currentList.filterNot { it.id == pharmacyId }

        try {
            pharmaciesRef?.child(pharmacyId.toString())?.removeValue()
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to delete pharmacy $pharmacyId in Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing pharmacy $pharmacyId: ${e.message}")
        }
    }

    fun deletePharmacy(pharmacy: Pharmacy) {
        deletePharmacy(pharmacy.id)
    }

    fun savePharmacies(list: List<Pharmacy>) {
        _livePharmacies.value = list
        try {
            val map = mutableMapOf<String, Pharmacy>()
            list.forEach { p ->
                map[p.id.toString()] = p
            }
            pharmaciesRef?.setValue(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch saving pharmacies: ${e.message}")
        }
    }

    fun resetToDefaults() {
        val initial = DefaultData.initialPharmacies
        _livePharmacies.value = initial
        try {
            val map = mutableMapOf<String, Pharmacy>()
            initial.forEach { p ->
                map[p.id.toString()] = p
            }
            pharmaciesRef?.setValue(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting pharmacies in Firebase: ${e.message}")
        }
    }

    fun reconnectRealtime() {
        try {
            firebaseDatabase?.goOnline()
        } catch (e: Exception) {
            Log.w(TAG, "goOnline error: ${e.message}")
        }
    }
}
