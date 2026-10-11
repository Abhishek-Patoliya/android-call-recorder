package com.callrecorder.app.domain.model

data class CapabilityItem(
    val title: String,
    val status: CapabilityStatus,
    val technicalReason: String,
    val recommendedWorkaround: String? = null
)

enum class CapabilityStatus {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    RESTRICTED_BY_OS,
    UNSUPPORTED
}

data class CompatibilityReport(
    val androidVersion: Int,
    val androidRelease: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val cellularCapabilities: List<CapabilityItem>,
    val whatsappVoiceCapabilities: List<CapabilityItem>,
    val whatsappVideoCapabilities: List<CapabilityItem>,
    val generalSummary: String
)
