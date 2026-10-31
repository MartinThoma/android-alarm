package com.martin_thoma.drift.core.ringing

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** What came of adding a sound file. */
sealed interface ImportResult {
    data class Added(
        val sound: CustomSound,
    ) : ImportResult

    data object TooLarge : ImportResult

    /** Not readable, or not audio that Android can play. */
    data object NotPlayable : ImportResult
}

/**
 * Sound files the user added (MP3, WAV, OGG and anything else Android plays). Each one is
 * copied into the app, so the alarm still rings if the original is moved or deleted, and
 * before the first unlock after a reboot (requirements 6.2 and 9).
 */
@Singleton
class CustomSounds
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        // Device-protected, so the copies are readable before the first unlock.
        private val directory = File(context.createDeviceProtectedStorageContext().filesDir, "sounds")
        private val mutableSounds = MutableStateFlow(load())

        /** The user's sounds, by name. */
        val sounds: StateFlow<List<CustomSound>> = mutableSounds.asStateFlow()

        /** The copy of [sound], or `null` if it is gone. */
        fun file(sound: CustomSound): File? = File(directory, sound.fileName).takeIf { it.isFile && it.parentFile == directory }

        suspend fun add(source: Uri): ImportResult =
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val (displayName, size) = describe(source)
                if (size != null && size > MAX_BYTES) return@withContext ImportResult.TooLarge
                directory.mkdirs()
                val target = freeFile(fileName(displayName, resolver.getType(source)))
                try {
                    val input = resolver.openInputStream(source) ?: return@withContext ImportResult.NotPlayable
                    val copied = input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                    if (copied > MAX_BYTES) {
                        target.delete()
                        return@withContext ImportResult.TooLarge
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Cannot copy $source", e)
                    target.delete()
                    return@withContext ImportResult.NotPlayable
                } catch (e: SecurityException) {
                    Log.w(TAG, "No access to $source", e)
                    target.delete()
                    return@withContext ImportResult.NotPlayable
                }
                if (!playable(target)) {
                    target.delete()
                    return@withContext ImportResult.NotPlayable
                }
                mutableSounds.value = load()
                ImportResult.Added(CustomSound(target.name))
            }

        suspend fun delete(sound: CustomSound) =
            withContext(Dispatchers.IO) {
                file(sound)?.delete()
                mutableSounds.value = load()
            }

        private fun load(): List<CustomSound> =
            directory
                .listFiles()
                .orEmpty()
                .filter { it.isFile }
                .map { CustomSound(it.name) }
                .sortedBy { it.title.lowercase() }

        private fun describe(source: Uri): Pair<String?, Long?> =
            context.contentResolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
                if (!it.moveToFirst()) return@use null to null
                val name = it.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { i -> i >= 0 && !it.isNull(i) }?.let(it::getString)
                val size = it.getColumnIndex(OpenableColumns.SIZE).takeIf { i -> i >= 0 && !it.isNull(i) }?.let(it::getLong)
                name to size
            } ?: (source.lastPathSegment to null)

        private fun freeFile(name: String): File {
            val base = name.substringBeforeLast('.')
            val extension = name.substringAfterLast('.', "")
            return generateSequence(1) { it + 1 }
                .map { n -> if (n == 1) name else "$base ($n)${if (extension.isEmpty()) "" else ".$extension"}" }
                .map { File(directory, it) }
                .first { !it.exists() }
        }

        private fun playable(file: File): Boolean {
            val player = MediaPlayer()
            return try {
                player.setDataSource(file.path)
                player.prepare()
                true
            } catch (e: Exception) {
                // Whatever the reason, Android cannot play it, so it cannot wake anyone.
                Log.w(TAG, "${file.name} is not playable", e)
                false
            } finally {
                player.release()
            }
        }

        internal companion object {
            const val MAX_BYTES = 30L * 1024 * 1024

            /** A safe file name from the user's file name, keeping or adding its extension. */
            fun fileName(
                displayName: String?,
                mimeType: String?,
            ): String {
                val name =
                    displayName
                        .orEmpty()
                        .substringAfterLast('/')
                        .replace(Regex("[^\\p{L}\\p{N} ._()-]"), "_")
                        .trim('.', ' ')
                val base = name.ifEmpty { "Sound" }.take(80)
                if (base.contains('.')) return base
                val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                return if (extension == null) base else "$base.$extension"
            }
        }
    }
