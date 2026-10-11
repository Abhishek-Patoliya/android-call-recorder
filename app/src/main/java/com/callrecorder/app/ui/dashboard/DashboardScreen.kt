package com.callrecorder.app.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.ui.components.AudioPlayerSheet
import com.callrecorder.app.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onRequestMediaProjection: () -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    storagePath: String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val audioState by viewModel.audioRecordingState.collectAsState()
    val videoState by viewModel.videoRecordingState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val allRecordings by viewModel.allRecordings.collectAsState()
    val filteredRecordings by viewModel.filteredRecordings.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val totalDurationMs by viewModel.totalDurationMs.collectAsState()

    val currentlyPlayingRecording = remember(playbackState.recordingId, allRecordings) {
        allRecordings.firstOrNull { it.id == playbackState.recordingId }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    label = { Text("Normal") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                    label = { Text("WA Voice") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text("WA Video") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Recordings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    NormalCallsSection(
                        recordingState = audioState,
                        compatibilityReport = viewModel.compatibilityReport,
                        recentRecordings = allRecordings.filter { it.callType == CallType.NORMAL_CALL.name },
                        playingId = playbackState.recordingId,
                        onStartRecording = { num, contact ->
                            viewModel.startAudioRecording(CallType.NORMAL_CALL, num, contact)
                        },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onPauseRecording = { viewModel.pauseAudioRecording() },
                        onResumeRecording = { viewModel.resumeAudioRecording() },
                        onPlayToggle = { rec ->
                            if (playbackState.recordingId == rec.id && playbackState.isPlaying) {
                                viewModel.pausePlayback()
                            } else {
                                viewModel.playRecording(rec)
                            }
                        },
                        onRenameClick = { rec ->
                            // Handled in recordings or direct
                            selectedTab = 3
                        },
                        onShareClick = { rec -> onShareRecording(rec) },
                        onDeleteClick = { rec -> viewModel.deleteRecording(rec) }
                    )
                }
                1 -> {
                    WhatsAppVoiceSection(
                        recordingState = audioState,
                        compatibilityReport = viewModel.compatibilityReport,
                        recentRecordings = allRecordings.filter { it.callType == CallType.WHATSAPP_VOICE.name },
                        playingId = playbackState.recordingId,
                        onStartRecording = { tag ->
                            viewModel.startAudioRecording(CallType.WHATSAPP_VOICE, phoneNumber = tag, contactName = tag)
                        },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onPauseRecording = { viewModel.pauseAudioRecording() },
                        onResumeRecording = { viewModel.resumeAudioRecording() },
                        onPlayToggle = { rec ->
                            if (playbackState.recordingId == rec.id && playbackState.isPlaying) {
                                viewModel.pausePlayback()
                            } else {
                                viewModel.playRecording(rec)
                            }
                        },
                        onRenameClick = { selectedTab = 3 },
                        onShareClick = { rec -> onShareRecording(rec) },
                        onDeleteClick = { rec -> viewModel.deleteRecording(rec) }
                    )
                }
                2 -> {
                    WhatsAppVideoSection(
                        recordingState = videoState,
                        compatibilityReport = viewModel.compatibilityReport,
                        recentRecordings = allRecordings.filter { it.callType == CallType.WHATSAPP_VIDEO.name },
                        playingId = playbackState.recordingId,
                        onRequestMediaProjection = onRequestMediaProjection,
                        onStopVideoRecording = { viewModel.stopVideoRecording() },
                        onPlayToggle = { rec ->
                            // Audio or video player
                            if (playbackState.recordingId == rec.id && playbackState.isPlaying) {
                                viewModel.pausePlayback()
                            } else {
                                viewModel.playRecording(rec)
                            }
                        },
                        onRenameClick = { selectedTab = 3 },
                        onShareClick = { rec -> onShareRecording(rec) },
                        onDeleteClick = { rec -> viewModel.deleteRecording(rec) }
                    )
                }
                3 -> {
                    RecordingsSection(
                        recordings = filteredRecordings,
                        totalCount = totalCount,
                        totalDurationMs = totalDurationMs,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        playingId = playbackState.recordingId,
                        onFilterSelect = { viewModel.setFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onPlayToggle = { rec ->
                            if (playbackState.recordingId == rec.id && playbackState.isPlaying) {
                                viewModel.pausePlayback()
                            } else {
                                viewModel.playRecording(rec)
                            }
                        },
                        onRename = { id, title -> viewModel.renameRecording(id, title) },
                        onShare = { rec -> onShareRecording(rec) },
                        onDelete = { rec -> viewModel.deleteRecording(rec) }
                    )
                }
                4 -> {
                    SettingsSection(
                        settings = settings,
                        compatibilityReport = viewModel.compatibilityReport,
                        storagePath = storagePath,
                        onSettingsChanged = { viewModel.updateSettings(it) }
                    )
                }
            }

            // Persistent Audio Player Bottom Sheet if audio is active
            if (currentlyPlayingRecording != null && (playbackState.isPlaying || playbackState.currentPositionMs > 0)) {
                AudioPlayerSheet(
                    playbackState = playbackState,
                    title = currentlyPlayingRecording.title,
                    onPlayPause = {
                        if (playbackState.isPlaying) viewModel.pausePlayback() else viewModel.resumePlayback()
                    },
                    onSeek = { pos -> viewModel.seekPlayback(pos) },
                    onClose = { viewModel.stopPlayback() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
