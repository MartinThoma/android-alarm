package com.martin_thoma.tide.core.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.martin_thoma.tide.core.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Dismiss" on the snoozed notification: ends the snooze, so the alarm does not ring again. */
@AndroidEntryPoint
class SnoozeReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var notifications: RingingNotifications

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        notifications.cancelSnoozed(alarmId)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.setSnoozedUntil(alarmId, null)
            } finally {
                pending.finish()
            }
        }
    }

    internal companion object {
        const val EXTRA_ALARM_ID = "com.martin_thoma.tide.extra.ALARM_ID"
    }
}
