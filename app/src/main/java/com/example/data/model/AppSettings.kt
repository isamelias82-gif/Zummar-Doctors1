package com.example.data.model

data class ActionButtonConfig(
    val isActive: Boolean = true,
    val actionInput: String = "+9647875023922",
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
        actionInput = "+9647875023922",
        actionType = "AUTO"
    ),
    val reportProblem: ActionButtonConfig = ActionButtonConfig(
        isActive = true,
        actionInput = "+9647875023922",
        actionType = "AUTO"
    ),
    val contactUs: ActionButtonConfig = ActionButtonConfig(
        isActive = true,
        actionInput = "+9647875023922",
        actionType = "AUTO"
    )
)
