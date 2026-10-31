package com.martin_thoma.tide.core.ringing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.martin_thoma.tide.core.data.AlarmRepository
import com.martin_thoma.tide.core.domain.TimeSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

internal const val TAG = "TideRinging"

/**
 * Rings an alarm: sound at the alarm's own volume, vibration, and a notification whose
 * full-screen intent shows the ringing screen (requirements 6.3). Stops on snooze, on dismiss,
 * or after [AUTO_SILENCE] (requirements AL-16). A new alarm replaces the one ringing.
 */
@AndroidEntryPoint
class RingingService : Service() {
    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var state: RingingState

    @Inject lateinit var player: AlarmPlayer

    @Inject lateinit var vibrator: AlarmVibrator

    @Inject lateinit var volume: AlarmVolume

    @Inject lateinit var notifications: RingingNotifications

    @Inject lateinit var timeSource: TimeSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ringing: Job? = null

    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tide:ringing").apply {
            setReferenceCounted(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_RING -> ring(intent.getLongExtra(EXTRA_ALARM_ID, -1), startId)
            ACTION_SNOOZE -> snooze(startId)
            else -> stop(startId)
        }
        return START_NOT_STICKY
    }

    private fun ring(
        alarmId: Long,
        startId: Int,
    ) {
        // A new alarm replaces the one ringing; the saved user volume stays saved.
        ringing?.cancel()
        player.stop()
        vibrator.stop()

        wakeLock.acquire(AUTO_SILENCE.inWholeMilliseconds + WAKE_LOCK_MARGIN_MILLIS)
        state.update(RingingStatus.Starting)
        val since = timeSource.now().toInstant()
        // Must come first: the system stops a foreground service that is slow to call this.
        startForeground(NOTIFICATION_ID, notifications.ringing(null, since), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)

        ringing =
            scope.launch {
                val alarm = repository.get(alarmId)
                if (alarm == null) {
                    Log.w(TAG, "Alarm $alarmId was deleted as it fired")
                    stop(startId)
                    return@launch
                }
                notifications.cancelSnoozed(alarm.id)
                volume.apply(alarm.volumePercent)
                player.play(alarm.soundUri)
                if (alarm.vibrate) vibrator.start()
                state.update(RingingStatus.Ringing(alarm, since))
                notifications.update(notifications.ringing(alarm, since))

                delay(AUTO_SILENCE)
                notifications.showMissed(alarm, since)
                stop(startId)
            }
    }

    private fun snooze(startId: Int) {
        val alarm = (state.status.value as? RingingStatus.Ringing)?.alarm
        silence()
        if (alarm == null) {
            stopSelf(startId)
            return
        }
        val until =
            timeSource
                .now()
                .toInstant()
                .plus(Duration.ofMinutes(alarm.snoozeMinutes.toLong()))
                .truncatedTo(ChronoUnit.SECONDS)
        scope.launch {
            // The scheduler picks the snooze up from the database, like any other change.
            repository.setSnoozedUntil(alarm.id, until)
            notifications.showSnoozed(alarm, until)
            // Only stops if no new alarm started meanwhile.
            stopSelf(startId)
        }
    }

    private fun stop(startId: Int) {
        silence()
        stopSelf(startId)
    }

    /** Ends the ring: sound, vibration, notification and volume go back to normal. */
    private fun silence() {
        ringing?.cancel()
        ringing = null
        player.stop()
        vibrator.stop()
        volume.restore()
        state.update(RingingStatus.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (wakeLock.isHeld) wakeLock.release()
    }

    override fun onDestroy() {
        if (state.status.value != RingingStatus.Idle) silence()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "com.martin_thoma.tide.action.RING"
        const val ACTION_SNOOZE = "com.martin_thoma.tide.action.SNOOZE"
        const val ACTION_DISMISS = "com.martin_thoma.tide.action.DISMISS"
        private const val EXTRA_ALARM_ID = "com.martin_thoma.tide.extra.ALARM_ID"

        internal const val NOTIFICATION_ID = 1

        /** How long an alarm rings if nobody stops it. A setting later (requirements GN-02). */
        val AUTO_SILENCE = 10.minutes
        private const val WAKE_LOCK_MARGIN_MILLIS = 60_000L

        /** Starts ringing [alarmId]. Allowed from the background right after an alarm fired. */
        fun ring(
            context: Context,
            alarmId: Long,
        ) {
            context.startForegroundService(intent(context, ACTION_RING).putExtra(EXTRA_ALARM_ID, alarmId))
        }

        internal fun intent(
            context: Context,
            action: String,
        ): Intent = Intent(context, RingingService::class.java).setAction(action)
    }
}
