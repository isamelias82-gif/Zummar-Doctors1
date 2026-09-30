package com.example.data.model

import java.util.Calendar

data class SponsorBanner(
    val bannerId: String = "sponsor_01",
    val isActive: Boolean = true,
    val imagePath: String = "",          // web url, base64 data URI, local path, or content uri
    val actionType: String = "AUTO",     // "PHONE", "URL", "WHATSAPP", "AUTO"
    val actionValue: String = "",        // Phone number or URL
    val expiryDate: String = "",         // format: "YYYY-MM-DD" e.g. "2026-12-31"
    val title: String = "مجمع النور الطبي التخصصي - زمار",
    val description: String = "",
    val actionLink: String = ""          // URL or phone link
) {
    /**
     * Determines whether the banner is currently active and within expiry date.
     */
    fun isCurrentlyActive(): Boolean {
        if (!isActive) return false
        if (expiryDate.isBlank()) return true

        return try {
            val parts = expiryDate.split("-").map { it.trim().toInt() }
            if (parts.size == 3) {
                val now = Calendar.getInstance()
                val expCal = Calendar.getInstance().apply {
                    set(parts[0], parts[1] - 1, parts[2], 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                now.timeInMillis <= expCal.timeInMillis
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Effective action input (either actionLink, actionValue, or blank)
     */
    val effectiveActionInput: String
        get() = actionLink.ifBlank { actionValue }.trim()

    companion object {
        const val ACTION_PHONE = "PHONE"
        const val ACTION_WHATSAPP = "WHATSAPP"
        const val ACTION_URL = "URL"
        const val ACTION_AUTO = "AUTO"

        val defaultBanner = SponsorBanner(
            bannerId = "sponsor_01",
            isActive = true,
            imagePath = "", // will fallback to default drawable
            actionType = ACTION_AUTO,
            actionValue = "",
            expiryDate = "2026-12-31",
            title = "مجمع النور الطبي التخصصي - زمار",
            description = "أوقات الدوام وخدمات العيادات الاستشارية",
            actionLink = ""
        )
    }
}
