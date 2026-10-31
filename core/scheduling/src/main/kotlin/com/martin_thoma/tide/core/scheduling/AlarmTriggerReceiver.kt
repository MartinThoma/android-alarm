package com.martin_thoma.tide.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.martin_thoma.tide.core.data.AlarmRepository
import com.martin_thoma.tide.core.ringing.RingingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Receives an alarm from [android.app.AlarmManager] when it is due. */
@AndroidEntryPoint
class AlarmTriggerReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var scheduler: AlarmScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val triggerAt = Instant.ofEpochMilli(intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0))
        val pending = goAsync()
        scope.launch {
            try {
                val alarm = repository.get(alarmId)
                // A deleted or disabled alarm can still fire once if its cancel was lost with the process.
                if (alarm != null && alarm.isActive && scheduler.markFired(alarmId, triggerAt)) {
                    Log.i(AlarmScheduler.TAG, "Alarm $alarmId is due")
                    // Ring first, so nothing below can keep the alarm silent.
                    RingingService.ring(context, alarmId)
                    repository.save(alarm.afterRinging())
                }
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
