package com.example.audio.importer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.R
import com.example.data.model.SongItem
import com.example.data.repository.NfcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BundledMp3Seeder {

    private data class Asset(val resId: Int, val title: String, val fileName: String)

    private val assets = listOf(
        Asset(R.raw.speech1, "Speech 1", "speech1.mp3"),
        Asset(R.raw.speech2, "Speech 2", "speech2.mp3"),
        Asset(R.raw.speech3, "Speech 3", "speech3.mp3"),
        Asset(R.raw.speech4, "Speech 4", "speech4.mp3"),
        Asset(R.raw.speech5, "Speech 5", "speech5.mp3"),
        Asset(R.raw.speech6, "Speech 6", "speech6.mp3")
    )

    suspend fun seed(context: Context, repository: NfcRepository) =
        withContext(Dispatchers.IO) {

            val directory = File(
                context.filesDir,
                "nfc_music_library/bundled"
            ).apply {
                mkdirs()
            }

            assets.forEach { asset ->

                val targetFile = File(directory, asset.fileName)

                if (!targetFile.exists()) {
                    context.resources.openRawResource(asset.resId).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val uri = Uri.fromFile(targetFile).toString()

                if (repository.getSongByUri(uri) == null) {

                    var durationMs = 0L

                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(targetFile.absolutePath)

                        durationMs =
                            retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION
                            )?.toLongOrNull() ?: 0L

                        retriever.release()
                    } catch (_: Exception) {
                    }

                    repository.insertSong(
                        SongItem(
                            title = asset.title,
                            artist = "Nizam Farooqui Collection 4",
                            album = "Bundled Recordings",
                            durationMs = durationMs,
                            uriString = uri,
                            artworkUrl = null,
                            isVoiceMp3 = false,
                            isOnline = false,
                            fileSize = targetFile.length()
                        )
                    )
                }
            }
        }
}
