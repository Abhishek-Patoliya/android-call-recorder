package com.callrecorder.app.domain

import com.callrecorder.app.domain.model.AppAudioSource
import com.callrecorder.app.domain.model.AudioFormatOption
import com.callrecorder.app.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioConfigTest {

    @Test
    fun testDefaultSettings() {
        val settings = UserSettings()
        assertEquals(AppAudioSource.MIC, settings.audioSource)
        assertEquals(AudioFormatOption.AAC_M4A, settings.audioFormat)
        assertEquals(44100, settings.sampleRate)
        assertEquals(128000, settings.audioBitrate)
    }

    @Test
    fun testAudioSourceEnums() {
        assertNotNull(AppAudioSource.MIC.label)
        assertNotNull(AppAudioSource.VOICE_COMMUNICATION.label)
        assertNotNull(AppAudioSource.VOICE_RECOGNITION.label)
    }

    @Test
    fun testAudioFormatOptions() {
        assertEquals("m4a", AudioFormatOption.AAC_M4A.extension)
        assertEquals("3gp", AudioFormatOption.THREE_GPP.extension)
        assertEquals("amr", AudioFormatOption.AMR_WB.extension)
    }
}
