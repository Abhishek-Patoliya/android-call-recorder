package com.callrecorder.app.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.callrecorder.app.domain.model.AppAudioSource
import com.callrecorder.app.domain.model.AudioFormatOption
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

class AudioRecordingEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "AudioRecordingEngine"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var currentCallType: CallType = CallType.NORMAL_CALL
    private var startTimeMs: Long = 0L
    private var pausedDurationMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L
    private var isPaused = false

    private var amplitudeTickerJob: Job? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    @Synchronized
    fun startRecording(
        outputFile: File,
        callType: CallType,
        settings: UserSettings = UserSettings()
    ): Boolean {
        if (_recordingState.value is RecordingState.Recording) {
            Log.w(TAG, "Recording is already active")
            return false
        }

        try {
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(settings.audioSource.sourceId)
                when (settings.audioFormat) {
                    AudioFormatOption.AAC_M4A -> {
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(settings.audioBitrate)
                        setAudioSamplingRate(settings.sampleRate)
                    }
                    AudioFormatOption.THREE_GPP -> {
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    }
                    AudioFormatOption.AMR_WB -> {
                        setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                    }
                }
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = outputFile
            currentCallType = callType
            startTimeMs = System.currentTimeMillis()
            pausedDurationMs = 0L
            isPaused = false

            _recordingState.value = RecordingState.Recording(
                callType = callType,
                outputFile = outputFile,
                startTimeMs = startTimeMs,
                durationSeconds = 0L,
                amplitude = 0,
                isVideo = false
            )

            startAmplitudeTicker()
            Log.i(TAG, "Recording started: ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanup()
            _recordingState.value = RecordingState.Error("Failed to start audio recorder: ${e.localizedMessage ?: "Unknown error"}")
            return false
        }
    }

    @Synchronized
    fun pauseRecording(): Boolean {
        val recorder = mediaRecorder ?: return false
        if (isPaused) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder.pause()
                isPaused = true
                pauseStartTimeMs = System.currentTimeMillis()
                stopAmplitudeTicker()

                val currentElapsed = (pauseStartTimeMs - startTimeMs - pausedDurationMs) / 1000
                currentOutputFile?.let { file ->
                    _recordingState.value = RecordingState.Paused(
                        callType = currentCallType,
                        outputFile = file,
                        durationSeconds = maxOf(0L, currentElapsed),
                        isVideo = false
                    )
                }
                Log.i(TAG, "Recording paused")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording: ${e.message}")
            false
        }
    }

    @Synchronized
    fun resumeRecording(): Boolean {
        val recorder = mediaRecorder ?: return false
        if (!isPaused) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder.resume()
                isPaused = false
                pausedDurationMs += (System.currentTimeMillis() - pauseStartTimeMs)
                startAmplitudeTicker()

                val currentElapsed = (System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000
                currentOutputFile?.let { file ->
                    _recordingState.value = RecordingState.Recording(
                        callType = currentCallType,
                        outputFile = file,
                        startTimeMs = startTimeMs,
                        durationSeconds = maxOf(0L, currentElapsed),
                        amplitude = 0,
                        isVideo = false
                    )
                }
                Log.i(TAG, "Recording resumed")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording: ${e.message}")
            false
        }
    }

    @Synchronized
    fun stopRecording(): Long {
        stopAmplitudeTicker()
        val duration = if (startTimeMs > 0) {
            val totalTime = if (isPaused) pauseStartTimeMs else System.currentTimeMillis()
            maxOf(0L, totalTime - startTimeMs - pausedDurationMs)
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
            Log.e(TAG, "Error stopping mediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
            _recordingState.value = RecordingState.Idle
            Log.i(TAG, "Recording stopped. Duration: ${duration}ms")
        }
        return duration
    }

    private fun startAmplitudeTicker() {
        stopAmplitudeTicker()
        amplitudeTickerJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive && mediaRecorder != null && !isPaused) {
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                val durationSec = maxOf(0L, (System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000)

                currentOutputFile?.let { file ->
                    _recordingState.value = RecordingState.Recording(
                        callType = currentCallType,
                        outputFile = file,
                        startTimeMs = startTimeMs,
                        durationSeconds = durationSec,
                        amplitude = amp,
                        isVideo = false
                    )
                }
                delay(100L)
            }
        }
    }

    private fun stopAmplitudeTicker() {
        amplitudeTickerJob?.cancel()
        amplitudeTickerJob = null
    }

    private fun cleanup() {
        stopAmplitudeTicker()
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}
