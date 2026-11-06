package com.martin_thoma.drift.feature.settings

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.martin_thoma.drift.core.notifications.DriftChannels

/** The state of every setting an alarm depends on (requirements GN-04). */
internal data class Health(
    val notifications: Notifications,
    val fullScreen: Boolean,
    val exactAlarms: Boolean,
    val battery: Battery,
    val samsung: Boolean,
) {
    enum class Notifications { ON, OFF, RINGING_TURNED_DOWN }

    enum class Battery { UNRESTRICTED, OPTIMISED, RESTRICTED }

    /**
     * Settings that can stop an alarm from ringing or from showing itself. An optimised battery is
     * only a tip: alarms set with `setAlarmClock` ring through Doze, and every app starts optimised.
     */
    val problems: Int
        get() = listOf(notifications != Notifications.ON, !fullScreen, !exactAlarms, battery == Battery.RESTRICTED).count { it }

    companion object {
        fun of(phone: PhoneSettings) =
            Health(
                notifications =
                    when {
                        !phone.notificationsEnabled -> {
                            Notifications.OFF
                        }

                        // Below high, a ringing alarm neither pops up nor fills the screen.
                        (phone.ringingChannelImportance ?: NotificationManager.IMPORTANCE_HIGH) < NotificationManager.IMPORTANCE_HIGH -> {
                            Notifications.RINGING_TURNED_DOWN
                        }

                        else -> {
                            Notifications.ON
                        }
                    },
                fullScreen = phone.fullScreenAllowed,
                exactAlarms = phone.exactAlarmsAllowed,
                battery =
                    when {
                        phone.backgroundRestricted -> Battery.RESTRICTED
                        phone.batteryOptimised -> Battery.OPTIMISED
                        else -> Battery.UNRESTRICTED
                    },
                samsung = phone.samsung,
            )
    }
}

/** The system screen that changes a setting, one tap away. */
internal enum class Fix {
    NOTIFICATIONS,
    RINGING_CHANNEL,
    FULL_SCREEN,
    EXACT_ALARMS,
    BATTERY_OPTIMISATION,
    APP_INFO,
    ;

    // Asking to stop battery optimisation is allowed for alarm apps (requirements 6.5).
    @SuppressLint("BatteryLife")
    fun intent(context: Context): Intent {
        val app = Uri.fromParts("package", context.packageName, null)
        return when (this) {
            NOTIFICATIONS -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }

            RINGING_CHANNEL -> {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, DriftChannels.RINGING)
            }

            FULL_SCREEN -> {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, app)
            }

            EXACT_ALARMS -> {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, app)
            }

            BATTERY_OPTIMISATION -> {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, app)
            }

            // "Restricted" is undone under App info > Battery; no screen goes there directly.
            APP_INFO -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, app)
            }
        }
    }
}
