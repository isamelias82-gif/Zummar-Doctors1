package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.AppSettings
import com.example.ui.activities.SupportWebActivity

/**
 * Universal Support & Crisp Chat Manager:
 * Centralizes in-app opening of Crisp Chat and administrative support channels via native Android WebView.
 * - Strictly configures DOM storage, database, JavaScript, and in-app WebViewClient routing.
 * - Dynamically updates and fetches target URLs from the Admin Panel / Firebase backend configuration.
 * - Enforces robust null/empty checks with immediate fallback to the verified Crisp chat embed URL:
 *   https://go.crisp.chat/chat/embed/?website_id=50ac8743-e9cf-4f46-a2f1-888d6724bd72
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
        cachedCrispUrl = resolveDynamicChatUrl(dynamicDefault)

        cachedContactUrl = if (settings.contactUs.effectiveInput.isNotBlank()) {
            resolveDynamicChatUrl(settings.contactUs.effectiveInput)
        } else {
            cachedCrispUrl
        }

        cachedReportUrl = if (settings.reportProblem.effectiveInput.isNotBlank()) {
            resolveDynamicChatUrl(settings.reportProblem.effectiveInput)
        } else {
            cachedCrispUrl
        }

        cachedFabUrl = if (settings.fabButton.effectiveInput.isNotBlank()) {
            resolveDynamicChatUrl(settings.fabButton.effectiveInput)
        } else {
            cachedCrispUrl
        }

        Log.d(TAG, "SupportChatManager updated: Crisp=$cachedCrispUrl, Contact=$cachedContactUrl, Report=$cachedReportUrl, FAB=$cachedFabUrl")
    }

    /**
     * Resolves the dynamic admin URL with comprehensive null and empty checks.
     * If the provided admin URL is null, empty, whitespace, or invalid, gracefully falls back
     * to the verified default Crisp Chat URL.
     */
    fun resolveDynamicChatUrl(adminUrl: String?): String {
        val trimmed = adminUrl?.trim().orEmpty()
        val candidate = if (trimmed.isNotBlank()) {
            trimmed
        } else {
            cachedCrispUrl.trim()
        }

        val resolved = if (candidate.isNotBlank()) candidate else DEFAULT_CRISP_URL

        return if (!resolved.startsWith("http://", ignoreCase = true) &&
            !resolved.startsWith("https://", ignoreCase = true)
        ) {
            "https://$resolved"
        } else {
            resolved
        }
    }

    /**
     * Opens Crisp chat in-app using the Native Android WebView (SupportWebActivity).
     * Bypasses external browser intent interceptors, guaranteeing the chat loads directly
     * with full DOM Storage, JavaScript, database, and custom WebViewClient on real devices.
     */
    fun openInAppChat(
        context: Context,
        url: String? = null
    ) {
        val targetUrl = resolveDynamicChatUrl(url)
        SupportWebActivity.start(context, targetUrl)
    }
}
