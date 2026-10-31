package com.martin_thoma.drift.feature.stopwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.martin_thoma.drift.core.data.StopwatchRepository
import com.martin_thoma.drift.core.domain.TimeSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The buttons of the stopwatch notification. */
@AndroidEntryPoint
class StopwatchActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: StopwatchRepository

    @Inject lateinit var timeSource: TimeSource

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val now = timeSource.now().toInstant().toEpochMilli()
        repository.update { watch ->
            when (intent.action) {
                ACTION_PAUSE -> watch.pause(now)
                ACTION_RESUME -> watch.start(now)
                ACTION_LAP -> watch.lap(now)
                ACTION_RESET -> watch.reset()
                else -> watch
            }
        }
    }

    internal companion object {
        const val ACTION_PAUSE = "com.martin_thoma.drift.action.STOPWATCH_PAUSE"
        const val ACTION_RESUME = "com.martin_thoma.drift.action.STOPWATCH_RESUME"
        const val ACTION_LAP = "com.martin_thoma.drift.action.STOPWATCH_LAP"
        const val ACTION_RESET = "com.martin_thoma.drift.action.STOPWATCH_RESET"
    }
}
