package com.martin_thoma.drift.feature.settings

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.martin_thoma.drift.core.notifications.DriftChannels
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** What the phone allows Drift. Read afresh each time: the user can change any of it in the system settings. */
interface PhoneSettings {
    val notificationsEnabled: Boolean

    /** The importance the user left the ringing channel at, or `null` before the first ring created it. */
    val ringingChannelImportance: Int?

    val fullScreenAllowed: Boolean

    val exactAlarmsAllowed: Boolean

    /** The user set the app's battery use to "Restricted". */
    val backgroundRestricted: Boolean

    val batteryOptimised: Boolean

    /** Samsung puts apps to sleep on its own, and no API tells us; the health check explains it. */
    val samsung: Boolean
}

class AndroidPhoneSettings
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PhoneSettings {
        private val notifications = context.getSystemService(NotificationManager::class.java)

        override val notificationsEnabled get() = notifications.areNotificationsEnabled()

        override val ringingChannelImportance get() = notifications.getNotificationChannel(DriftChannels.RINGING)?.importance

        override val fullScreenAllowed get() = notifications.canUseFullScreenIntent()

        override val exactAlarmsAllowed get() = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

        override val backgroundRestricted get() = context.getSystemService(ActivityManager::class.java).isBackgroundRestricted

        override val batteryOptimised get() =
            !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

        override val samsung get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

@Module
@InstallIn(SingletonComponent::class)
interface PhoneSettingsModule {
    @Binds
    fun phoneSettings(settings: AndroidPhoneSettings): PhoneSettings
}
