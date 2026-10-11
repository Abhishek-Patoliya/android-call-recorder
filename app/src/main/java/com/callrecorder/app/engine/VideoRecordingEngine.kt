package com.callrecorder.app.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.domain.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VideoRecordingEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "VideoRecordingEngine"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L
    private var tickerJob: Job? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun startRecording(
        projection: MediaProjection,
        outputFile: File,
        displayMetrics: DisplayMetrics,
        settings: UserSettings = UserSettings()
    ): Boolean {
        if (_recordingState.value is RecordingState.Recording) {
            Log.w(TAG, "Video recording is already active")
            return false
        }

        try {
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            val width = settings.videoResolutionWidth.coerceAtMost(displayMetrics.widthPixels)
            val height = settings.videoResolutionHeight.coerceAtMost(displayMetrics.heightPixels)
            val dpi = displayMetrics.densityDpi

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(settings.videoBitrate)
                setVideoFrameRate(30)
                setVideoSize(width, height)
                setOutputFile(outputFile.absolutePath)
                prepare()
            }

            val vDisplay = projection.createVirtualDisplay(
                "WhatsAppVideoRecordingDisplay",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            recorder.start()

            mediaRecorder = recorder
            virtualDisplay = vDisplay
            mediaProjection = projection
            currentOutputFile = outputFile
            startTimeMs = System.currentTimeMillis()

            _recordingState.value = RecordingState.Recording(
                callType = CallType.WHATSAPP_VIDEO,
                outputFile = outputFile,
                startTimeMs = startTimeMs,
                durationSeconds = 0L,
                amplitude = 0,
                isVideo = true
            )

            startDurationTicker()
            Log.i(TAG, "Video recording started: ${outputFile.name} ($width x $height)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording: ${e.message}", e)
            cleanup()
            _recordingState.value = RecordingState.Error("Failed to start screen video recorder: ${e.localizedMessage ?: "Unknown error"}")
            return false
        }
    }

    fun stopRecording(): Long {
        stopTicker()
        val duration = if (startTimeMs > 0) {
            maxOf(0L, System.currentTimeMillis() - startTimeMs)
        } else {
            0L
        }

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video recorder: ${e.message}")
        }

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtualDisplay: ${e.message}")
        }

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaProjection: ${e.message}")
        }

        mediaRecorder = null
        virtualDisplay = null
        mediaProjection = null
        _recordingState.value = RecordingState.Idle

        Log.i(TAG, "Video recording stopped. Duration: ${duration}ms")
        return duration
    }

    private fun startDurationTicker() {
        stopTicker()
        tickerJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive && mediaRecorder != null) {
                val durationSec = maxOf(0L, (System.currentTimeMillis() - startTimeMs) / 1000)
                currentOutputFile?.let { file ->
                    _recordingState.value = RecordingState.Recording(
                        callType = CallType.WHATSAPP_VIDEO,
                        outputFile = file,
                        startTimeMs = startTimeMs,
                        durationSeconds = durationSec,
                        amplitude = 0,
                        isVideo = true
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun cleanup() {
        stopTicker()
        try { mediaRecorder?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaRecorder = null
        virtualDisplay = null
        mediaProjection = null
    }
}
