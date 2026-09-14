package com.example.audio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.SongItem
import com.example.data.model.VoiceRecording
import com.example.data.model.VoiceSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoicePlaybackPlayer(
    private val context: Context,
    private val duckingEngine: AudioDuckingEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSourceType: VoiceSourceType? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeVoiceId = MutableStateFlow<String?>(null)
    val activeVoiceId: StateFlow<String?> = _activeVoiceId.asStateFlow()

    private val _currentVoiceTitle = MutableStateFlow("")
    val currentVoiceTitle: StateFlow<String> = _currentVoiceTitle.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var progressJob: Job? = null

    fun playVoiceMp3(song: SongItem) {
        startPlayback(
            uri = Uri.parse(song.uriString),
            title = song.title,
            voiceId = "mp3_${song.id}",
            sourceType = VoiceSourceType.VOICE_MP3
        )
    }

    fun playSavedRecording(recording: VoiceRecording) {
        startPlayback(
            uri = Uri.parse(recording.filePath),
            title = recording.title,
            voiceId = "rec_${recording.id}",
            sourceType = VoiceSourceType.SAVED_RECORDING
        )
    }

    private fun startPlayback(
        uri: Uri,
        title: String,
        voiceId: String,
        sourceType: VoiceSourceType
    ) {
        stop()

        try {
            _activeVoiceId.value = voiceId
            _currentVoiceTitle.value = title
            currentSourceType = sourceType

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                val vol = (duckingEngine.getSettings().voiceMicVolumePercent.coerceIn(0, 100)) / 100f
                setVolume(vol, vol)
                setOnCompletionListener {
                    handlePlaybackEnded()
                }
                setOnErrorListener { _, _, _ ->
                    handlePlaybackEnded()
                    true
                }
            }

            mediaPlayer = mp
            _duration.value = mp.duration.coerceAtLeast(0).toLong()

            // Trigger real ducking engine
            duckingEngine.onVoiceSourceStarted(sourceType)
            mp.start()
            _isPlaying.value = true
            startProgressTracker()

        } catch (e: Exception) {
            e.printStackTrace()
            handlePlaybackEnded()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                currentSourceType?.let { duckingEngine.onVoiceSourceStopped(it) }
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { mp ->
            val vol = (duckingEngine.getSettings().voiceMicVolumePercent.coerceIn(0, 100)) / 100f
            mp.setVolume(vol, vol)
            currentSourceType?.let { duckingEngine.onVoiceSourceStarted(it) }
            mp.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        currentSourceType?.let { duckingEngine.onVoiceSourceStopped(it) }
        currentSourceType = null
        _isPlaying.value = false
        _activeVoiceId.value = null
        _playbackPosition.value = 0L
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            val clamped = positionMs.coerceIn(0L, _duration.value)
            mp.seekTo(clamped.toInt())
            _playbackPosition.value = clamped
        }
    }

    private fun handlePlaybackEnded() {
        progressJob?.cancel()
        currentSourceType?.let { duckingEngine.onVoiceSourceStopped(it) }
        currentSourceType = null
        _isPlaying.value = false
        _activeVoiceId.value = null
        _playbackPosition.value = 0L
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playbackPosition.value = mp.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    fun release() {
        stop()
    }
}
