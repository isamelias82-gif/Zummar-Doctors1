package com.example.data.model

data class ActionButtonConfig(
    val isActive: Boolean = true,
    val actionInput: String = "",
    val actionType: String = "AUTO" // "AUTO", "PHONE", "URL", "WHATSAPP"
) {
    val effectiveInput: String
        get() = actionInput.trim()
}

data class AppSettings(
    val pharmaciesEnabled: Boolean = true,
    val laboratoriesEnabled: Boolean = true,
    val fabButton: ActionButtonConfig = ActionButtonConfig(
        isActive = true,
        actionInput = "",
        actionType = "AUTO"
    ),
    val reportProblem: ActionButtonConfig = ActionButtonConfig(
        isActive = true,
        actionInput = "",
        actionType = "AUTO"
    ),
    val contactUs: ActionButtonConfig = ActionButtonConfig(
        isActive = true,
        actionInput = "",
        actionType = "AUTO"
    ),
    val crispUrl: String = "",
    val doctorShareText: String = "",
    val doctorShareFooterText: String = ""
) {
    /**
     * Resolves the dynamic Crisp / Support chat URL configured by the Admin.
     * Evaluates crispUrl, contactUs, reportProblem, and fabButton inputs in order,
     * falling back to the default Crisp URL.
     */
    fun getDynamicSupportUrl(): String {
        return crispUrl.trim().ifBlank {
            contactUs.effectiveInput.ifBlank {
                reportProblem.effectiveInput.ifBlank {
                    fabButton.effectiveInput.ifBlank {
                        "https://go.crisp.chat/chat/embed/?website_id=50ac8743-e9cf-4f46-a2f1-888d6724bd72"
                    }
                }
            }
        }
    }
}
