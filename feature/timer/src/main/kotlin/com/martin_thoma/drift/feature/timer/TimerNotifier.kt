package com.martin_thoma.drift.feature.timer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.martin_thoma.drift.core.data.TimerRepository
import com.martin_thoma.drift.core.designsystem.formatDuration
import com.martin_thoma.drift.core.domain.TimeSource
import com.martin_thoma.drift.core.model.Timer
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
 * One notification per started timer (requirements TM-07). The system counts down by itself, and
 * past zero it counts the overtime, so nothing in the app ticks in the background.
 */
@Singleton
class TimerNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: TimerRepository,
        private val timeSource: TimeSource,
    ) {
        private val manager = context.getSystemService(NotificationManager::class.java)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val shown = mutableSetOf<Long>()

        /** Follows the timers for as long as the process lives. Started once by the Application. */
        fun start() {
            repository.timers.onEach(::show).launchIn(scope)
        }

        private fun show(timers: List<Timer>) {
            val active = timers.filterNot { it.isIdle }
            (shown - active.mapTo(mutableSetOf()) { it.id }).forEach { manager.cancel(tag(it), 0) }
            shown.clear()
            if (active.isEmpty()) return
            DriftChannels.ensure(context)
            val now = timeSource.now().toInstant().toEpochMilli()
            val locale = context.resources.configuration.locales[0]
            for (timer in active) {
                val builder =
                    Notification
                        .Builder(context, DriftChannels.RUNNING)
                        .setSmallIcon(R.drawable.ic_stat_timer)
                        .setContentTitle(timer.label.ifEmpty { context.getString(R.string.feature_timer_title) })
                        .setCategory(Notification.CATEGORY_PROGRESS)
                        .setOnlyAlertOnce(true)
                        .setOngoing(timer.isRunning)
                        .setGroup(GROUP)
                        .setContentIntent(OpenApp.pendingIntent(context, OpenApp.TAB_TIMER))
                if (timer.isRunning) {
                    builder
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setShowWhen(true)
                        .setWhen(checkNotNull(timer.endsAt))
                        .addAction(action(timer, R.string.feature_timer_pause, TimerActionReceiver.ACTION_START_OR_PAUSE))
                        .addAction(action(timer, R.string.feature_timer_add_minute, TimerActionReceiver.ACTION_ADD_MINUTE))
                } else {
                    builder
                        .setShowWhen(false)
                        .setContentText(context.getString(R.string.feature_timer_paused, formatDuration(timer.remaining(now), locale)))
                        .addAction(action(timer, R.string.feature_timer_resume, TimerActionReceiver.ACTION_START_OR_PAUSE))
                }
                builder.addAction(action(timer, R.string.feature_timer_reset, TimerActionReceiver.ACTION_RESET))
                manager.notify(tag(timer.id), 0, builder.build())
                shown += timer.id
            }
        }

        private fun action(
            timer: Timer,
            title: Int,
            action: String,
        ): Notification.Action {
            val intent =
                Intent(context, TimerActionReceiver::class.java)
                    .setAction(action)
                    .setData("drift://timer/${timer.id}".toUri())
                    .putExtra(TimerActionReceiver.EXTRA_TIMER_ID, timer.id)
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return Notification.Action.Builder(null, context.getString(title), pending).build()
        }

        private fun tag(id: Long) = "timer:$id"

        private companion object {
            const val GROUP = "timers"
        }
    }
