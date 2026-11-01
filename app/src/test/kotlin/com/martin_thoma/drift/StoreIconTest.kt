package com.martin_thoma.drift

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the store icon (512 × 512, as F-Droid and Google Play want it) from the app's own adaptive
 * icon, so the listing never shows an outdated one. Run with ./gradlew :app:recordRoborazziDebug.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class StoreIconTest {
    @Test
    fun `store icon`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // An adaptive icon layer is 108 dp; launchers show its middle 72 dp. Show a little more than that.
        val layer = (size * 108f / 80f).toInt()
        val offset = (size - layer) / 2
        for (drawable in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            context.getDrawable(drawable)!!.apply {
                setBounds(offset, offset, offset + layer, offset + layer)
                draw(canvas)
            }
        }
        if (System.getProperty("roborazzi.test.record") == "true") {
            val target = File("../fastlane/metadata/android/en-US/images/icon.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertEquals(size, bitmap.width)
    }
}
