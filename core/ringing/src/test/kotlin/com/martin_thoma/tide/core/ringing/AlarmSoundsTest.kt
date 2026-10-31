package com.martin_thoma.tide.core.ringing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmSoundsTest {
    @Test
    fun `an alarm without a sound rings with the default`() {
        assertEquals(AlarmSounds.Default, AlarmSounds.builtIn(null))
    }

    @Test
    fun `every built-in sound is found by its uri, and the uris are unique`() {
        AlarmSounds.builtIn.forEach { assertEquals(it, AlarmSounds.builtIn(it.uri)) }
        assertEquals(
            AlarmSounds.builtIn.size,
            AlarmSounds.builtIn
                .map { it.uri }
                .toSet()
                .size,
        )
    }

    @Test
    fun `the requested groups are there`() {
        val byCategory = AlarmSounds.builtIn.groupBy { it.category }.mapValues { (_, sounds) -> sounds.map { it.id } }
        assertEquals(listOf("sea", "rain", "thunder", "crickets"), byCategory[SoundCategory.NATURE])
        assertEquals(listOf("alarm_clock"), byCategory[SoundCategory.NOISY])
        assertEquals(listOf("canon_in_d", "fur_elise"), byCategory[SoundCategory.CLASSICS])
    }

    @Test
    fun `custom sounds and other uris are told apart`() {
        assertEquals(CustomSound("Morning.mp3"), AlarmSounds.custom("tide:custom/Morning.mp3"))
        assertNull(AlarmSounds.builtIn("tide:custom/Morning.mp3"))
        assertNull(AlarmSounds.custom("content://media/external/audio/media/12"))
        assertNull(AlarmSounds.builtIn("tide:sound/removed-in-a-later-version"))
    }

    @Test
    fun `file names are made safe and keep an extension`() {
        assertEquals("Morning birds.mp3", CustomSounds.fileName("Morning birds.mp3", "audio/mpeg"))
        assertEquals("passwd.wav", CustomSounds.fileName("../../etc/passwd.wav", null))
        assertEquals("Sound", CustomSounds.fileName(null, null))
    }
}
