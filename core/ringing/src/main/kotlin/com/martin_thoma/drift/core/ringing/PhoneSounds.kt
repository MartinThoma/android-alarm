package com.martin_thoma.drift.core.ringing

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A sound that came with the phone. An alarm stores its [uri]. */
data class PhoneSound(
    val uri: String,
    val title: String,
    val kind: Kind,
) {
    enum class Kind { ALARM, RINGTONE }
}

/** Where the phone's own sounds come from; tests replace it. */
fun interface PhoneSoundSource {
    /** The phone's alarm tones and ringtones, each by title. Slow: call off the main thread. */
    fun load(): List<PhoneSound>
}

/** The tones in the phone's system storage, as the phone's own sound settings list them. */
class MediaStorePhoneSounds
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PhoneSoundSource {
        override fun load(): List<PhoneSound> =
            listOf(
                PhoneSound.Kind.ALARM to MediaStore.Audio.Media.IS_ALARM,
                PhoneSound.Kind.RINGTONE to MediaStore.Audio.Media.IS_RINGTONE,
            ).flatMap { (kind, column) -> query(kind, column) }
                // A tone that is both counts as an alarm tone.
                .distinctBy { it.uri }

        private fun query(
            kind: PhoneSound.Kind,
            column: String,
        ): List<PhoneSound> {
            val collection = MediaStore.Audio.Media.INTERNAL_CONTENT_URI
            return try {
                context.contentResolver
                    .query(
                        collection,
                        arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE),
                        "$column != 0",
                        null,
                        MediaStore.Audio.Media.TITLE,
                    )?.use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                val uri = ContentUris.withAppendedId(collection, cursor.getLong(0)).toString()
                                add(PhoneSound(uri, cursor.getString(1).orEmpty(), kind))
                            }
                        }
                    }.orEmpty()
            } catch (e: Exception) {
                // No media storage (before the first unlock, or on a stripped-down phone): no phone sounds.
                Log.w(TAG, "Cannot list the phone's sounds", e)
                emptyList()
            }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
interface PhoneSoundsModule {
    @Binds
    fun source(source: MediaStorePhoneSounds): PhoneSoundSource
}

/**
 * The phone's own alarm tones and ringtones (requirements AL-04). The phone's storage cannot be
 * read before the first unlock after a reboot, so a chosen tone is copied into the app, as the
 * phone's own clock does with its alarm sound.
 */
@Singleton
class PhoneSounds
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val source: PhoneSoundSource,
    ) {
        // Device-protected, so the copies are readable before the first unlock.
        private val directory = File(context.createDeviceProtectedStorageContext().filesDir, "phone-sounds")
        private val mutableSounds = MutableStateFlow<List<PhoneSound>>(emptyList())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        init {
            scope.launch { mutableSounds.value = source.load() }
        }

        /** The phone's tones, alarm tones first, each kind by title. Empty until they are loaded. */
        val sounds: StateFlow<List<PhoneSound>> = mutableSounds.asStateFlow()

        /** The app's copy of the phone's tone [soundUri], or `null` if there is none. */
        fun file(soundUri: String): File? = File(directory, fileName(soundUri)).takeIf { it.isFile && it.length() > 0 }

        /** Copies the phone's tone [soundUri] into the app, unless it is there already. */
        suspend fun keep(soundUri: String) =
            withContext(Dispatchers.IO) {
                if (!isPhoneSound(soundUri) || file(soundUri) != null) return@withContext
                directory.mkdirs()
                val target = File(directory, fileName(soundUri))
                try {
                    val input = context.contentResolver.openInputStream(soundUri.toUri()) ?: return@withContext
                    input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                } catch (e: Exception) {
                    // The tone still plays from the phone once it is unlocked.
                    Log.w(TAG, "Cannot copy $soundUri", e)
                }
                if (target.length() == 0L) target.delete()
            }

        /** Deletes the copies of all tones but those in [inUse]. */
        suspend fun forgetAllBut(inUse: Collection<String?>) =
            withContext(Dispatchers.IO) {
                val keep =
                    inUse
                        .filterNotNull()
                        .filter(::isPhoneSound)
                        .map(::fileName)
                        .toSet()
                directory
                    .listFiles()
                    .orEmpty()
                    .filter { it.name !in keep }
                    .forEach { it.delete() }
            }

        private fun fileName(soundUri: String) = UUID.nameUUIDFromBytes(soundUri.toByteArray()).toString()

        companion object {
            /** Whether an alarm with [soundUri] rings with one of the phone's own tones. */
            fun isPhoneSound(soundUri: String?): Boolean = soundUri?.startsWith("content://") == true
        }
    }
