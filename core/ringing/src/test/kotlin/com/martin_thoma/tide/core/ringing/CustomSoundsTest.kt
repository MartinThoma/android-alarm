package com.martin_thoma.tide.core.ringing

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSoundsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun userFile(name: String): Uri = Uri.fromFile(folder.newFile(name).apply { writeBytes(ByteArray(2048) { it.toByte() }) })

    @Test
    fun `an added file is copied into the app and listed`() {
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
        val sounds = CustomSounds(context)

        val result = runBlocking { sounds.add(userFile("Morning birds.mp3")) }

        val added = (result as ImportResult.Added).sound
        assertEquals("Morning birds", added.title)
        assertEquals(listOf(added), sounds.sounds.value)
        assertEquals(2048L, sounds.file(added)?.length())
    }

    @Test
    fun `the same name twice gets a number`() {
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> ShadowMediaPlayer.MediaInfo() }
        val sounds = CustomSounds(context)
        runBlocking { sounds.add(userFile("Song.mp3")) }

        val second =
            runBlocking { sounds.add(Uri.fromFile(folder.newFolder("other").resolve("Song.mp3").apply { writeBytes(ByteArray(10)) })) }

        assertEquals("Song (2)", (second as ImportResult.Added).sound.title)
    }

    @Test
    fun `a file Android cannot play is refused and not kept`() {
        // Without media info, Robolectric's player fails like a real one on a non-audio file.
        ShadowMediaPlayer.setMediaInfoProvider { _: DataSource -> null }
        val sounds = CustomSounds(context)

        val result = runBlocking { sounds.add(userFile("notes.txt")) }

        assertEquals(ImportResult.NotPlayable, result)
        assertTrue(sounds.sounds.value.isEmpty())
    }
}
