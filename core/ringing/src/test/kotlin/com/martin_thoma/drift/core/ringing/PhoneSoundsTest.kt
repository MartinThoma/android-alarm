package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneSoundsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tone = "content://media/internal/audio/media/7"
    private val other = "content://media/internal/audio/media/8"

    private fun phoneHas(
        uri: String,
        bytes: Int,
    ) = shadowOf(context.contentResolver).registerInputStream(Uri.parse(uri), ByteArrayInputStream(ByteArray(bytes) { it.toByte() }))

    @Test
    fun `a chosen tone is copied, so it plays before the first unlock`() {
        phoneHas(tone, 4096)
        val sounds = PhoneSounds(context) { emptyList() }

        runBlocking { sounds.keep(tone) }

        assertEquals(4096L, sounds.file(tone)?.length())
        assertTrue(
            sounds
                .file(tone)
                ?.path
                .orEmpty()
                .contains(context.createDeviceProtectedStorageContext().filesDir.path),
        )
    }

    @Test
    fun `copies of tones no alarm uses any more are deleted`() {
        phoneHas(tone, 100)
        phoneHas(other, 100)
        val sounds = PhoneSounds(context) { emptyList() }
        runBlocking {
            sounds.keep(tone)
            sounds.keep(other)
            sounds.forgetAllBut(listOf(null, AlarmSounds.SILENT_URI, other))
        }

        assertNull(sounds.file(tone))
        assertEquals(100L, sounds.file(other)?.length())
    }

    @Test
    fun `only the phone's tones count as phone sounds`() {
        assertTrue(PhoneSounds.isPhoneSound(tone))
        assertFalse(PhoneSounds.isPhoneSound(null))
        assertFalse(PhoneSounds.isPhoneSound(AlarmSounds.Default.uri))
        assertFalse(PhoneSounds.isPhoneSound(CustomSound("a.mp3").uri))
    }
}
