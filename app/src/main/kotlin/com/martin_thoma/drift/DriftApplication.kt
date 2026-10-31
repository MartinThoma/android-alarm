package com.martin_thoma.drift

import android.app.Application
import com.martin_thoma.drift.core.ringing.AlarmVolume
import com.martin_thoma.drift.core.scheduling.ScheduleSync
import com.martin_thoma.drift.feature.stopwatch.StopwatchNotifier
import com.martin_thoma.drift.feature.timer.TimerNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DriftApplication : Application() {
    @Inject lateinit var alarmVolume: AlarmVolume

    @Inject lateinit var scheduleSync: ScheduleSync

    @Inject lateinit var stopwatchNotifier: StopwatchNotifier

    @Inject lateinit var timerNotifier: TimerNotifier

    override fun onCreate() {
        super.onCreate()
        // Nothing rings in a fresh process, so a saved volume is left over from a ring the process died in.
        alarmVolume.restore()
        scheduleSync.start()
        stopwatchNotifier.start()
        timerNotifier.start()
    }
}
