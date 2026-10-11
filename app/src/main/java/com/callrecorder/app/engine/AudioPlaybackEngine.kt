package com.callrecorder.app.engine

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
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

data class PlaybackState(
    val recordingId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val filePath: String? = null
)

class AudioPlaybackEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "AudioPlaybackEngine"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun play(recordingId: Long, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "File not found for playback: ${file.absolutePath}")
            return
        }

        if (_playbackState.value.recordingId == recordingId && mediaPlayer != null) {
            resume()
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    stopTicker()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration.toLong(),
                        isCompleted = true
                    )
                }
                start()
            }

            mediaPlayer = player
            _playbackState.value = PlaybackState(
                recordingId = recordingId,
                isPlaying = true,
                currentPositionMs = 0L,
                totalDurationMs = player.duration.toLong(),
                isCompleted = false,
                filePath = file.absolutePath
            )

            startProgressTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                stopTicker()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = player.currentPosition.toLong()
                )
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isCompleted = false
                )
                startProgressTicker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = positionMs
            )
        }
    }

    fun stop() {
        stopTicker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
            _playbackState.value = PlaybackState()
        }
    }

    private fun startProgressTicker() {
        stopTicker()
        tickerJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                val total = mediaPlayer?.duration?.toLong() ?: 0L
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = current,
                    totalDurationMs = total
                )
                delay(200L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
