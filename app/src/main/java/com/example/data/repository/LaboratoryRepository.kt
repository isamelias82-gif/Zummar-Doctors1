package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DefaultData
import com.example.data.model.Laboratory
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
import kotlinx.coroutines.withContext

class LaboratoryRepository(private val context: Context) {

    companion object {
        private const val TAG = "LaboratoryRepository"
        private const val RTDB_URL = "https://zummar-doctors-default-rtdb.firebaseio.com"

        @Volatile
        private var INSTANCE: LaboratoryRepository? = null

        fun getInstance(context: Context): LaboratoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LaboratoryRepository(context.applicationContext).also { INSTANCE = it }
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

    private val laboratoriesRef: DatabaseReference? by lazy {
        try {
            firebaseDatabase?.getReference("laboratories")
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining laboratoriesRef: ${e.message}")
            null
        }
    }

    private val _laboratoriesFlow = MutableStateFlow<List<Laboratory>>(DefaultData.initialLaboratories)
    val laboratoriesFlow: StateFlow<List<Laboratory>> = _laboratoriesFlow

    init {
        setupListener()
    }

    private fun setupListener() {
        try {
            val ref = laboratoriesRef ?: return
            try {
                ref.keepSynced(true)
            } catch (e: Exception) {
                Log.w(TAG, "keepSynced error: ${e.message}")
            }

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val parsed = SafeFirebaseParser.parseLaboratories(snapshot)
                            if (parsed.isNotEmpty()) {
                                _laboratoriesFlow.value = parsed.sortedBy { it.id }
                            } else if (snapshot.exists()) {
                                // Node exists but has 0 items (e.g., all were deleted)
                                _laboratoriesFlow.value = emptyList()
                            } else {
                                // Node does not exist at all in Firebase Realtime Database
                                seedDefaultLaboratories()
                                _laboratoriesFlow.value = DefaultData.initialLaboratories
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in laboratories listener: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Laboratories listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach listener: ${e.message}")
        }
    }

    /**
     * Forces a direct fetch from the Firebase Realtime Database server,
     * bypassing local offline cache to immediately reflect remote edits/deletions.
     */
    suspend fun fetchFromServer(): Boolean = withContext(Dispatchers.IO) {
        val ref = laboratoriesRef ?: return@withContext false
        try {
            val snapshot = SafeFirebaseParser.fetchDirectFromServer(ref)
            if (snapshot != null) {
                val parsed = SafeFirebaseParser.parseLaboratories(snapshot)
                if (parsed.isNotEmpty()) {
                    _laboratoriesFlow.value = parsed.sortedBy { it.id }
                } else if (snapshot.exists()) {
                    _laboratoriesFlow.value = emptyList()
                } else {
                    seedDefaultLaboratories()
                    _laboratoriesFlow.value = DefaultData.initialLaboratories
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchFromServer error: ${e.message}")
            false
        }
    }

    private fun seedDefaultLaboratories() {
        try {
            val ref = laboratoriesRef ?: return
            val map = mutableMapOf<String, Laboratory>()
            DefaultData.initialLaboratories.forEach { lab ->
                map[lab.id.toString()] = lab
            }
            ref.setValue(map)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot seed initial laboratories: ${e.message}")
        }
    }

    /**
     * Add a laboratory with key preservation at /laboratories/$id
     */
    fun addLaboratory(laboratory: Laboratory): Long {
        val currentList = _laboratoriesFlow.value
        val assignedId = if (laboratory.id > 0) {
            laboratory.id
        } else {
            val maxId = currentList.maxOfOrNull { it.id } ?: 0L
            maxId + 1L
        }
        val targetLab = laboratory.copy(id = assignedId)

        // Optimistically update local state flow
        val updated = currentList.filterNot { it.id == assignedId } + targetLab
        _laboratoriesFlow.value = updated.sortedBy { it.id }

        // Write directly to /laboratories/$id in Firebase Realtime Database
        try {
            laboratoriesRef?.child(assignedId.toString())?.setValue(targetLab)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to add laboratory $assignedId to Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing laboratory $assignedId: ${e.message}")
        }
        return assignedId
    }

    /**
     * Update an existing laboratory preserving its ID at /laboratories/$id
     */
    fun updateLaboratory(laboratory: Laboratory) {
        val targetId = laboratory.id
        val currentList = _laboratoriesFlow.value

        // Optimistically update local state flow
        val updated = currentList.map { if (it.id == targetId) laboratory else it }
        _laboratoriesFlow.value = updated

        // Direct update to /laboratories/$id in Firebase Realtime Database
        try {
            laboratoriesRef?.child(targetId.toString())?.setValue(laboratory)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to update laboratory $targetId in Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating laboratory $targetId: ${e.message}")
        }
    }

    /**
     * Delete a laboratory by ID from /laboratories/$id
     */
    fun deleteLaboratory(laboratoryId: Long) {
        val currentList = _laboratoriesFlow.value

        // Optimistically update local state flow
        _laboratoriesFlow.value = currentList.filterNot { it.id == laboratoryId }

        // Delete node at /laboratories/$id in Firebase Realtime Database
        try {
            laboratoriesRef?.child(laboratoryId.toString())?.removeValue()
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to delete laboratory $laboratoryId in Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing laboratory $laboratoryId: ${e.message}")
        }
    }

    fun deleteLaboratory(laboratory: Laboratory) {
        deleteLaboratory(laboratory.id)
    }

    /**
     * Full replace of laboratories if needed
     */
    fun saveLaboratories(list: List<Laboratory>) {
        _laboratoriesFlow.value = list
        try {
            val map = mutableMapOf<String, Laboratory>()
            list.forEach { lab ->
                map[lab.id.toString()] = lab
            }
            laboratoriesRef?.setValue(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch saving laboratories: ${e.message}")
        }
    }

    fun resetToDefaults() {
        seedDefaultLaboratories()
        _laboratoriesFlow.value = DefaultData.initialLaboratories
    }
}
