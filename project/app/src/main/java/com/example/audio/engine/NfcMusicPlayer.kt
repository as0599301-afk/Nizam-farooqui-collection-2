package com.example.audio.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.RepeatMode
import com.example.data.model.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NfcMusicPlayer(
    private val context: Context,
    private val duckingEngine: AudioDuckingEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = initExoPlayer()
            }
            return _exoPlayer!!
        }

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<SongItem>>(emptyList())
    val queue: StateFlow<List<SongItem>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var progressTrackerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            _isPlaying.value = isPlayingNow
            if (isPlayingNow) {
                startProgressTracker()
            } else {
                progressTrackerJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _duration.value = _exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    _playbackPosition.value = _exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                }
                Player.STATE_ENDED -> {
                    handleSongCompleted()
                }
                Player.STATE_IDLE -> {
                    _isPlaying.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "Playback error: ${error.localizedMessage ?: "Cannot load audio stream"}"
            _isPlaying.value = false
        }
    }

    private fun initExoPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(context.applicationContext).build()
        duckingEngine.onVolumeChangedListener = { effectiveVolume ->
            _exoPlayer?.volume = effectiveVolume
        }
        player.volume = duckingEngine.effectiveMusicVolume.value
        player.addListener(playerListener)
        return player
    }

    init {
        // Wire up ducking engine volume callback
        duckingEngine.onVolumeChangedListener = { effectiveVolume ->
            _exoPlayer?.volume = effectiveVolume
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (isActive) {
                val player = _exoPlayer
                if (player != null && player.isPlaying) {
                    _playbackPosition.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0L) {
                        _duration.value = dur
                    }
                }
                delay(250)
            }
        }
    }

    fun playSong(song: SongItem) {
        val currentQ = _queue.value.toMutableList()
        val existingIndex = currentQ.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _currentQueueIndex.value = existingIndex
        } else {
            currentQ.add(song)
            _queue.value = currentQ
            _currentQueueIndex.value = currentQ.lastIndex
        }
        startPlaybackInternal(song)
    }

    fun playQueue(songs: List<SongItem>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val validIndex = startIndex.coerceIn(0, songs.lastIndex)
        _currentQueueIndex.value = validIndex
        startPlaybackInternal(songs[validIndex])
    }

    fun addToQueue(song: SongItem) {
        val updated = _queue.value.toMutableList().apply { add(song) }
        _queue.value = updated
        if (_currentSong.value == null) {
            playQueue(updated, 0)
        }
    }

    fun removeFromQueue(index: Int) {
        val currentQ = _queue.value.toMutableList()
        if (index in currentQ.indices) {
            currentQ.removeAt(index)
            _queue.value = currentQ
            if (index == _currentQueueIndex.value) {
                if (currentQ.isNotEmpty()) {
                    val nextIdx = index.coerceIn(0, currentQ.lastIndex)
                    _currentQueueIndex.value = nextIdx
                    startPlaybackInternal(currentQ[nextIdx])
                } else {
                    stop()
                }
            } else if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value = _currentQueueIndex.value - 1
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentQueueIndex.value = -1
        stop()
    }

    private fun startPlaybackInternal(song: SongItem) {
        _errorMessage.value = null
        _currentSong.value = song
        _playbackPosition.value = 0L
        _duration.value = song.durationMs

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUrl?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(song.uriString))
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.volume = duckingEngine.effectiveMusicVolume.value
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
    }

    fun resume() {
        if (_currentSong.value != null) {
            exoPlayer.play()
            _isPlaying.value = true
        } else if (_queue.value.isNotEmpty()) {
            val idx = _currentQueueIndex.value.coerceAtLeast(0)
            startPlaybackInternal(_queue.value[idx])
        }
    }

    fun stop() {
        exoPlayer.stop()
        _isPlaying.value = false
        _playbackPosition.value = 0L
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(0L))
        exoPlayer.seekTo(clamped)
        _playbackPosition.value = clamped
    }

    fun next() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        val nextIndex = _currentQueueIndex.value + 1
        if (nextIndex < currentQ.size) {
            _currentQueueIndex.value = nextIndex
            startPlaybackInternal(currentQ[nextIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _currentQueueIndex.value = 0
            startPlaybackInternal(currentQ[0])
        }
    }

    fun previous() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        // If played more than 3 seconds, replay current song first
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = _currentQueueIndex.value - 1
        if (prevIndex >= 0) {
            _currentQueueIndex.value = prevIndex
            startPlaybackInternal(currentQ[prevIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            val lastIdx = currentQ.lastIndex
            _currentQueueIndex.value = lastIdx
            startPlaybackInternal(currentQ[lastIdx])
        } else {
            seekTo(0L)
        }
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
    }

    private fun handleSongCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                val nextIndex = _currentQueueIndex.value + 1
                if (nextIndex < _queue.value.size) {
                    next()
                } else {
                    stop()
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun release() {
        progressTrackerJob?.cancel()
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
