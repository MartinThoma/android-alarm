package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Plays an alarm sound in a loop on the alarm stream, so it passes Do Not Disturb when alarms
 * are allowed. Falls back to the bundled chime if the chosen sound cannot play (requirements AL-17).
 * Call from the main thread. Each user (the ringing service, a preview) gets its own player.
 */
class AlarmPlayer
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val customSounds: CustomSounds,
    ) {
        private val audioManager = context.getSystemService(AudioManager::class.java)
        private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(ATTRIBUTES).build()
        private var player: MediaPlayer? = null

        /** Plays [soundUri], or the bundled chime if it is `null` or cannot play. */
        fun play(soundUri: String?) {
            stop()
            audioManager.requestAudioFocus(focusRequest)
            val chosen = prepare(soundUri)
            if (chosen != null) start(chosen, onError = ::playFallback) else playFallback()
        }

        fun stop() {
            player?.run {
                stop()
                release()
            }
            player = null
            audioManager.abandonAudioFocusRequest(focusRequest)
        }

        private fun prepare(soundUri: String?): MediaPlayer? {
            AlarmSounds.builtIn(soundUri)?.let { return prepareRaw(it.raw) }
            AlarmSounds.custom(soundUri)?.let { sound ->
                val file = customSounds.file(sound) ?: return null
                return prepare { setDataSource(file.path) }
            }
            return prepare { setDataSource(context, checkNotNull(soundUri).toUri()) }
        }

        private fun prepareRaw(raw: Int) = prepare { context.resources.openRawResourceFd(raw).use { setDataSource(it) } }

        private fun playFallback() {
            val fallback = prepareRaw(AlarmSounds.Default.raw)
            if (fallback != null) start(fallback, onError = {}) else Log.e(TAG, "The bundled sound cannot play")
        }

        private fun start(
            mediaPlayer: MediaPlayer,
            onError: () -> Unit,
        ) {
            player = mediaPlayer
            mediaPlayer.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Playback failed ($what, $extra)")
                failed.release()
                if (player === failed) {
                    player = null
                    onError()
                }
                true
            }
            mediaPlayer.start()
        }

        private fun prepare(setSource: MediaPlayer.() -> Unit): MediaPlayer? {
            val mediaPlayer = MediaPlayer()
            return try {
                mediaPlayer.apply {
                    setAudioAttributes(ATTRIBUTES)
                    setSource()
                    isLooping = true
                    prepare()
                }
            } catch (e: Exception) {
                // A missing file, a lost permission or an unreadable format: all mean "use the fallback".
                Log.w(TAG, "Cannot play the alarm sound", e)
                mediaPlayer.release()
                null
            }
        }

        private companion object {
            val ATTRIBUTES: AudioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
        }
    }
