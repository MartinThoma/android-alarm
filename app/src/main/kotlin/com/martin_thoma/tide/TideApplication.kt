package com.martin_thoma.tide

import android.app.Application
import com.martin_thoma.tide.core.scheduling.ScheduleSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TideApplication : Application() {
    @Inject lateinit var scheduleSync: ScheduleSync

    override fun onCreate() {
        super.onCreate()
        scheduleSync.start()
    }
}
