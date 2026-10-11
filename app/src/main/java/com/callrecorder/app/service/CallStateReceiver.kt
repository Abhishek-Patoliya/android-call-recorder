package com.callrecorder.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.callrecorder.app.domain.model.CallType

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            val state = when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
                else -> return
            }

            onCallStateChanged(context, state, number)
        }
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                savedNumber = number
                Log.d(TAG, "Cellular Call Ringing: $number")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                    Log.d(TAG, "Cellular Call Outgoing Offhook")
                } else {
                    Log.d(TAG, "Cellular Call Incoming Answered: $savedNumber")
                }
                // Note: Auto-recording check can be configured by user in settings.
                // If auto-recording is enabled, start service
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Cellular Call Ended")
                if (CallRecordingService.isServiceRunning) {
                    val stopIntent = Intent(context, CallRecordingService::class.java).apply {
                        action = CallRecordingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                }
                isIncoming = false
                savedNumber = null
            }
        }
        lastState = state
    }
}
