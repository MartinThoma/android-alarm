package com.martin_thoma.drift.core.ringing

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.martin_thoma.drift.core.model.Alarm
import com.martin_thoma.drift.core.model.Timer
import com.martin_thoma.drift.core.notifications.DriftChannels
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Notification channels and notifications for ringing, snoozed and missed alarms (requirements 6.4). */
@Singleton
class RingingNotifications
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val screen: RingingScreen,
    ) {
        private val manager = context.getSystemService(NotificationManager::class.java)

        /** The foreground notification of [RingingService]; its full-screen intent opens the ringing screen. */
        fun ringing(
            alarm: Alarm?,
            since: Instant,
        ): Notification {
            createChannels()
            val screenIntent = PendingIntent.getActivity(context, 0, screen.intent(context), IMMUTABLE_UPDATE)
            return Notification
                .Builder(context, DriftChannels.RINGING)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title(alarm))
                .setContentText(formatTime(since))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(false)
                .setFullScreenIntent(screenIntent, true)
                .setContentIntent(screenIntent)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .setDeleteIntent(showAgain())
                .addAction(serviceAction(R.string.core_ringing_snooze, RingingService.ACTION_SNOOZE))
                .apply {
                    // Shaking and sums happen on the ringing screen only; a Dismiss here would skip them.
                    // While the alarm loads (`null`, a few milliseconds) its challenge is not known yet.
                    if (alarm != null && !alarm.challenge.wakesUp) {
                        addAction(serviceAction(R.string.core_ringing_dismiss, RingingService.ACTION_DISMISS))
                    }
                }.build()
        }

        /** The foreground notification while a timer rings: "+1 min" and Stop. */
        fun timerRinging(timer: Timer): Notification {
            val screenIntent = PendingIntent.getActivity(context, 0, screen.intent(context), IMMUTABLE_UPDATE)
            return Notification
                .Builder(context, DriftChannels.RINGING)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(timer.label.ifEmpty { context.getString(R.string.core_ringing_timer) })
                .setContentText(context.getString(R.string.core_ringing_times_up))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(false)
                .setFullScreenIntent(screenIntent, true)
                .setContentIntent(screenIntent)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .setDeleteIntent(showAgain())
                .addAction(serviceAction(R.string.core_ringing_add_minute, RingingService.ACTION_ADD_MINUTE))
                .addAction(serviceAction(R.string.core_ringing_stop, RingingService.ACTION_DISMISS))
                .build()
        }

        fun update(notification: Notification) = manager.notify(RingingService.NOTIFICATION_ID, notification)

        /** Posts the ringing notification for [status] again, after the user swiped it away. */
        fun showAgain(status: RingingStatus) {
            when (status) {
                is RingingStatus.Ringing -> {
                    update(ringing(status.alarm, status.since))
                }

                is RingingStatus.TimerRinging -> {
                    update(timerRinging(status.timer))
                }

                // Starting: the service posts the loaded alarm's notification in a moment. Idle: nothing rings.
                RingingStatus.Starting, RingingStatus.Idle -> {}
            }
        }

        /** Counts down to the end of the snooze, with an action to dismiss the alarm early (not past a wake-up challenge). */
        fun showSnoozed(
            alarm: Alarm,
            until: Instant,
        ) {
            createChannels()
            val dismiss =
                PendingIntent.getBroadcast(
                    context,
                    alarm.id.toInt(),
                    Intent(context, SnoozeReceiver::class.java).putExtra(SnoozeReceiver.EXTRA_ALARM_ID, alarm.id),
                    IMMUTABLE_UPDATE,
                )
            val notification =
                Notification
                    .Builder(context, DriftChannels.UPCOMING)
                    .setSmallIcon(R.drawable.ic_stat_alarm)
                    .setContentTitle(title(alarm))
                    .setContentText(context.getString(R.string.core_ringing_snoozed_until, formatTime(until)))
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setWhen(until.toEpochMilli())
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setContentIntent(openApp())
                    .apply {
                        // Snooze, then Dismiss from the lock screen, would also skip the challenge.
                        if (!alarm.challenge.wakesUp) {
                            addAction(Notification.Action.Builder(null, context.getString(R.string.core_ringing_dismiss), dismiss).build())
                        }
                    }.build()
            manager.notify(snoozedTag(alarm.id), 0, notification)
        }

        fun cancelSnoozed(alarmId: Long) = manager.cancel(snoozedTag(alarmId), 0)

        /** The alarm rang until auto-silence stopped it (requirements AL-16). */
        fun showMissed(
            alarm: Alarm,
            since: Instant,
        ) {
            createChannels()
            val notification =
                Notification
                    .Builder(context, DriftChannels.MISSED)
                    .setSmallIcon(R.drawable.ic_stat_alarm)
                    .setContentTitle(context.getString(R.string.core_ringing_missed))
                    .setContentText(context.getString(R.string.core_ringing_missed_text, title(alarm), formatTime(since)))
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setWhen(since.toEpochMilli())
                    .setContentIntent(openApp())
                    .setAutoCancel(true)
                    .build()
            manager.notify("missed:${alarm.id}", 0, notification)
        }

        private fun createChannels() = DriftChannels.ensure(context)

        private fun serviceAction(
            title: Int,
            action: String,
        ): Notification.Action {
            val intent = PendingIntent.getService(context, 0, RingingService.intent(context, action), IMMUTABLE_UPDATE)
            return Notification.Action.Builder(null, context.getString(title), intent).build()
        }

        /** Since Android 14 a foreground service's notification can be swiped away; a ringing one comes back. */
        private fun showAgain(): PendingIntent =
            PendingIntent.getBroadcast(context, 0, Intent(context, RingingDismissedReceiver::class.java), IMMUTABLE_UPDATE)

        private fun openApp(): PendingIntent {
            val launch = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun title(alarm: Alarm?) = alarm?.label?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.core_ringing_alarm)

        private fun formatTime(instant: Instant): String = DateFormat.getTimeFormat(context).format(Date.from(instant))

        private fun snoozedTag(alarmId: Long) = "snoozed:$alarmId"

        private companion object {
            const val IMMUTABLE_UPDATE = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
