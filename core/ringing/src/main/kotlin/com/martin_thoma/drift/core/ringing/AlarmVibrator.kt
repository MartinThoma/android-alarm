package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A calm, repeating vibration for a ringing alarm. */
@Singleton
class AlarmVibrator
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

        fun start() {
            // Off, on, off; repeats from index 0.
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 1_000), 0)
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        }

        fun stop() = vibrator.cancel()
    }
