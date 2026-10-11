package com.callrecorder.app.domain.model

import java.io.File

sealed interface RecordingState {
    data object Idle : RecordingState
    
    data class Recording(
        val callType: CallType,
        val outputFile: File,
        val startTimeMs: Long,
        val durationSeconds: Long = 0,
        val amplitude: Int = 0,
        val isVideo: Boolean = false
    ) : RecordingState

    data class Paused(
        val callType: CallType,
        val outputFile: File,
        val durationSeconds: Long,
        val isVideo: Boolean = false
    ) : RecordingState

    data class Error(val message: String) : RecordingState
}
