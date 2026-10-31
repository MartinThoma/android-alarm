package com.martin_thoma.tide.core.scheduling

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.martin_thoma.tide.core.data.AlarmRepository
import com.martin_thoma.tide.core.domain.NextTrigger
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.model.Alarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings the system's alarms in line with the database. Every call is idempotent, so it is
 * safe to run after any change, boot, update or time change (requirements 6.1).
 */
@Singleton
class AlarmScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: AlarmRepository,
        private val timeSource: TimeSource,
    ) {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)
        private val scheduledIds = mutableSetOf<Long>()
        private val lastFired = mutableMapOf<Long, Instant>()

        suspend fun rescheduleAll() = schedule(repository.getAll())

        /** Records that [alarmId] fired for [triggerAt], so a slightly early delivery cannot ring twice. */
        @Synchronized
        fun markFired(
            alarmId: Long,
            triggerAt: Instant,
        ) {
            lastFired[alarmId] = triggerAt
        }

        @Synchronized
        fun schedule(alarms: List<Alarm>) {
            val now = timeSource.now()
            val current = alarms.mapTo(mutableSetOf()) { it.id }
            (scheduledIds - current).forEach(::cancel)
            scheduledIds.clear()
            for (alarm in alarms) {
                val notBefore = lastFired[alarm.id]?.let { maxOf(now, ZonedDateTime.ofInstant(it, now.zone)) } ?: now
                val trigger = NextTrigger.of(alarm, notBefore)
                if (trigger == null) {
                    cancel(alarm.id)
                } else if (setAlarmClock(alarm.id, trigger.toInstant())) {
                    scheduledIds += alarm.id
                }
            }
        }

        /**
         * Returns false if exact alarms are not allowed; the health check reports that state.
         * Lint only knows SCHEDULE_EXACT_ALARM for this call; we hold the equivalent USE_EXACT_ALARM
         * and check canScheduleExactAlarms() first.
         */
        @SuppressLint("MissingPermission")
        private fun setAlarmClock(
            alarmId: Long,
            triggerAt: Instant,
        ): Boolean {
            // USE_EXACT_ALARM is granted at install, so this only fails if the system revokes it.
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms; alarm $alarmId is not scheduled")
                return false
            }
            val info = AlarmManager.AlarmClockInfo(triggerAt.toEpochMilli(), showAppIntent())
            alarmManager.setAlarmClock(info, triggerIntent(alarmId, triggerAt))
            return true
        }

        private fun cancel(alarmId: Long) = alarmManager.cancel(triggerIntent(alarmId, Instant.EPOCH))

        private fun triggerIntent(
            alarmId: Long,
            triggerAt: Instant,
        ): PendingIntent {
            val intent =
                Intent(context, AlarmTriggerReceiver::class.java)
                    // A distinct data URI per alarm keeps the PendingIntents apart.
                    .setData("tide://alarm/$alarmId".toUri())
                    .putExtra(EXTRA_ALARM_ID, alarmId)
                    .putExtra(EXTRA_TRIGGER_AT, triggerAt.toEpochMilli())
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /** Opened when the user taps the next alarm in the system UI. */
        private fun showAppIntent(): PendingIntent {
            val launch = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        }

        internal companion object {
            const val TAG = "TideScheduling"
            const val EXTRA_ALARM_ID = "com.martin_thoma.tide.extra.ALARM_ID"
            const val EXTRA_TRIGGER_AT = "com.martin_thoma.tide.extra.TRIGGER_AT"
        }
    }
