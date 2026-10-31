package com.martin_thoma.tide.core.scheduling

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.martin_thoma.tide.core.data.TimerRepository
import com.martin_thoma.tide.core.model.Timer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One exact system alarm for every running timer, rebuilt from the database like alarms. It fires
 * in Doze too (requirements TM-06); unlike an alarm clock it does not show as the next alarm.
 */
@Singleton
class TimerScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: TimerRepository,
    ) {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)
        private val scheduledIds = mutableSetOf<Long>()

        suspend fun rescheduleAll() = schedule(repository.getAll())

        @Synchronized
        fun schedule(timers: List<Timer>) {
            val running = timers.filter { it.isRunning }
            val runningIds = running.mapTo(mutableSetOf()) { it.id }
            (scheduledIds - runningIds).forEach { alarmManager.cancel(expiryIntent(it)) }
            scheduledIds.clear()
            running.forEach { timer ->
                if (setExact(timer.id, checkNotNull(timer.endsAt))) scheduledIds += timer.id
            }
        }

        /** Lint only knows SCHEDULE_EXACT_ALARM; we hold USE_EXACT_ALARM and check canScheduleExactAlarms() first. */
        @SuppressLint("MissingPermission")
        private fun setExact(
            timerId: Long,
            endsAt: Long,
        ): Boolean {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(AlarmScheduler.TAG, "Cannot schedule exact alarms; timer $timerId is not scheduled")
                return false
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, expiryIntent(timerId))
            return true
        }

        private fun expiryIntent(timerId: Long): PendingIntent {
            val intent =
                Intent(context, TimerExpiredReceiver::class.java)
                    .setData("tide://timer/$timerId".toUri())
                    .putExtra(EXTRA_TIMER_ID, timerId)
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        internal companion object {
            const val EXTRA_TIMER_ID = "com.martin_thoma.tide.extra.TIMER_ID"
        }
    }
