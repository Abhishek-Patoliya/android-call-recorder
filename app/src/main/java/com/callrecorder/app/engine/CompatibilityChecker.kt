package com.callrecorder.app.engine

import android.content.Context
import android.os.Build
import com.callrecorder.app.domain.model.CapabilityItem
import com.callrecorder.app.domain.model.CapabilityStatus
import com.callrecorder.app.domain.model.CompatibilityReport

object CompatibilityChecker {

    fun generateReport(
        context: Context,
        overrideSdk: Int? = null,
        overrideManufacturer: String? = null,
        overrideModel: String? = null,
        overrideRelease: String? = null
    ): CompatibilityReport {
        val sdk = overrideSdk ?: Build.VERSION.SDK_INT
        val release = overrideRelease ?: Build.VERSION.RELEASE ?: "14"
        val manufacturer = (overrideManufacturer ?: Build.MANUFACTURER ?: "Android").replaceFirstChar { it.uppercase() }
        val model = overrideModel ?: Build.MODEL ?: "Device"

        val cellularItems = mutableListOf<CapabilityItem>()
        val whatsappVoiceItems = mutableListOf<CapabilityItem>()
        val whatsappVideoItems = mutableListOf<CapabilityItem>()

        // 1. Cellular Call Audio Inspection
        cellularItems.add(
            CapabilityItem(
                title = "Hardware Line 2-Way Recording (VOICE_CALL)",
                status = CapabilityStatus.RESTRICTED_BY_OS,
                technicalReason = "Android OS (API 28+) strictly restricts AudioSource.VOICE_CALL and CAPTURE_AUDIO_OUTPUT to system/privileged apps. Third-party apps cannot record both sides directly off the cellular modem.",
                recommendedWorkaround = "Use Acoustic Microphone capture with the phone on speakerphone to capture incoming caller voice."
            )
        )

        cellularItems.add(
            CapabilityItem(
                title = "Acoustic Microphone Capture (MIC / VOICE_COMMUNICATION)",
                status = CapabilityStatus.SUPPORTED,
                technicalReason = "Standard Android audio capture permitted with RECORD_AUDIO permission and active foreground service.",
                recommendedWorkaround = "Turn on speakerphone during active calls so the microphone can pick up both sides of the conversation."
            )
        )

        cellularItems.add(
            CapabilityItem(
                title = "Call State & Incoming Number Detection",
                status = if (sdk >= Build.VERSION_CODES.S) CapabilityStatus.PARTIALLY_SUPPORTED else CapabilityStatus.SUPPORTED,
                technicalReason = if (sdk >= Build.VERSION_CODES.S) {
                    "Android 12+ requires READ_PHONE_STATE & READ_CALL_LOG runtime permissions for caller ID detection; state transitions are observed via TelephonyCallback."
                } else {
                    "Observed via PhoneStateListener and TelephonyManager broadcasts."
                },
                recommendedWorkaround = "Grant Phone State and Call Log permissions when prompted."
            )
        )

        // 2. WhatsApp Voice Call Inspection
        whatsappVoiceItems.add(
            CapabilityItem(
                title = "Internal Audio Capture (AudioPlaybackCapture)",
                status = CapabilityStatus.RESTRICTED_BY_OS,
                technicalReason = "Android 10+ AudioPlaybackCaptureConfiguration explicitly excludes USAGE_VOICE_COMMUNICATION from third-party app capture. WhatsApp does not opt into FLAG_ALLOW_CAPTURE_BY_ALL.",
                recommendedWorkaround = "Android platform policy prohibits 3rd-party apps from intercepting other apps' encrypted VoIP streams."
            )
        )

        whatsappVoiceItems.add(
            CapabilityItem(
                title = "Simultaneous Mic Recording during WhatsApp Call",
                status = CapabilityStatus.PARTIALLY_SUPPORTED,
                technicalReason = "Android 10+ concurrent capture policies prioritize the active VoIP call app. The recording service might receive silence or low gain if hardware echo canceller is engaged by WhatsApp.",
                recommendedWorkaround = "Set audio source to VOICE_COMMUNICATION or MIC in Settings and enable speakerphone during the VoIP call."
            )
        )

        // 3. WhatsApp Video Call Inspection
        whatsappVideoItems.add(
            CapabilityItem(
                title = "Screen Video Recording (MediaProjection)",
                status = CapabilityStatus.SUPPORTED,
                technicalReason = "Supported via MediaProjection API with explicit user consent dialog and FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION.",
                recommendedWorkaround = "Grant system screen recording dialog permission each time video recording is initiated."
            )
        )

        whatsappVideoItems.add(
            CapabilityItem(
                title = "Video Audio Track (Mic + Speaker Pickup)",
                status = CapabilityStatus.PARTIALLY_SUPPORTED,
                technicalReason = "MediaProjection screen video captures the screen frames. Audio track records via microphone input. Remote party voice requires speakerphone output.",
                recommendedWorkaround = "Keep WhatsApp speakerphone active during video recording for two-way audio pickup."
            )
        )

        val generalSummary = buildString {
            append("Running on $manufacturer $model (Android $release, API $sdk). ")
            append("Stock Android strictly enforces privacy sandboxing: 2-way hardware call capture is blocked on API 28+ by design. ")
            append("This application provides compliant, foreground-service-driven recording with speakerphone assist, transparent status, and zero security bypasses.")
        }

        return CompatibilityReport(
            androidVersion = sdk,
            androidRelease = release,
            deviceManufacturer = manufacturer,
            deviceModel = model,
            cellularCapabilities = cellularItems,
            whatsappVoiceCapabilities = whatsappVoiceItems,
            whatsappVideoCapabilities = whatsappVideoItems,
            generalSummary = generalSummary
        )
    }
}
