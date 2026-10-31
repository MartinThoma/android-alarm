package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Per-alarm volume, approach A (requirements AL-05): while an alarm rings, the alarm stream
 * is set to the alarm's own level. The user's level is saved first and restored afterwards;
 * it is saved to disk, so it is also restored if the process dies mid-ring.
 */
@Singleton
class AlarmVolume
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val audioManager = context.getSystemService(AudioManager::class.java)

        // Device-protected, so this also works before the first unlock after a reboot.
        private val saved = context.createDeviceProtectedStorageContext().getSharedPreferences("alarm_volume", Context.MODE_PRIVATE)

        /** Sets the alarm stream to [percent] of its range. Keeps the level saved by an earlier, unfinished ring. */
        fun apply(percent: Int) {
            if (!saved.contains(KEY_USER_LEVEL)) {
                saved.edit(commit = true) { putInt(KEY_USER_LEVEL, audioManager.getStreamVolume(STREAM)) }
            }
            setLevel(levelFor(percent, audioManager.getStreamMinVolume(STREAM), audioManager.getStreamMaxVolume(STREAM)))
        }

        /** Puts back the user's level, if a ring changed it. Safe to call at any time. */
        fun restore() {
            if (!saved.contains(KEY_USER_LEVEL)) return
            setLevel(saved.getInt(KEY_USER_LEVEL, 0))
            saved.edit(commit = true) { remove(KEY_USER_LEVEL) }
        }

        private fun setLevel(level: Int) {
            try {
                audioManager.setStreamVolume(STREAM, level, 0)
            } catch (e: SecurityException) {
                // Only expected under Do Not Disturb rules we cannot change; the alarm still rings.
                Log.w(TAG, "Cannot set the alarm volume", e)
            }
        }

        internal companion object {
            const val STREAM = AudioManager.STREAM_ALARM
            const val KEY_USER_LEVEL = "user_level"

            /** [percent] of the stream's range, never silent: an alarm at 0 % still rings at the lowest level. */
            fun levelFor(
                percent: Int,
                minLevel: Int,
                maxLevel: Int,
            ): Int = (maxLevel * percent / 100.0).roundToInt().coerceIn(maxOf(minLevel, 1), maxLevel)
        }
    }
