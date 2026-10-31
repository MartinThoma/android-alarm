package com.martin_thoma.tide

import android.app.Application
import com.martin_thoma.tide.core.ringing.AlarmVolume
import com.martin_thoma.tide.core.scheduling.ScheduleSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TideApplication : Application() {
    @Inject lateinit var alarmVolume: AlarmVolume

    @Inject lateinit var scheduleSync: ScheduleSync

    override fun onCreate() {
        super.onCreate()
        // Nothing rings in a fresh process, so a saved volume is left over from a ring the process died in.
        alarmVolume.restore()
        scheduleSync.start()
    }
}
