package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val uriString: String,
    val artworkUrl: String? = null,
    val isVoiceMp3: Boolean = false,
    val isOnline: Boolean = false,
    val fileSize: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_recordings")
data class VoiceRecording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val dateRecorded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0
)

@Entity(tableName = "ducking_settings")
data class DuckingSettings(
    @PrimaryKey
    val id: Int = 1,
    val isDuckingEnabled: Boolean = true,
    val duckingLevelPercent: Int = 10, // Background music drops to 30%
    val fadeDownSpeedMs: Long = 300L,
    val fadeUpSpeedMs: Long = 600L,
    val musicVolumePercent: Int = 100,
    val voiceMicVolumePercent: Int = 100
)

enum class SearchCategory(val displayName: String) {
    SONG("Song"),
    MOVIE("Movie"),
    ARTIST("Artist")
}

enum class VoiceEffectType(val displayName: String, val description: String) {
    ORIGINAL_VOICE("Original Voice", "Direct clean audio pass-through without effects"),
    HIGH_ECHO("High Echo", "Strong reverberant echo with extended decay"),
    MEDIUM_ECHO("Medium Echo", "Balanced studio echo with moderate repeat"),
    LOW_ECHO("Low Echo", "Subtle ambient echo for natural warmth"),
    CHILD_VOICE("Child Voice", "Real-time child-like pitch transposition")
}

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class VoiceSourceType {
    LIVE_MIC,
    VOICE_MP3,
    SAVED_RECORDING,
    PREVIEW_TEST
}
