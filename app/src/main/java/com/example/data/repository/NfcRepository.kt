package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.NfcDao
import com.example.data.model.DuckingSettings
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongItem
import com.example.data.model.VoiceRecording
import kotlinx.coroutines.flow.Flow
import java.io.File

class NfcRepository(
    private val nfcDao: NfcDao,
    private val context: Context
) {
    val musicSongs: Flow<List<SongItem>> = nfcDao.getAllMusicSongs()
    val voiceMp3Songs: Flow<List<SongItem>> = nfcDao.getAllVoiceMp3Songs()
    val voiceRecordings: Flow<List<VoiceRecording>> = nfcDao.getAllVoiceRecordings()
    val playlists: Flow<List<Playlist>> = nfcDao.getAllPlaylists()
    val duckingSettings: Flow<DuckingSettings?> = nfcDao.getDuckingSettings()

    suspend fun insertSong(song: SongItem): Long = nfcDao.insertSong(song)
    suspend fun getSongByUri(uri: String): SongItem? = nfcDao.getSongByUri(uri)

    suspend fun updateSong(song: SongItem) = nfcDao.updateSong(song)

    suspend fun renameSong(id: Long, newTitle: String) = nfcDao.renameSong(id, newTitle)

    // Remove from NFC library without deleting physical file from phone
    suspend fun removeFromLibrary(songId: Long) {
        nfcDao.deleteSongById(songId)
    }

    // Permanently delete file from phone storage if it resides in app-managed directory
    suspend fun deletePermanently(song: SongItem): Boolean {
        nfcDao.deleteSongById(song.id)
        return try {
            val uri = Uri.parse(song.uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) file.delete() else false
            } else {
                // If it's a content URI, try deleting via ContentResolver
                context.contentResolver.delete(uri, null, null) > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun insertRecording(recording: VoiceRecording): Long = nfcDao.insertRecording(recording)

    suspend fun renameRecording(id: Long, newTitle: String) = nfcDao.renameRecording(id, newTitle)

    suspend fun deleteRecording(recording: VoiceRecording): Boolean {
        nfcDao.deleteRecordingById(recording.id)
        return try {
            val file = File(recording.filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createPlaylist(name: String): Long = nfcDao.insertPlaylist(Playlist(name = name))

    suspend fun deletePlaylist(playlist: Playlist) = nfcDao.deletePlaylist(playlist)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        nfcDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongItem>> = nfcDao.getSongsForPlaylist(playlistId)

    suspend fun updateDuckingSettings(settings: DuckingSettings) {
        nfcDao.insertOrUpdateDuckingSettings(settings)
    }

    suspend fun getDuckingSettingsSync(): DuckingSettings {
        return nfcDao.getDuckingSettingsSync() ?: DuckingSettings(
            id = 1,
            isDuckingEnabled = true,
            duckingLevelPercent = 30,
            fadeDownSpeedMs = 300L,
            fadeUpSpeedMs = 600L,
            musicVolumePercent = 100,
            voiceMicVolumePercent = 100
        )
    }
}
