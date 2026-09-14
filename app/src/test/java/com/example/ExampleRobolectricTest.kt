package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.engine.AudioDuckingEngine
import com.example.data.model.DuckingSettings
import com.example.data.model.VoiceSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches NFC app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nizam Farooqui Collection 4", appName)
    }

    @Test
    fun `ducking engine ducks when voice source active`() {
        val engine = AudioDuckingEngine()
        engine.updateSettings(
            DuckingSettings(
                isDuckingEnabled = true,
                duckingLevelPercent = 30,
                fadeDownSpeedMs = 50L,
                fadeUpSpeedMs = 50L
            )
        )

        // Initially not ducked
        assertTrue(!engine.isDucked.value)

        // Mic starts
        engine.onVoiceSourceStarted(VoiceSourceType.LIVE_MIC)
        assertTrue(engine.isDucked.value)
        assertTrue(engine.isSourceActive(VoiceSourceType.LIVE_MIC))

        // Multiple voice sources (Mic + Voice MP3)
        engine.onVoiceSourceStarted(VoiceSourceType.VOICE_MP3)
        assertTrue(engine.isDucked.value)

        // Stop Mic, Voice MP3 is still playing -> must remain ducked!
        engine.onVoiceSourceStopped(VoiceSourceType.LIVE_MIC)
        assertTrue(engine.isDucked.value)

        // Stop Voice MP3 -> all voice sources stopped -> ducking turns off
        engine.onVoiceSourceStopped(VoiceSourceType.VOICE_MP3)
        assertTrue(!engine.isDucked.value)
    }
}
