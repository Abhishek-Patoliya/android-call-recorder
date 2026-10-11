package com.callrecorder.app.data

import com.callrecorder.app.data.local.entity.RecordingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecordingEntityTest {

    @Test
    fun testEntityCreation() {
        val entity = RecordingEntity(
            id = 1L,
            title = "Test Normal Call",
            filePath = "/data/user/0/com.callrecorder.app/files/recordings/call_1.m4a",
            fileName = "call_1.m4a",
            callType = "NORMAL_CALL",
            phoneNumber = "+1234567890",
            contactName = "Alice",
            durationMs = 45000L,
            fileSizeBytes = 512000L,
            timestamp = 1700000000000L,
            format = "m4a",
            mimeType = "audio/mp4"
        )

        assertEquals(1L, entity.id)
        assertEquals("Test Normal Call", entity.title)
        assertEquals("NORMAL_CALL", entity.callType)
        assertEquals("+1234567890", entity.phoneNumber)
        assertEquals("Alice", entity.contactName)
        assertEquals(45000L, entity.durationMs)
        assertFalse(entity.isFavorite)
    }
}
