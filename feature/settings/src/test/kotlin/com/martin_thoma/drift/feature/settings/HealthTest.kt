package com.martin_thoma.drift.feature.settings

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthTest {
    private data class Phone(
        override val notificationsEnabled: Boolean = true,
        override val ringingChannelImportance: Int? = null,
        override val fullScreenAllowed: Boolean = true,
        override val exactAlarmsAllowed: Boolean = true,
        override val backgroundRestricted: Boolean = false,
        override val batteryOptimised: Boolean = false,
        override val samsung: Boolean = false,
    ) : PhoneSettings

    @Test
    fun `a phone that allows everything has no problems`() {
        val health = Health.of(Phone())
        assertEquals(Health.Notifications.ON, health.notifications)
        assertEquals(Health.Battery.UNRESTRICTED, health.battery)
        assertEquals(0, health.problems)
    }

    @Test
    fun `an optimised battery is a tip, not a problem`() {
        val health = Health.of(Phone(batteryOptimised = true))
        assertEquals(Health.Battery.OPTIMISED, health.battery)
        assertEquals(0, health.problems)
    }

    @Test
    fun `restricted beats optimised`() {
        val health = Health.of(Phone(batteryOptimised = true, backgroundRestricted = true))
        assertEquals(Health.Battery.RESTRICTED, health.battery)
        assertEquals(1, health.problems)
    }

    @Test
    fun `a ringing channel below high is a problem, one not yet created is not`() {
        assertEquals(
            Health.Notifications.RINGING_TURNED_DOWN,
            Health.of(Phone(ringingChannelImportance = NotificationManager.IMPORTANCE_DEFAULT)).notifications,
        )
        assertEquals(
            Health.Notifications.ON,
            Health.of(Phone(ringingChannelImportance = NotificationManager.IMPORTANCE_HIGH)).notifications,
        )
        // Notifications off for the whole app says more than a turned-down channel.
        assertEquals(
            Health.Notifications.OFF,
            Health.of(Phone(notificationsEnabled = false, ringingChannelImportance = NotificationManager.IMPORTANCE_NONE)).notifications,
        )
    }

    @Test
    fun `every problem counts once`() {
        val phone = Phone(notificationsEnabled = false, fullScreenAllowed = false, exactAlarmsAllowed = false, backgroundRestricted = true)
        assertEquals(4, Health.of(phone).problems)
    }
}
