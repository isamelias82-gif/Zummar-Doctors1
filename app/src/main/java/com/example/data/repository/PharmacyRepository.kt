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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _pharmaciesFlow = MutableStateFlow<List<Pharmacy>>(DefaultData.initialPharmacies)
    val pharmaciesFlow: StateFlow<List<Pharmacy>> = _pharmaciesFlow

    init {
        setupListener()
    }

    private fun setupListener() {
        try {
            val ref = pharmaciesRef ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val parsed = SafeFirebaseParser.parsePharmacies(snapshot)
                            if (parsed.isNotEmpty()) {
                                _pharmaciesFlow.value = parsed.sortedBy { it.id }
                            } else {
                                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                                    seedDefaultPharmacies()
                                }
                                _pharmaciesFlow.value = DefaultData.initialPharmacies
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in pharmacies listener: ${e.message}")
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
            Log.e(TAG, "Failed to attach listener: ${e.message}")
            _pharmaciesFlow.value = DefaultData.initialPharmacies
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
     * Add a pharmacy with key preservation at /pharmacies/$id
     */
    fun addPharmacy(pharmacy: Pharmacy): Long {
        val currentList = _pharmaciesFlow.value
        val assignedId = if (pharmacy.id > 0) {
            pharmacy.id
        } else {
            val maxId = currentList.maxOfOrNull { it.id } ?: 0L
            maxId + 1L
        }
        val targetPharmacy = pharmacy.copy(id = assignedId)

        // Optimistically update local state flow
        val updated = currentList.filterNot { it.id == assignedId } + targetPharmacy
        _pharmaciesFlow.value = updated.sortedBy { it.id }

        // Write directly to /pharmacies/$id in Firebase Realtime Database
        try {
            pharmaciesRef?.child(assignedId.toString())?.setValue(targetPharmacy)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to add pharmacy $assignedId to Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing pharmacy $assignedId: ${e.message}")
        }
        return assignedId
    }

    /**
     * Update an existing pharmacy preserving its ID at /pharmacies/$id
     */
    fun updatePharmacy(pharmacy: Pharmacy) {
        val targetId = pharmacy.id
        val currentList = _pharmaciesFlow.value

        // Optimistically update local state flow
        val updated = currentList.map { if (it.id == targetId) pharmacy else it }
        _pharmaciesFlow.value = updated

        // Direct update to /pharmacies/$id in Firebase Realtime Database
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
     * Delete a pharmacy by ID from /pharmacies/$id
     */
    fun deletePharmacy(pharmacyId: Long) {
        val currentList = _pharmaciesFlow.value

        // Optimistically update local state flow
        _pharmaciesFlow.value = currentList.filterNot { it.id == pharmacyId }

        // Delete node at /pharmacies/$id in Firebase Realtime Database
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

    /**
     * Full replace of pharmacies if needed
     */
    fun savePharmacies(list: List<Pharmacy>) {
        _pharmaciesFlow.value = list
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
        seedDefaultPharmacies()
        _pharmaciesFlow.value = DefaultData.initialPharmacies
    }
}
