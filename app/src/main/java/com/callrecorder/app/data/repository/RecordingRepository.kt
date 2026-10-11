package com.callrecorder.app.data.repository

import android.content.Context
import com.callrecorder.app.data.local.dao.RecordingDao
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.domain.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val context: Context
) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    fun getRecordingsByType(type: CallType): Flow<List<RecordingEntity>> {
        return recordingDao.getRecordingsByType(type.name)
    }

    fun getTotalCount(): Flow<Int> = recordingDao.getTotalCount()
    fun getTotalDuration(): Flow<Long?> = recordingDao.getTotalDuration()

    fun getRecordingsDirectory(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createOutputFile(callType: CallType): File {
        val dir = getRecordingsDirectory()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${callType.name.lowercase()}_$timeStamp.${callType.extension}"
        return File(dir, fileName)
    }

    suspend fun saveCompletedRecording(
        file: File,
        callType: CallType,
        durationMs: Long,
        phoneNumber: String? = null,
        contactName: String? = null,
        customTitle: String? = null
    ): RecordingEntity = withContext(Dispatchers.IO) {
        val size = if (file.exists()) file.length() else 0L
        val timeStampStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        val defaultTitle = customTitle ?: "${callType.displayName} ($timeStampStr)"

        val entity = RecordingEntity(
            title = defaultTitle,
            filePath = file.absolutePath,
            fileName = file.name,
            callType = callType.name,
            phoneNumber = phoneNumber,
            contactName = contactName,
            durationMs = durationMs,
            fileSizeBytes = size,
            timestamp = System.currentTimeMillis(),
            format = callType.extension,
            mimeType = callType.mimeType
        )
        val id = recordingDao.insertRecording(entity)
        entity.copy(id = id)
    }

    suspend fun renameRecording(id: Long, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        recordingDao.updateTitle(id, newTitle.trim())
        true
    }

    suspend fun deleteRecording(recording: RecordingEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
            recordingDao.deleteRecording(recording)
            true
        } catch (e: Exception) {
            recordingDao.deleteRecording(recording)
            false
        }
    }
}
