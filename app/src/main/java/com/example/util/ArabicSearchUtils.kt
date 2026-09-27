package com.example.util

import com.example.data.model.Doctor

object ArabicSearchUtils {

    /**
     * Normalizes Arabic text by:
     * 1. Converting to lowercase.
     * 2. Removing Arabic diacritics (Harakat / Tashkeel: Fatha, Damma, Kasra, Sukun, Shadda, Tanween, etc.).
     * 3. Normalizing Alef variants (أ, إ, آ, ٱ) -> ا
     * 4. Normalizing Taa Marbuta (ة) -> ه
     * 5. Normalizing Alef Maksura (ى) -> ي
     * 6. Normalizing Persian / alternative chars (ك -> ك, etc.)
     * 7. Removing Kashida / Tatweel (ـ)
     */
    fun normalizeArabic(text: String): String {
        if (text.isEmpty()) return ""
        return text.lowercase()
            .replace(Regex("[\u064B-\u065F\u0670]"), "") // Tashkeel
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace("ـ", "") // Tatweel
            .trim()
    }

    /**
     * Strips title prefixes like "د." or "دكتور" or "طبيب" for flexible query matching.
     */
    fun stripDoctorTitles(normalizedText: String): String {
        return normalizedText
            .replace(Regex("^د\\.\\s*"), "")
            .replace(Regex("^د\\s+"), "")
            .replace(Regex("^دكتور\\s+"), "")
            .replace(Regex("^دكتورة\\s+"), "")
            .replace(Regex("^طبيب\\s+"), "")
            .replace(Regex("^طبيبة\\s+"), "")
            .trim()
    }

    /**
     * Checks if a doctor matches the query in real-time.
     * Evaluates across:
     * - Name (with and without "د.")
     * - Specialty / Section (e.g. أطفال, جلدية, باطنية, أسنان, عيون, عظام, قلب, نسائية, سونار)
     * - Address / Landmark
     * - Notes / Working info
     * - Days of week
     * Supports matching partial queries of 1, 2, or more characters.
     */
    fun matchesDoctor(doctor: Doctor, rawQuery: String): Boolean {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return true

        val normalizedQuery = normalizeArabic(trimmed)
        if (normalizedQuery.isEmpty()) return true

        val queryWithoutTitle = stripDoctorTitles(normalizedQuery)

        val normalizedDocName = normalizeArabic(doctor.name)
        val nameWithoutTitle = stripDoctorTitles(normalizedDocName)

        val normalizedSpecialty = normalizeArabic(doctor.specialty)
        val normalizedAddress = normalizeArabic(doctor.addressLandmark)
        val normalizedNotes = normalizeArabic(doctor.notes)
        val normalizedTitle = normalizeArabic(doctor.title)

        // Single or multi-character direct substring matching
        if (normalizedDocName.contains(normalizedQuery) ||
            nameWithoutTitle.contains(normalizedQuery) ||
            (queryWithoutTitle.isNotEmpty() && nameWithoutTitle.contains(queryWithoutTitle)) ||
            normalizedSpecialty.contains(normalizedQuery) ||
            normalizedAddress.contains(normalizedQuery) ||
            normalizedNotes.contains(normalizedQuery) ||
            normalizedTitle.contains(normalizedQuery)
        ) {
            return true
        }

        // Check if query matches any of the working days
        for (day in doctor.days) {
            if (normalizeArabic(day).contains(normalizedQuery)) {
                return true
            }
        }

        // Multi-token matching (e.g. "احمد باطنية")
        val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.size > 1) {
            val allTokensMatch = tokens.all { token ->
                val tokenNoTitle = stripDoctorTitles(token)
                normalizedDocName.contains(token) ||
                nameWithoutTitle.contains(token) ||
                (tokenNoTitle.isNotEmpty() && nameWithoutTitle.contains(tokenNoTitle)) ||
                normalizedSpecialty.contains(token) ||
                normalizedAddress.contains(token) ||
                normalizedNotes.contains(token) ||
                doctor.days.any { normalizeArabic(it).contains(token) }
            }
            if (allTokensMatch) return true
        }

        return false
    }
}
