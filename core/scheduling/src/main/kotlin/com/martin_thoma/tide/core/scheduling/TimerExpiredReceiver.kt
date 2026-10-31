package com.martin_thoma.tide.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.martin_thoma.tide.core.data.TimerRepository
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.ringing.RingingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A timer has run out: ring it like an alarm (requirements TM-06). */
@AndroidEntryPoint
class TimerExpiredReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: TimerRepository

    @Inject lateinit var timeSource: TimeSource

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val timerId = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1)
        val pending = goAsync()
        scope.launch {
            try {
                val timer = repository.get(timerId)
                // A paused, reset or deleted timer can still fire once if its cancel was lost with the process.
                if (timer != null && timer.hasExpired(timeSource.now().toInstant().toEpochMilli() + EARLY_TOLERANCE_MILLIS)) {
                    Log.i(AlarmScheduler.TAG, "Timer $timerId has run out")
                    RingingService.ringTimer(context, timerId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        /** The system may deliver a moment early. */
        const val EARLY_TOLERANCE_MILLIS = 1_000L
    }
}
