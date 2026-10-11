package com.callrecorder.app.domain

import com.callrecorder.app.domain.model.CallType
import org.junit.Assert.assertEquals
import org.junit.Test

class CallTypeTest {

    @Test
    fun testCallTypeProperties() {
        assertEquals("m4a", CallType.NORMAL_CALL.extension)
        assertEquals("audio/mp4", CallType.NORMAL_CALL.mimeType)

        assertEquals("m4a", CallType.WHATSAPP_VOICE.extension)
        assertEquals("audio/mp4", CallType.WHATSAPP_VOICE.mimeType)

        assertEquals("mp4", CallType.WHATSAPP_VIDEO.extension)
        assertEquals("video/mp4", CallType.WHATSAPP_VIDEO.mimeType)
    }

    @Test
    fun testFromStringFallback() {
        assertEquals(CallType.NORMAL_CALL, CallType.fromString("NORMAL_CALL"))
        assertEquals(CallType.WHATSAPP_VOICE, CallType.fromString("whatsapp_voice"))
        assertEquals(CallType.WHATSAPP_VIDEO, CallType.fromString("WHATSAPP_VIDEO"))
        assertEquals(CallType.NORMAL_CALL, CallType.fromString("INVALID_UNKNOWN"))
    }
}
