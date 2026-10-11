package com.callrecorder.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.callrecorder.app.data.local.AppDatabase
import com.callrecorder.app.data.repository.RecordingRepository
import com.callrecorder.app.domain.model.AppAudioSource
import com.callrecorder.app.domain.model.AudioFormatOption
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.domain.model.UserSettings
import com.callrecorder.app.engine.AudioRecordingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class CallRecordingService : Service() {

    companion object {
        private const val TAG = "CallRecordingService"
        const val ACTION_START = "com.callrecorder.app.action.START_AUDIO"
        const val ACTION_STOP = "com.callrecorder.app.action.STOP_AUDIO"
        const val ACTION_PAUSE = "com.callrecorder.app.action.PAUSE_AUDIO"
        const val ACTION_RESUME = "com.callrecorder.app.action.RESUME_AUDIO"

        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
        const val EXTRA_AUDIO_FORMAT = "extra_audio_format"

        private val _activeState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val activeState: StateFlow<RecordingState> = _activeState.asStateFlow()

        var isServiceRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: RecordingRepository
    private lateinit var recordingEngine: AudioRecordingEngine
    private var currentCallType = CallType.NORMAL_CALL
    private var currentPhoneNumber: String? = null
    private var currentContactName: String? = null
    private var currentOutputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        RecordingNotificationHelper.createNotificationChannel(this)
        val db = AppDatabase.getDatabase(this)
        repository = RecordingRepository(db.recordingDao(), this)
        recordingEngine = AudioRecordingEngine(this, serviceScope)

        serviceScope.launch {
            recordingEngine.recordingState.collectLatest { state ->
                _activeState.value = state
                when (state) {
                    is RecordingState.Recording -> {
                        updateNotification(state.durationSeconds, isPaused = false)
                    }
                    is RecordingState.Paused -> {
                        updateNotification(state.durationSeconds, isPaused = true)
                    }
                    is RecordingState.Idle, is RecordingState.Error -> {
                        // Handled on stop
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val typeStr = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CallType.NORMAL_CALL.name
                currentCallType = CallType.fromString(typeStr)
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                currentContactName = intent.getStringExtra(EXTRA_CONTACT_NAME)

                val audioSourceName = intent.getStringExtra(EXTRA_AUDIO_SOURCE) ?: AppAudioSource.MIC.name
                val audioFormatName = intent.getStringExtra(EXTRA_AUDIO_FORMAT) ?: AudioFormatOption.AAC_M4A.name

                val audioSource = AppAudioSource.entries.firstOrNull { it.name == audioSourceName } ?: AppAudioSource.MIC
                val audioFormat = AudioFormatOption.entries.firstOrNull { it.name == audioFormatName } ?: AudioFormatOption.AAC_M4A

                val settings = UserSettings(
                    audioSource = audioSource,
                    audioFormat = audioFormat
                )

                startForegroundWithNotification()
                startRecording(settings)
            }
            ACTION_STOP -> {
                stopAndSaveRecording()
            }
            ACTION_PAUSE -> {
                recordingEngine.pauseRecording()
            }
            ACTION_RESUME -> {
                recordingEngine.resumeRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = RecordingNotificationHelper.buildAudioNotification(
            this,
            currentCallType,
            durationSeconds = 0,
            isPaused = false
        )

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                RecordingNotificationHelper.NOTIFICATION_ID_AUDIO,
                notification,
                foregroundType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
        }
    }

    private fun updateNotification(durationSeconds: Long, isPaused: Boolean) {
        val notification = RecordingNotificationHelper.buildAudioNotification(
            this,
            currentCallType,
            durationSeconds,
            isPaused
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RecordingNotificationHelper.NOTIFICATION_ID_AUDIO, notification)
    }

    private fun startRecording(settings: UserSettings) {
        val outputFile = repository.createOutputFile(currentCallType)
        currentOutputFile = outputFile
        val success = recordingEngine.startRecording(outputFile, currentCallType, settings)
        if (!success) {
            stopSelf()
        }
    }

    private fun stopAndSaveRecording() {
        val outputFile = currentOutputFile
        val durationMs = recordingEngine.stopRecording()

        if (outputFile != null && outputFile.exists() && durationMs > 500) {
            serviceScope.launch(Dispatchers.IO) {
                repository.saveCompletedRecording(
                    file = outputFile,
                    callType = currentCallType,
                    durationMs = durationMs,
                    phoneNumber = currentPhoneNumber,
                    contactName = currentContactName
                )
                stopForegroundAndService()
            }
        } else {
            // Very short or corrupted file
            outputFile?.delete()
            stopForegroundAndService()
        }
    }

    private fun stopForegroundAndService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isServiceRunning = false
        recordingEngine.stopRecording()
        _activeState.value = RecordingState.Idle
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
