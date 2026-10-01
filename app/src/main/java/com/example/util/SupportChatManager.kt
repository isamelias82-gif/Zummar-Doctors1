package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.example.data.model.AppSettings
import com.example.ui.activities.SupportWebActivity

/**
 * Universal Support & Crisp Chat Manager:
 * Centralizes in-app opening of Crisp Chat and administrative support channels.
 * - Prioritizes Chrome Custom Tabs (CustomTabsIntent) for seamless in-app overlay display.
 * - Falls back to in-app SupportWebActivity (with full JS, DOM & Database storage, and software rendering).
 * - Dynamically updates and fetches target URLs from the Admin Panel / Firebase backend configuration.
 * - Ensures proper back-button and close behavior returning cleanly to the current screen.
 */
object SupportChatManager {
    private const val TAG = "SupportChatManager"

    const val DEFAULT_CRISP_URL = "https://go.crisp.chat/chat/embed/?website_id=50ac8743-e9cf-4f46-a2f1-888d6724bd72"

    @Volatile
    var cachedCrispUrl: String = DEFAULT_CRISP_URL
        private set

    @Volatile
    var cachedReportUrl: String = DEFAULT_CRISP_URL
        private set

    @Volatile
    var cachedContactUrl: String = DEFAULT_CRISP_URL
        private set

    @Volatile
    var cachedFabUrl: String = DEFAULT_CRISP_URL
        private set

    /**
     * Updates cached support URLs dynamically whenever AppSettings is loaded or modified by the Admin.
     */
    fun updateFromSettings(settings: AppSettings) {
        val dynamicDefault = settings.getDynamicSupportUrl()
        cachedCrispUrl = dynamicDefault

        cachedContactUrl = if (settings.contactUs.effectiveInput.isNotBlank()) {
            settings.contactUs.effectiveInput
        } else {
            dynamicDefault
        }

        cachedReportUrl = if (settings.reportProblem.effectiveInput.isNotBlank()) {
            settings.reportProblem.effectiveInput
        } else {
            dynamicDefault
        }

        cachedFabUrl = if (settings.fabButton.effectiveInput.isNotBlank()) {
            settings.fabButton.effectiveInput
        } else {
            dynamicDefault
        }

        Log.d(TAG, "SupportChatManager updated: Crisp=$cachedCrispUrl, Contact=$cachedContactUrl, Report=$cachedReportUrl, FAB=$cachedFabUrl")
    }

    /**
     * Opens Crisp chat or support link in-app.
     * Uses Chrome Custom Tabs (CustomTabsIntent) with app-themed toolbar for an in-app overlay experience.
     * Falls back to SupportWebActivity (in-app WebView dialog) if Custom Tabs is not supported.
     */
    fun openInAppChat(
        context: Context,
        url: String? = null
    ) {
        val target = if (!url.isNullOrBlank()) {
            url.trim()
        } else {
            cachedCrispUrl
        }.ifBlank {
            DEFAULT_CRISP_URL
        }

        val fullUrl = if (!target.startsWith("http://", ignoreCase = true) &&
            !target.startsWith("https://", ignoreCase = true)
        ) {
            "https://$target"
        } else {
            target
        }

        try {
            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(Color.parseColor("#00796B")) // Matching TealPrimaryDark
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(colorSchemeParams)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(fullUrl))
        } catch (e: Exception) {
            Log.w(TAG, "CustomTabs launch failed, falling back to SupportWebActivity: ${e.message}")
            try {
                SupportWebActivity.start(context, fullUrl)
            } catch (e2: Exception) {
                Log.e(TAG, "SupportWebActivity fallback failed: ${e2.message}")
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }
}
