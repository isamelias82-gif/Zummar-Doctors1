package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Doctor
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctors ORDER BY orderIndex ASC, id ASC")
    fun getAllDoctors(): Flow<List<Doctor>>

    @Query("SELECT * FROM doctors WHERE id = :id")
    suspend fun getDoctorById(id: Long): Doctor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDoctors(doctors: List<Doctor>)

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Delete
    suspend fun deleteDoctor(doctor: Doctor)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctorById(id: Long)

    @Query("DELETE FROM doctors")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM doctors")
    suspend fun getCount(): Int
}
