package com.callrecorder.app.domain.model

enum class CallType(val displayName: String, val extension: String, val mimeType: String) {
    NORMAL_CALL("Normal Call", "m4a", "audio/mp4"),
    WHATSAPP_VOICE("WhatsApp Voice", "m4a", "audio/mp4"),
    WHATSAPP_VIDEO("WhatsApp Video", "mp4", "video/mp4"),
    MANUAL_AUDIO("Manual Audio", "m4a", "audio/mp4");

    companion object {
        fun fromString(value: String): CallType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL_CALL
        }
    }
}
