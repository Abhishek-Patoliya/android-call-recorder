package com.callrecorder.app.domain

import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.RecordingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RecordingStateTest {

    @Test
    fun testIdleState() {
        val state: RecordingState = RecordingState.Idle
        assertTrue(state is RecordingState.Idle)
    }

    @Test
    fun testRecordingStateTransitions() {
        val dummyFile = File("/tmp/test.m4a")
        val recordingState = RecordingState.Recording(
            callType = CallType.NORMAL_CALL,
            outputFile = dummyFile,
            startTimeMs = 1000L,
            durationSeconds = 12L,
            amplitude = 1500,
            isVideo = false
        )

        assertEquals(12L, recordingState.durationSeconds)
        assertEquals(1500, recordingState.amplitude)
        assertEquals(CallType.NORMAL_CALL, recordingState.callType)

        val pausedState = RecordingState.Paused(
            callType = recordingState.callType,
            outputFile = recordingState.outputFile,
            durationSeconds = recordingState.durationSeconds,
            isVideo = false
        )

        assertEquals(12L, pausedState.durationSeconds)
    }

    @Test
    fun testErrorState() {
        val error = RecordingState.Error("Failed to initialize recorder")
        assertEquals("Failed to initialize recorder", error.message)
    }
}
