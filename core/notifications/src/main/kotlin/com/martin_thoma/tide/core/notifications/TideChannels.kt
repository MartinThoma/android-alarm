package com.martin_thoma.tide.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Tide's notification channels (requirements 6.4). Creating them again is cheap and harmless. */
object TideChannels {
    const val RINGING = "ringing"
    const val UPCOMING = "upcoming"
    const val MISSED = "missed"
    const val RUNNING = "running"

    fun ensure(context: Context) {
        fun channel(
            id: String,
            name: Int,
            description: Int,
            importance: Int,
        ) = NotificationChannel(id, context.getString(name), importance).apply { this.description = context.getString(description) }

        val ringing =
            channel(
                RINGING,
                R.string.core_notifications_channel_ringing,
                R.string.core_notifications_channel_ringing_description,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // The ringing service plays the sound and vibrates on the alarm stream itself.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        val upcoming =
            channel(
                UPCOMING,
                R.string.core_notifications_channel_upcoming,
                R.string.core_notifications_channel_upcoming_description,
                NotificationManager.IMPORTANCE_LOW,
            )
        val missed =
            channel(
                MISSED,
                R.string.core_notifications_channel_missed,
                R.string.core_notifications_channel_missed_description,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        // A running stopwatch or timer updates often; it must never make a sound.
        val running =
            channel(
                RUNNING,
                R.string.core_notifications_channel_running,
                R.string.core_notifications_channel_running_description,
                NotificationManager.IMPORTANCE_LOW,
            )
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(ringing, upcoming, missed, running))
    }
}
