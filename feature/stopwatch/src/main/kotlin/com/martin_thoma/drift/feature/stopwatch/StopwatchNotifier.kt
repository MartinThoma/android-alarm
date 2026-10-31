package com.martin_thoma.drift.feature.stopwatch

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.martin_thoma.drift.core.data.StopwatchRepository
import com.martin_thoma.drift.core.designsystem.formatDuration
import com.martin_thoma.drift.core.domain.TimeSource
import com.martin_thoma.drift.core.model.Stopwatch
import com.martin_thoma.drift.core.notifications.DriftChannels
import com.martin_thoma.drift.core.notifications.OpenApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows the stopwatch in a notification while it is not reset (requirements SW-04). The system
 * counts the seconds itself (a chronometer), so nothing in the app ticks in the background.
 */
@Singleton
class StopwatchNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: StopwatchRepository,
        private val timeSource: TimeSource,
    ) {
        private val manager = context.getSystemService(NotificationManager::class.java)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        /** Follows the stopwatch for as long as the process lives. Started once by the Application. */
        fun start() {
            repository.stopwatch.onEach(::show).launchIn(scope)
        }

        private fun show(watch: Stopwatch) {
            if (watch.isReset) {
                manager.cancel(NOTIFICATION_TAG, 0)
                return
            }
            DriftChannels.ensure(context)
            val now = timeSource.now().toInstant().toEpochMilli()
            val elapsed = watch.elapsed(now)
            val locale = context.resources.configuration.locales[0]
            val builder =
                Notification
                    .Builder(context, DriftChannels.RUNNING)
                    .setSmallIcon(R.drawable.ic_stat_stopwatch)
                    .setContentTitle(context.getString(R.string.feature_stopwatch_title))
                    .setCategory(Notification.CATEGORY_STOPWATCH)
                    .setOnlyAlertOnce(true)
                    .setOngoing(watch.isRunning)
                    .setContentIntent(OpenApp.pendingIntent(context, OpenApp.TAB_STOPWATCH))
            if (watch.isRunning) {
                builder
                    .setUsesChronometer(true)
                    .setShowWhen(true)
                    .setWhen(now - elapsed)
                    .addAction(action(R.string.feature_stopwatch_pause, StopwatchActionReceiver.ACTION_PAUSE))
                    .addAction(action(R.string.feature_stopwatch_lap, StopwatchActionReceiver.ACTION_LAP))
            } else {
                builder
                    .setShowWhen(false)
                    .setContentText(context.getString(R.string.feature_stopwatch_paused, formatDuration(elapsed, locale)))
                    .addAction(action(R.string.feature_stopwatch_resume, StopwatchActionReceiver.ACTION_RESUME))
            }
            builder.addAction(action(R.string.feature_stopwatch_reset, StopwatchActionReceiver.ACTION_RESET))
            manager.notify(NOTIFICATION_TAG, 0, builder.build())
        }

        private fun action(
            title: Int,
            action: String,
        ): Notification.Action {
            val intent = Intent(context, StopwatchActionReceiver::class.java).setAction(action)
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return Notification.Action.Builder(null, context.getString(title), pending).build()
        }

        private companion object {
            const val NOTIFICATION_TAG = "stopwatch"
        }
    }
