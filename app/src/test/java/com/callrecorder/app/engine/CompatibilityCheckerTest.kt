package com.callrecorder.app.engine

import android.content.Context
import com.callrecorder.app.domain.model.CapabilityStatus
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityCheckerTest {

    @Test
    fun testGenerateReportFlagsRestrictions() {
        val mockContext = mockk<Context>(relaxed = true)
        val report = CompatibilityChecker.generateReport(
            context = mockContext,
            overrideSdk = 34,
            overrideManufacturer = "Google",
            overrideModel = "Pixel 8 Pro",
            overrideRelease = "14"
        )

        assertNotNull(report)
        assertEquals(34, report.androidVersion)
        assertEquals("Google", report.deviceManufacturer)
        assertEquals("Pixel 8 Pro", report.deviceModel)
        assertNotNull(report.generalSummary)

        // 1. Cellular line 2-way recording must be flagged as RESTRICTED_BY_OS
        val hardwareLine = report.cellularCapabilities.firstOrNull {
            it.title.contains("Hardware Line", ignoreCase = true)
        }
        assertNotNull(hardwareLine)
        assertEquals(CapabilityStatus.RESTRICTED_BY_OS, hardwareLine?.status)
        assertTrue(hardwareLine?.technicalReason?.contains("API 28") == true)

        // 2. Microphone acoustic capture should be supported
        val micCapture = report.cellularCapabilities.firstOrNull {
            it.title.contains("Microphone", ignoreCase = true)
        }
        assertNotNull(micCapture)
        assertEquals(CapabilityStatus.SUPPORTED, micCapture?.status)

        // 3. AudioPlaybackCapture for WhatsApp VoIP must be flagged as RESTRICTED_BY_OS
        val playbackCapture = report.whatsappVoiceCapabilities.firstOrNull {
            it.title.contains("AudioPlaybackCapture", ignoreCase = true)
        }
        assertNotNull(playbackCapture)
        assertEquals(CapabilityStatus.RESTRICTED_BY_OS, playbackCapture?.status)

        // 4. MediaProjection screen recording should be supported
        val screenVideo = report.whatsappVideoCapabilities.firstOrNull {
            it.title.contains("MediaProjection", ignoreCase = true)
        }
        assertNotNull(screenVideo)
        assertEquals(CapabilityStatus.SUPPORTED, screenVideo?.status)
    }

    @Test
    fun testDefaultReportFallbackDoesNotCrash() {
        val mockContext = mockk<Context>(relaxed = true)
        val report = CompatibilityChecker.generateReport(mockContext)
        assertNotNull(report)
        assertNotNull(report.deviceManufacturer)
        assertNotNull(report.deviceModel)
    }
}
