package com.callrecorder.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 20
    val normalizedAmp = (amplitude.coerceIn(0, 32767).toFloat() / 32767f).coerceIn(0.05f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val randomFactor = if (isRecording) {
                0.3f + (0.7f * ((index * 7 + 3) % 10) / 10f)
            } else {
                0.1f
            }
            val targetHeightFraction = if (isRecording) {
                (normalizedAmp * randomFactor).coerceIn(0.1f, 1f)
            } else {
                0.1f
            }

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeightFraction,
                animationSpec = tween(durationMillis = 100),
                label = "barHeight$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isRecording) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}
