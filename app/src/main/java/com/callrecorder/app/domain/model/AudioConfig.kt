package com.callrecorder.app.domain.model

import android.media.MediaRecorder

enum class AppAudioSource(val sourceId: Int, val label: String, val description: String) {
    MIC(
        MediaRecorder.AudioSource.MIC,
        "Microphone (Default)",
        "Captures acoustic sound near phone microphone. Speakerphone required for caller audio."
    ),
    VOICE_COMMUNICATION(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        "Voice Communication",
        "Tuned for VoIP / VoIP acoustic echo cancellation. Hardware-dependent."
    ),
    VOICE_RECOGNITION(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        "Voice Recognition",
        "Raw audio stream without OEM aggressive noise suppression."
    ),
    DEFAULT(
        MediaRecorder.AudioSource.DEFAULT,
        "System Default",
        "System default audio input."
    )
}

enum class AudioFormatOption(val extension: String, val mimeType: String, val label: String) {
    AAC_M4A("m4a", "audio/mp4", "M4A (AAC High Quality)"),
    THREE_GPP("3gp", "audio/3gpp", "3GP (Compact Voice)"),
    AMR_WB("amr", "audio/amr-wb", "AMR-WB (Speech Optimized)")
}

data class UserSettings(
    val audioSource: AppAudioSource = AppAudioSource.MIC,
    val audioFormat: AudioFormatOption = AudioFormatOption.AAC_M4A,
    val sampleRate: Int = 44100,
    val audioBitrate: Int = 128000,
    val autoRecordCellular: Boolean = false,
    val speakerphoneAssistEnabled: Boolean = true,
    val videoResolutionWidth: Int = 1080,
    val videoResolutionHeight: Int = 1920,
    val videoBitrate: Int = 4000000
)
