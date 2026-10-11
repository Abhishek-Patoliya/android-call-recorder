package com.callrecorder.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.domain.model.CallType
import com.callrecorder.app.domain.model.CompatibilityReport
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.ui.components.CompatibilityCard
import com.callrecorder.app.ui.components.RecordingItemCard
import com.callrecorder.app.ui.components.WaveformVisualizer
import com.callrecorder.app.ui.theme.RecordingRed
import com.callrecorder.app.ui.theme.WarningAmber
import com.callrecorder.app.ui.theme.WarningAmberLight
import java.util.Locale

@Composable
fun NormalCallsSection(
    recordingState: RecordingState,
    compatibilityReport: CompatibilityReport,
    recentRecordings: List<RecordingEntity>,
    playingId: Long?,
    onStartRecording: (phoneNumber: String?, contactName: String?) -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onPlayToggle: (RecordingEntity) -> Unit,
    onRenameClick: (RecordingEntity) -> Unit,
    onShareClick: (RecordingEntity) -> Unit,
    onDeleteClick: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumberInput by remember { mutableStateOf("") }
    var contactNameInput by remember { mutableStateOf("") }

    val isRecordingActive = recordingState is RecordingState.Recording && recordingState.callType == CallType.NORMAL_CALL
    val isRecordingPaused = recordingState is RecordingState.Paused && recordingState.callType == CallType.NORMAL_CALL

    val durationSec = when (recordingState) {
        is RecordingState.Recording -> recordingState.durationSeconds
        is RecordingState.Paused -> recordingState.durationSeconds
        else -> 0L
    }

    val amplitude = if (recordingState is RecordingState.Recording) recordingState.amplitude else 0

    val formattedDuration = remember(durationSec) {
        val min = durationSec / 60
        val sec = durationSec % 60
        String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cellular Call Recording",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Standard telephone network voice calls",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Recording Card / Controller
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecordingActive) {
                        RecordingRed.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isRecordingActive -> RecordingRed
                                        isRecordingPaused -> WarningAmber
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isRecordingActive -> "RECORDING ACTIVE"
                                isRecordingPaused -> "RECORDING PAUSED"
                                else -> "RECORDER READY"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRecordingActive -> RecordingRed
                                isRecordingPaused -> WarningAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = formattedDuration,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    WaveformVisualizer(
                        amplitude = amplitude,
                        isRecording = isRecordingActive
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isRecordingActive && !isRecordingPaused) {
                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            label = { Text("Phone Number / Tag (Optional)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = contactNameInput,
                            onValueChange = { contactNameInput = it },
                            label = { Text("Contact Name (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onStartRecording(
                                    phoneNumberInput.ifBlank { null },
                                    contactNameInput.ifBlank { null }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Call Recording")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isRecordingPaused) onResumeRecording() else onPauseRecording()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRecordingPaused) "Resume" else "Pause")
                            }

                            Button(
                                onClick = onStopRecording,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecordingRed
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Save")
                            }
                        }
                    }
                }
            }
        }

        // Speakerphone Advice Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmberLight)
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Speakerphone Recommended for 2-Way Voice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Android OS isolates raw incoming cellular modem audio. To record both your voice and the caller's voice clearly, keep your phone on speakerphone during active calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // Technical Capabilities
        item {
            Text(
                text = "Android OS Capabilities & Compliance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(compatibilityReport.cellularCapabilities) { capability ->
            CompatibilityCard(item = capability)
        }

        // Recent Normal Recordings
        if (recentRecordings.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Cellular Recordings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentRecordings.take(5), key = { it.id }) { rec ->
                RecordingItemCard(
                    recording = rec,
                    isPlaying = playingId == rec.id,
                    onPlayToggle = { onPlayToggle(rec) },
                    onRenameClick = { onRenameClick(rec) },
                    onShareClick = { onShareClick(rec) },
                    onDeleteClick = { onDeleteClick(rec) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
