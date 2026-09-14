package com.example.audio.importer

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.SongItem
import com.example.data.repository.NfcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioFileImporter(
    private val context: Context,
    private val repository: NfcRepository
) {
    suspend fun importAudioFile(uri: Uri, isVoiceMp3: Boolean): Result<SongItem> = withContext(Dispatchers.IO) {
        try {
            // Try to take persistable URI permission
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            // Get display name & size from ContentResolver
            var fileName = "Imported_${System.currentTimeMillis()}.mp3"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Copy file to app storage to ensure perpetual accessibility
            val targetDirName = if (isVoiceMp3) "nfc_voice_mp3s" else "nfc_music_library"
            val targetDir = File(context.filesDir, targetDirName).apply { mkdirs() }
            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(targetDir, "${System.currentTimeMillis()}_$cleanName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize == 0L) {
                fileSize = targetFile.length()
            }

            // Extract metadata via MediaMetadataRetriever
            var title = fileName.substringBeforeLast(".")
            var artist = if (isVoiceMp3) "My Voice" else "Unknown Artist"
            var album = if (isVoiceMp3) "Voice MP3s" else "Imported Library"
            var durationMs = 0L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(targetFile.absolutePath)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank() && !isVoiceMp3) artist = metaArtist
                if (!metaAlbum.isNullOrBlank()) album = metaAlbum
                if (!metaDur.isNullOrBlank()) durationMs = metaDur.toLongOrNull() ?: 0L
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val songItem = SongItem(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uriString = Uri.fromFile(targetFile).toString(),
                artworkUrl = null,
                isVoiceMp3 = isVoiceMp3,
                isOnline = false,
                fileSize = fileSize,
                dateAdded = System.currentTimeMillis()
            )

            val id = repository.insertSong(songItem)
            Result.success(songItem.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
