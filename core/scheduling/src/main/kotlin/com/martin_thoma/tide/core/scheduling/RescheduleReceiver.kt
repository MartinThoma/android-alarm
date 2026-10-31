package com.martin_thoma.tide.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rebuilds all system alarms after boot, app update and time or time-zone changes. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: AlarmScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.i(AlarmScheduler.TAG, "Rescheduling after ${intent.action}")
        val pending = goAsync()
        scope.launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
