package com.example

import android.app.Application
import com.example.audio.engine.AudioDuckingEngine
import com.example.audio.engine.NfcMusicPlayer
import com.example.audio.engine.VoicePlaybackPlayer
import com.example.audio.importer.AudioFileImporter
import com.example.audio.importer.BundledMp3Seeder
import com.example.audio.mic.LiveMicEngine
import com.example.audio.provider.MusicProviderRegistry
import com.example.audio.provider.YouTubeMusicProvider
import com.example.audio.recording.VoiceRecordingManager
import com.example.data.db.NfcDatabase
import com.example.data.repository.NfcRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NfcApplication : Application() {

    val database: NfcDatabase by lazy { NfcDatabase.getInstance(this) }
    val repository: NfcRepository by lazy { NfcRepository(database.nfcDao(), this) }
    val duckingEngine: AudioDuckingEngine by lazy { AudioDuckingEngine() }
    val musicPlayer: NfcMusicPlayer by lazy { NfcMusicPlayer(this, duckingEngine) }
    val voicePlaybackPlayer: VoicePlaybackPlayer by lazy { VoicePlaybackPlayer(this, duckingEngine) }
    val liveMicEngine: LiveMicEngine by lazy { LiveMicEngine(this, duckingEngine) }
    val voiceRecordingManager: VoiceRecordingManager by lazy { VoiceRecordingManager(this, repository, liveMicEngine) }
    val audioFileImporter: AudioFileImporter by lazy { AudioFileImporter(this, repository) }
    val providerRegistry: MusicProviderRegistry by lazy { MusicProviderRegistry().apply { registerProvider(YouTubeMusicProvider()) } }

    override fun onCreate() {
        super.onCreate()

        // Sync stored ducking settings into AudioDuckingEngine asynchronously without blocking startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.getDuckingSettingsSync()
                duckingEngine.updateSettings(settings)
                BundledMp3Seeder.seed(this@NfcApplication, repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            liveMicEngine.release()
            musicPlayer.release()
            voicePlaybackPlayer.release()
        } catch (_: Exception) {}
    }
}
