package com.callrecorder.app

import android.app.Application
import com.callrecorder.app.service.RecordingNotificationHelper

class CallRecorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecordingNotificationHelper.createNotificationChannel(this)
    }
}
