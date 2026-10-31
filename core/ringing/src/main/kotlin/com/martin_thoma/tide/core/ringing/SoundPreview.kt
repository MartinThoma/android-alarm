package com.martin_thoma.tide.core.ringing

import javax.inject.Inject

/**
 * Plays a sound exactly as it will ring: on the alarm stream at the alarm's own volume
 * (requirements AL-06). Never while an alarm rings, and never changes a ringing alarm's volume.
 */
class SoundPreview
    @Inject
    constructor(
        private val player: AlarmPlayer,
        private val volume: AlarmVolume,
        private val ringing: RingingState,
    ) {
        fun play(
            soundUri: String?,
            volumePercent: Int,
        ) {
            if (ringing.status.value != RingingStatus.Idle) return
            volume.apply(volumePercent)
            player.play(soundUri)
        }

        fun stop() {
            player.stop()
            if (ringing.status.value == RingingStatus.Idle) volume.restore()
        }
    }
