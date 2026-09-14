package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DuckingSettings
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongItem
import com.example.data.model.VoiceRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcDao {

    // Music Songs (Non-voice MP3s)
    @Query("SELECT * FROM songs WHERE isVoiceMp3 = 0 ORDER BY dateAdded DESC")
    fun getAllMusicSongs(): Flow<List<SongItem>>

    // Imported Voice MP3s
    @Query("SELECT * FROM songs WHERE isVoiceMp3 = 1 ORDER BY dateAdded DESC")
    fun getAllVoiceMp3Songs(): Flow<List<SongItem>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongItem): Long
    @Query("SELECT * FROM songs WHERE uriString = :uri LIMIT 1")
    suspend fun getSongByUri(uri: String): SongItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongItem>)

    @Update
    suspend fun updateSong(song: SongItem)

    @Delete
    suspend fun deleteSong(song: SongItem)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("UPDATE songs SET title = :newTitle WHERE id = :id")
    suspend fun renameSong(id: Long, newTitle: String)

    // Voice Recordings
    @Query("SELECT * FROM voice_recordings ORDER BY dateRecorded DESC")
    fun getAllVoiceRecordings(): Flow<List<VoiceRecording>>

    @Query("SELECT * FROM voice_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): VoiceRecording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: VoiceRecording): Long

    @Update
    suspend fun updateRecording(recording: VoiceRecording)

    @Delete
    suspend fun deleteRecording(recording: VoiceRecording)

    @Query("DELETE FROM voice_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("UPDATE voice_recordings SET title = :newTitle WHERE id = :id")
    suspend fun renameRecording(id: Long, newTitle: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(ref: PlaylistSongCrossRef)

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = :playlistId ORDER BY ps.orderIndex ASC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongItem>>

    // Ducking Settings
    @Query("SELECT * FROM ducking_settings WHERE id = 1 LIMIT 1")
    fun getDuckingSettings(): Flow<DuckingSettings?>

    @Query("SELECT * FROM ducking_settings WHERE id = 1 LIMIT 1")
    suspend fun getDuckingSettingsSync(): DuckingSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDuckingSettings(settings: DuckingSettings)
}
