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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

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

    // Authoritative live state flow kept in sync with Firebase Realtime Database
    private val _liveLaboratories = MutableStateFlow<List<Laboratory>>(DefaultData.initialLaboratories)
    val laboratoriesFlow: StateFlow<List<Laboratory>> get() = _liveLaboratories

    init {
        setupBackgroundListener()
    }

    private fun setupBackgroundListener() {
        val ref = laboratoriesRef ?: return
        try {
            ref.keepSynced(true)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val parsed = SafeFirebaseParser.parseLaboratories(snapshot)
                        if (parsed.isNotEmpty()) {
                            _liveLaboratories.value = parsed.sortedBy { it.id }
                        } else if (snapshot.exists()) {
                            _liveLaboratories.value = emptyList()
                        } else {
                            seedDefaultLaboratories()
                            _liveLaboratories.value = DefaultData.initialLaboratories
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
    fun getLaboratoriesFlow(): Flow<List<Laboratory>> = callbackFlow {
        trySend(_liveLaboratories.value)

        val ref = laboratoriesRef
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
                        val parsed = SafeFirebaseParser.parseLaboratories(snapshot)
                        if (parsed.isNotEmpty()) {
                            val sorted = parsed.sortedBy { it.id }
                            _liveLaboratories.value = sorted
                            trySend(sorted)
                        } else if (snapshot.exists()) {
                            _liveLaboratories.value = emptyList()
                            trySend(emptyList())
                        } else {
                            seedDefaultLaboratories()
                            _liveLaboratories.value = DefaultData.initialLaboratories
                            trySend(DefaultData.initialLaboratories)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing laboratories in callbackFlow: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Laboratories callbackFlow listener cancelled: ${error.message}")
                }
            }

            ref.addValueEventListener(listener)
        }

        val localJob = launch {
            _liveLaboratories.collect { trySend(it) }
        }

        awaitClose {
            localJob.cancel()
            if (ref != null && listener != null) {
                ref.removeEventListener(listener)
            }
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
     * Add a laboratory with key preservation directly at /laboratories/$id
     */
    fun addLaboratory(laboratory: Laboratory): Long {
        val currentList = _liveLaboratories.value
        val assignedId = if (laboratory.id > 0) {
            laboratory.id
        } else {
            val maxId = currentList.maxOfOrNull { it.id } ?: 0L
            maxId + 1L
        }
        val targetLab = laboratory.copy(id = assignedId)

        val updated = currentList.filterNot { it.id == assignedId } + targetLab
        _liveLaboratories.value = updated.sortedBy { it.id }

        try {
            laboratoriesRef?.child(assignedId.toString())?.setValue(targetLab)
                ?.addOnFailureListener { error ->
                    Log.e(TAG, "Failed to write laboratory $assignedId to Firebase: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing laboratory $assignedId: ${e.message}")
        }
        return assignedId
    }

    /**
     * Update an existing laboratory preserving its ID directly at /laboratories/$id
     */
    fun updateLaboratory(laboratory: Laboratory) {
        val targetId = laboratory.id
        val currentList = _liveLaboratories.value
        val updated = currentList.map { if (it.id == targetId) laboratory else it }
        _liveLaboratories.value = updated

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
     * Delete a laboratory by ID directly removing the child at /laboratories/$id
     */
    fun deleteLaboratory(laboratoryId: Long) {
        val currentList = _liveLaboratories.value
        _liveLaboratories.value = currentList.filterNot { it.id == laboratoryId }

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

    fun saveLaboratories(list: List<Laboratory>) {
        _liveLaboratories.value = list
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
        val initial = DefaultData.initialLaboratories
        _liveLaboratories.value = initial
        try {
            val map = mutableMapOf<String, Laboratory>()
            initial.forEach { lab ->
                map[lab.id.toString()] = lab
            }
            laboratoriesRef?.setValue(map)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting laboratories in Firebase: ${e.message}")
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
