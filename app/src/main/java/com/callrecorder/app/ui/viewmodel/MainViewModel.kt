package com.callrecorder.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.data.local.AppDatabase
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.data.repository.RecordingRepository
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.CompatibilityReport
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.domain.model.UserSettings
import com.callrecorder.app.engine.AudioPlaybackEngine
import com.callrecorder.app.engine.CompatibilityChecker
import com.callrecorder.app.engine.PlaybackState
import com.callrecorder.app.service.CallRecordingService
import com.callrecorder.app.service.ScreenRecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository
    private val playbackEngine: AudioPlaybackEngine

    val compatibilityReport: CompatibilityReport

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _selectedFilter = MutableStateFlow<CallType?>(null)
    val selectedFilter: StateFlow<CallType?> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val audioRecordingState: StateFlow<RecordingState> = CallRecordingService.activeState
    val videoRecordingState: StateFlow<RecordingState> = ScreenRecordingService.activeState
    val playbackState: StateFlow<PlaybackState>

    val allRecordings: StateFlow<List<RecordingEntity>>
    val filteredRecordings: StateFlow<List<RecordingEntity>>

    val totalCount: StateFlow<Int>
    val totalDurationMs: StateFlow<Long>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RecordingRepository(db.recordingDao(), application)
        playbackEngine = AudioPlaybackEngine(application, viewModelScope)
        playbackState = playbackEngine.playbackState
        compatibilityReport = CompatibilityChecker.generateReport(application)

        allRecordings = repository.allRecordings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        totalCount = repository.getTotalCount().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        totalDurationMs = repository.getTotalDuration().combine(_settings) { dur, _ ->
            dur ?: 0L
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

        filteredRecordings = combine(allRecordings, _selectedFilter, _searchQuery) { list, filter, query ->
            list.filter { item ->
                val matchesFilter = filter == null || item.callType.equals(filter.name, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        (item.phoneNumber?.contains(query, ignoreCase = true) == true) ||
                        (item.contactName?.contains(query, ignoreCase = true) == true)
                matchesFilter && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setFilter(callType: CallType?) {
        _selectedFilter.value = callType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startAudioRecording(
        callType: CallType,
        phoneNumber: String? = null,
        contactName: String? = null
    ) {
        val app = getApplication<Application>()
        val intent = Intent(app, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_START
            putExtra(CallRecordingService.EXTRA_CALL_TYPE, callType.name)
            putExtra(CallRecordingService.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(CallRecordingService.EXTRA_CONTACT_NAME, contactName)
            putExtra(CallRecordingService.EXTRA_AUDIO_SOURCE, _settings.value.audioSource.name)
            putExtra(CallRecordingService.EXTRA_AUDIO_FORMAT, _settings.value.audioFormat.name)
        }
        app.startService(intent)
    }

    fun stopAudioRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_STOP
        }
        app.startService(intent)
    }

    fun pauseAudioRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_PAUSE
        }
        app.startService(intent)
    }

    fun resumeAudioRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_RESUME
        }
        app.startService(intent)
    }

    fun startVideoRecording(resultCode: Int, data: Intent) {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, data)
        }
        app.startService(intent)
    }

    fun stopVideoRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_STOP
        }
        app.startService(intent)
    }

    fun playRecording(recording: RecordingEntity) {
        val file = File(recording.filePath)
        playbackEngine.play(recording.id, file)
    }

    fun pausePlayback() {
        playbackEngine.pause()
    }

    fun resumePlayback() {
        playbackEngine.resume()
    }

    fun seekPlayback(positionMs: Long) {
        playbackEngine.seekTo(positionMs)
    }

    fun stopPlayback() {
        playbackEngine.stop()
    }

    fun renameRecording(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameRecording(id, newTitle)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playbackState.value.recordingId == recording.id) {
                stopPlayback()
            }
            repository.deleteRecording(recording)
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
    }

    override fun onCleared() {
        playbackEngine.stop()
        super.onCleared()
    }
}
