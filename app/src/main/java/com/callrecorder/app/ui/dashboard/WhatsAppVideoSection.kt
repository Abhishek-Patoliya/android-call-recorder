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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.domain.model.CompatibilityReport
import com.callrecorder.app.domain.model.RecordingState
import com.callrecorder.app.ui.components.CompatibilityCard
import com.callrecorder.app.ui.components.RecordingItemCard
import com.callrecorder.app.ui.theme.RecordingRed
import com.callrecorder.app.ui.theme.SuccessGreenLight
import java.util.Locale

@Composable
fun WhatsAppVideoSection(
    recordingState: RecordingState,
    compatibilityReport: CompatibilityReport,
    recentRecordings: List<RecordingEntity>,
    playingId: Long?,
    onRequestMediaProjection: () -> Unit,
    onStopVideoRecording: () -> Unit,
    onPlayToggle: (RecordingEntity) -> Unit,
    onRenameClick: (RecordingEntity) -> Unit,
    onShareClick: (RecordingEntity) -> Unit,
    onDeleteClick: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecordingActive = recordingState is RecordingState.Recording && recordingState.isVideo

    val durationSec = if (recordingState is RecordingState.Recording && recordingState.isVideo) {
        recordingState.durationSeconds
    } else 0L

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
                text = "WhatsApp Video Calls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "MediaProjection screen video & audio capture",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Privacy & Consent Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Explicit User Consent Mandatory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Android strictly requires explicit system consent before initiating screen projection. An ongoing foreground notification will remain active throughout the session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Controller Card
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
                                .background(if (isRecordingActive) RecordingRed else Color(0xFF0277BD))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecordingActive) "VIDEO CAPTURE ACTIVE" else "VIDEO RECORDER READY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecordingActive) RecordingRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = formattedDuration,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isRecordingActive) {
                        Button(
                            onClick = onRequestMediaProjection,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Consent & Record Video")
                        }
                    } else {
                        Button(
                            onClick = onStopVideoRecording,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RecordingRed)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop & Save Video Recording")
                        }
                    }
                }
            }
        }

        // Capabilities List
        item {
            Text(
                text = "Screen Recording Technical Specifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(compatibilityReport.whatsappVideoCapabilities) { capability ->
            CompatibilityCard(item = capability)
        }

        // Recent WhatsApp Video Recordings
        if (recentRecordings.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent WhatsApp Video Recordings",
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
