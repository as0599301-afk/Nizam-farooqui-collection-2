package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NfcApplication
import com.example.audio.mic.LiveMicEngine
import com.example.audio.recording.RecordingState
import com.example.data.model.DuckingSettings
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.SearchCategory
import com.example.data.model.SongItem
import com.example.data.model.VoiceEffectType
import com.example.data.model.VoiceRecording
import com.example.data.model.VoiceSourceType
import com.example.service.NfcPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NfcScreen(val title: String, val shortLabel: String) {
    MUSIC_PLAYER("Music Player", "Player"),
    ONLINE_SEARCH("Online Search", "Search"),
    LOCAL_LIBRARY("Local MP3 Library", "Library"),
    LIVE_MIC("Live Microphone", "Live Mic"),
    VOICE_EFFECTS("Voice Effects", "Effects"),
    VOICE_RECORDINGS("Voice Recordings", "Recordings"),
    DUCKING_SETTINGS("Ducking Settings", "Ducking"),
    APP_SETTINGS("App Settings", "Settings")
}

class NfcViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as? NfcApplication
        ?: (application.applicationContext as? NfcApplication)
        ?: error("Application must be an instance of NfcApplication")
    private val musicPlayer = app.musicPlayer
    private val voicePlayer = app.voicePlaybackPlayer
    private val duckingEngine = app.duckingEngine
    private val liveMicEngine = app.liveMicEngine
    private val recordingManager = app.voiceRecordingManager
    private val importer = app.audioFileImporter
    private val repository = app.repository
    private val providerRegistry = app.providerRegistry

    // Active screen navigation
    private val _currentScreen = MutableStateFlow(NfcScreen.MUSIC_PLAYER)
    val currentScreen: StateFlow<NfcScreen> = _currentScreen.asStateFlow()

    // Music Player state
    val currentSong: StateFlow<SongItem?> = musicPlayer.currentSong
    val isPlaying: StateFlow<Boolean> = musicPlayer.isPlaying
    val playbackPosition: StateFlow<Long> = musicPlayer.playbackPosition
    val duration: StateFlow<Long> = musicPlayer.duration
    val repeatMode: StateFlow<RepeatMode> = musicPlayer.repeatMode
    val queue: StateFlow<List<SongItem>> = musicPlayer.queue
    val currentQueueIndex: StateFlow<Int> = musicPlayer.currentQueueIndex
    val playerError: StateFlow<String?> = musicPlayer.errorMessage

    // Ducking state
    val isDucked: StateFlow<Boolean> = duckingEngine.isDucked
    val activeVoiceSources: StateFlow<Set<VoiceSourceType>> = duckingEngine.activeVoiceSources
    val currentDuckingFactor: StateFlow<Float> = duckingEngine.currentDuckingFactor
    val effectiveMusicVolume: StateFlow<Float> = duckingEngine.effectiveMusicVolume

    val duckingSettings: StateFlow<DuckingSettings?> = repository.duckingSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DuckingSettings()
    )

    // Voice Playback Player state (Imported Voice MP3s & Saved Recordings)
    val voiceIsPlaying: StateFlow<Boolean> = voicePlayer.isPlaying
    val activeVoiceId: StateFlow<String?> = voicePlayer.activeVoiceId
    val currentVoiceTitle: StateFlow<String> = voicePlayer.currentVoiceTitle
    val voicePosition: StateFlow<Long> = voicePlayer.playbackPosition
    val voiceDuration: StateFlow<Long> = voicePlayer.duration

    // Live Mic state
    val isMicActive: StateFlow<Boolean> = liveMicEngine.isMicActive
    val audioMeterLevel: StateFlow<Float> = liveMicEngine.audioMeterLevel
    val currentVoiceEffect: StateFlow<VoiceEffectType> = liveMicEngine.currentEffect
    val micVolume: StateFlow<Int> = liveMicEngine.micVolume
    val micError: StateFlow<String?> = liveMicEngine.errorMessage

    // Voice Recording state
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    val recordingDurationMs: StateFlow<Long> = recordingManager.recordingDurationMs
    val recordingError: StateFlow<String?> = recordingManager.recordingError

    // Local Library data
    val localMusicSongs: StateFlow<List<SongItem>> = repository.musicSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val voiceMp3Songs: StateFlow<List<SongItem>> = repository.voiceMp3Songs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val savedRecordings: StateFlow<List<VoiceRecording>> = repository.voiceRecordings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Online Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(SearchCategory.SONG)
    val searchCategory: StateFlow<SearchCategory> = _searchCategory.asStateFlow()

    private val _searchLanguage = MutableStateFlow("All")
    val searchLanguage: StateFlow<String> = _searchLanguage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SongItem>>(emptyList())
    val searchResults: StateFlow<List<SongItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    val activeMusicProvider = providerRegistry.activeProvider

    // Generic UI notifications/snackbars
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var searchJob: Job? = null

    fun navigateTo(screen: NfcScreen) {
        _currentScreen.value = screen
    }

    // --- Music Player Actions ---
    fun playSong(song: SongItem) {
        musicPlayer.playSong(song)
        NfcPlaybackService.startService(app)
    }

    fun playQueue(songs: List<SongItem>, startIndex: Int = 0) {
        musicPlayer.playQueue(songs, startIndex)
        NfcPlaybackService.startService(app)
    }

    fun addToQueue(song: SongItem) {
        musicPlayer.addToQueue(song)
        _userMessage.value = "Added \"${song.title}\" to queue"
    }

    fun removeFromQueue(index: Int) {
        musicPlayer.removeFromQueue(index)
    }

    fun clearQueue() {
        musicPlayer.clearQueue()
    }

    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
        NfcPlaybackService.startService(app)
    }

    fun pauseMusic() {
        musicPlayer.pause()
    }

    fun resumeMusic() {
        musicPlayer.resume()
        NfcPlaybackService.startService(app)
    }

    fun stopMusic() {
        musicPlayer.stop()
        NfcPlaybackService.stopService(app)
    }

    fun seekMusicTo(positionMs: Long) {
        musicPlayer.seekTo(positionMs)
    }

    fun nextTrack() {
        musicPlayer.next()
    }

    fun previousTrack() {
        musicPlayer.previous()
    }

    fun toggleRepeatMode() {
        musicPlayer.toggleRepeatMode()
    }

    // --- Voice Playback Player Actions (Imported Voice MP3 & Saved Recordings) ---
    fun playVoiceMp3(song: SongItem) {
        voicePlayer.playVoiceMp3(song)
    }

    fun playSavedRecording(recording: VoiceRecording) {
        voicePlayer.playSavedRecording(recording)
    }

    fun toggleVoicePlayPause() {
        voicePlayer.togglePlayPause()
    }

    fun stopVoicePlayback() {
        voicePlayer.stop()
    }

    fun seekVoiceTo(positionMs: Long) {
        voicePlayer.seekTo(positionMs)
    }

    // --- Live Microphone & Voice Effects Actions ---
    fun toggleMic(): Boolean {
        return if (liveMicEngine.isMicActive.value) {
            liveMicEngine.stopMic()
            true
        } else {
            liveMicEngine.startMic()
        }
    }

    fun setVoiceEffect(effect: VoiceEffectType) {
        liveMicEngine.setEffect(effect)
    }

    fun setMicVolume(volumePercent: Int) {
        liveMicEngine.setMicVolume(volumePercent)
        // Also update settings in db
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            repository.updateDuckingSettings(current.copy(voiceMicVolumePercent = volumePercent))
        }
    }

    // --- Voice Recording Actions ---
    fun startRecording(): Boolean {
        return recordingManager.startRecording()
    }

    fun pauseRecording() {
        recordingManager.pauseRecording()
    }

    fun resumeRecording() {
        recordingManager.resumeRecording()
    }

    fun stopAndSaveRecording(title: String? = null) {
        viewModelScope.launch {
            val saved = recordingManager.stopAndSaveRecording(title)
            if (saved != null) {
                _userMessage.value = "Voice recording saved: ${saved.title}"
            }
        }
    }

    // --- Local MP3 Library Actions ---
    fun importAudioFile(uri: Uri, isVoiceMp3: Boolean) {
        viewModelScope.launch {
            val result = importer.importAudioFile(uri, isVoiceMp3)
            result.onSuccess { song ->
                val label = if (isVoiceMp3) "Voice MP3" else "Music MP3"
                _userMessage.value = "Imported $label: \"${song.title}\""
            }.onFailure { err ->
                _userMessage.value = "Import failed: ${err.message}"
            }
        }
    }

    fun renameSong(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameSong(id, newTitle)
            _userMessage.value = "Renamed to \"$newTitle\""
        }
    }

    fun removeSongFromLibrary(songId: Long) {
        viewModelScope.launch {
            repository.removeFromLibrary(songId)
            _userMessage.value = "Removed from NFC library (Original phone file preserved)"
        }
    }

    fun deleteSongPermanently(song: SongItem) {
        viewModelScope.launch {
            repository.deletePermanently(song)
            _userMessage.value = "Permanently deleted \"${song.title}\""
        }
    }

    fun renameRecording(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameRecording(id, newTitle)
            _userMessage.value = "Recording renamed to \"$newTitle\""
        }
    }

    fun deleteRecording(recording: VoiceRecording) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
            _userMessage.value = "Deleted recording \"${recording.title}\""
        }
    }

    // --- Ducking Settings Actions ---
    fun updateDuckingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            val updated = current.copy(isDuckingEnabled = enabled)
            repository.updateDuckingSettings(updated)
            duckingEngine.updateSettings(updated)
        }
    }

    fun updateDuckingLevel(percent: Int) {
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            val updated = current.copy(duckingLevelPercent = percent.coerceIn(0, 100))
            repository.updateDuckingSettings(updated)
            duckingEngine.updateSettings(updated)
        }
    }

    fun updateFadeDownSpeed(ms: Long) {
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            val updated = current.copy(fadeDownSpeedMs = ms.coerceIn(50L, 3000L))
            repository.updateDuckingSettings(updated)
            duckingEngine.updateSettings(updated)
        }
    }

    fun updateFadeUpSpeed(ms: Long) {
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            val updated = current.copy(fadeUpSpeedMs = ms.coerceIn(50L, 3000L))
            repository.updateDuckingSettings(updated)
            duckingEngine.updateSettings(updated)
        }
    }

    fun updateMusicVolume(percent: Int) {
        viewModelScope.launch {
            val current = duckingSettings.value ?: DuckingSettings()
            val updated = current.copy(musicVolumePercent = percent.coerceIn(0, 100))
            repository.updateDuckingSettings(updated)
            duckingEngine.updateSettings(updated)
        }
    }

    fun triggerDuckingTestPreview() {
        duckingEngine.triggerTestPreview()
        _userMessage.value = "Ducking preview active for 3.5s..."
    }

    // --- Online Search Actions ---
    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onSearchCategoryChanged(cat: SearchCategory) {
        _searchCategory.value = cat
        if (_searchQuery.value.isNotBlank()) {
            executeSearch()
        }
    }

    fun onSearchLanguageChanged(lang: String) {
        _searchLanguage.value = lang
        if (_searchQuery.value.isNotBlank()) {
            executeSearch()
        }
    }

    fun executeSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null

            val provider = providerRegistry.activeProvider.value
            val result = provider.search(
                query = query,
                category = _searchCategory.value,
                language = _searchLanguage.value
            )

            result.onSuccess { list ->
                _searchResults.value = list
                if (list.isEmpty()) {
                    _searchError.value = "No legal music matches found for \"$query\""
                }
            }.onFailure { err ->
                _searchError.value = err.localizedMessage ?: "Failed to connect to music search provider"
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    fun selectProvider(providerId: String) {
        providerRegistry.setActiveProvider(providerId)
        _userMessage.value = "Provider switched to ${providerRegistry.activeProvider.value.displayName}"
        if (_searchQuery.value.isNotBlank()) {
            executeSearch()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearErrors() {
        musicPlayer.clearError()
        liveMicEngine.clearError()
        recordingManager.clearError()
        _searchError.value = null
    }
}
