package com.callrecorder.app.service

import android.app.Activity
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import com.callrecorder.app.data.local.AppDatabase
import com.callrecorder.app.data.repository.RecordingRepository
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.domain.model.UserSettings
import com.callrecorder.app.engine.VideoRecordingEngine
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

class ScreenRecordingService : Service() {

    companion object {
        private const val TAG = "ScreenRecordingService"
        const val ACTION_START = "com.callrecorder.app.action.START_VIDEO"
        const val ACTION_STOP = "com.callrecorder.app.action.STOP_VIDEO"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _activeState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val activeState: StateFlow<RecordingState> = _activeState.asStateFlow()

        var isServiceRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: RecordingRepository
    private lateinit var videoEngine: VideoRecordingEngine
    private var currentOutputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        RecordingNotificationHelper.createNotificationChannel(this)
        val db = AppDatabase.getDatabase(this)
        repository = RecordingRepository(db.recordingDao(), this)
        videoEngine = VideoRecordingEngine(this, serviceScope)

        serviceScope.launch {
            videoEngine.recordingState.collectLatest { state ->
                _activeState.value = state
                if (state is RecordingState.Recording) {
                    updateNotification(state.durationSeconds)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    Log.e(TAG, "Invalid MediaProjection consent data")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundWithNotification()
                initMediaProjectionAndRecord(resultCode, resultData)
            }
            ACTION_STOP -> {
                stopAndSaveRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = RecordingNotificationHelper.buildVideoNotification(this, 0)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                RecordingNotificationHelper.NOTIFICATION_ID_VIDEO,
                notification,
                foregroundType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MediaProjection foreground service: ${e.message}", e)
        }
    }

    private fun updateNotification(durationSeconds: Long) {
        val notification = RecordingNotificationHelper.buildVideoNotification(this, durationSeconds)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RecordingNotificationHelper.NOTIFICATION_ID_VIDEO, notification)
    }

    private fun initMediaProjectionAndRecord(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection token creation returned null")
            stopSelf()
            return
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val outputFile = repository.createOutputFile(CallType.WHATSAPP_VIDEO)
        currentOutputFile = outputFile

        val success = videoEngine.startRecording(
            projection = mediaProjection,
            outputFile = outputFile,
            displayMetrics = metrics,
            settings = UserSettings()
        )

        if (!success) {
            stopSelf()
        }
    }

    private fun stopAndSaveRecording() {
        val outputFile = currentOutputFile
        val durationMs = videoEngine.stopRecording()

        if (outputFile != null && outputFile.exists() && durationMs > 500) {
            serviceScope.launch(Dispatchers.IO) {
                repository.saveCompletedRecording(
                    file = outputFile,
                    callType = CallType.WHATSAPP_VIDEO,
                    durationMs = durationMs,
                    customTitle = "WhatsApp Video (${outputFile.nameWithoutExtension})"
                )
                stopForegroundAndService()
            }
        } else {
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
        videoEngine.stopRecording()
        _activeState.value = RecordingState.Idle
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
