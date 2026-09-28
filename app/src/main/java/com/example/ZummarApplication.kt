package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class ZummarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("ZummarApplication", "FirebaseApp init failed: ${e.message}")
        }

        // Enable persistence safely in Application.onCreate() before any other Firebase calls
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("ZummarApplication", "Default FirebaseDatabase setPersistenceEnabled ignored: ${e.message}")
        }

        try {
            FirebaseDatabase.getInstance("https://zummar-doctors-default-rtdb.firebaseio.com").setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("ZummarApplication", "Custom URL FirebaseDatabase setPersistenceEnabled ignored: ${e.message}")
        }

        // Pre-initialize repositories to attach active Realtime Database listeners immediately on launch
        try {
            com.example.data.repository.PharmacyRepository.getInstance(this)
            com.example.data.repository.LaboratoryRepository.getInstance(this)
        } catch (e: Exception) {
            Log.w("ZummarApplication", "Repository listener pre-init error: ${e.message}")
        }
    }
}
