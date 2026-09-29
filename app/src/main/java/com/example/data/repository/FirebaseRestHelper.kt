package com.example.data.repository

import android.util.Log
import com.example.data.model.AppSettings
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.model.SponsorBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct HTTPS REST fallback engine for Firebase Realtime Database.
 * Operates concurrently with WebSocket listeners to guarantee immediate synchronization
 * on all Android release builds, physical devices, carrier networks, and restrictive firewalls.
 */
object FirebaseRestHelper {
    private const val TAG = "FirebaseRestHelper"
    private const val BASE_URL = "https://zummar-doctors-default-rtdb.firebaseio.com"

    suspend fun getJson(path: String): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$path.json")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                Log.w(TAG, "HTTP error on $path: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception fetching $path: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchDoctors(): List<Doctor>? {
        val json = getJson("doctors") ?: return null
        return SafeFirebaseParser.parseDoctorsFromJson(json)
    }

    suspend fun fetchPharmacies(): List<Pharmacy>? {
        val json = getJson("pharmacies") ?: return null
        return SafeFirebaseParser.parsePharmaciesFromJson(json)
    }

    suspend fun fetchLaboratories(): List<Laboratory>? {
        val json = getJson("laboratories") ?: return null
        return SafeFirebaseParser.parseLaboratoriesFromJson(json)
    }

    suspend fun fetchSponsorBanner(): SponsorBanner? {
        val json = getJson("sponsor_banner") ?: return null
        return SafeFirebaseParser.parseSponsorBannerFromJson(json)
    }

    suspend fun fetchAppSettings(): AppSettings? {
        val json = getJson("app_settings") ?: return null
        return try {
            val obj = JSONObject(json)
            val pharm = obj.optBoolean("pharmacies_enabled", true)
            val lab = obj.optBoolean("laboratories_enabled", true)
            AppSettings(pharmaciesEnabled = pharm, laboratoriesEnabled = lab)
        } catch (e: Exception) {
            null
        }
    }
}
