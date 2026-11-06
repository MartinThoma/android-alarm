package com.martin_thoma.drift.core.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The ringing notification was swiped away while it still rings: shows it again, so Snooze,
 * Dismiss and the ringing screen stay one tap away. Runs on the main thread, like [RingingService],
 * so the status cannot change halfway.
 */
@AndroidEntryPoint
class RingingDismissedReceiver : BroadcastReceiver() {
    @Inject lateinit var state: RingingState

    @Inject lateinit var notifications: RingingNotifications

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        notifications.showAgain(state.status.value)
    }
}
