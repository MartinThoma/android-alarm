package com.martin_thoma.tide.feature.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The buttons of a timer's notification. */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {
    @Inject lateinit var actions: TimerActions

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val id = intent.getLongExtra(EXTRA_TIMER_ID, -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (intent.action) {
                    ACTION_START_OR_PAUSE -> actions.startOrPause(id)
                    ACTION_ADD_MINUTE -> actions.addMinute(id)
                    ACTION_RESET -> actions.reset(id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    internal companion object {
        const val ACTION_START_OR_PAUSE = "com.martin_thoma.tide.action.TIMER_START_OR_PAUSE"
        const val ACTION_ADD_MINUTE = "com.martin_thoma.tide.action.TIMER_ADD_MINUTE"
        const val ACTION_RESET = "com.martin_thoma.tide.action.TIMER_RESET"
        const val EXTRA_TIMER_ID = "com.martin_thoma.tide.extra.TIMER_ID"
    }
}
