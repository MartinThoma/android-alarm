package com.martin_thoma.drift.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Tabs a notification can open: the activity reads [EXTRA_TAB]. */
object OpenApp {
    const val EXTRA_TAB = "com.martin_thoma.drift.extra.TAB"
    const val TAB_STOPWATCH = "stopwatch"
    const val TAB_TIMER = "timer"

    /** Opens the app on [tab], reusing the open screen instead of stacking a new one. */
    fun pendingIntent(
        context: Context,
        tab: String,
    ): PendingIntent {
        val intent =
            checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
                .putExtra(EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, tab.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
