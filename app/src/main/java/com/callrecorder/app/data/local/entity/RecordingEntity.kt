package com.callrecorder.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileName: String,
    val callType: String,
    val phoneNumber: String? = null,
    val contactName: String? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val format: String = "m4a",
    val mimeType: String = "audio/mp4",
    val notes: String? = null,
    val isFavorite: Boolean = false
)
