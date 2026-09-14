package com.example.audio.provider

import com.example.data.model.SearchCategory
import com.example.data.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class JamendoLegalMusicProvider(
    private var clientId: String = "c25a815a" // Public Jamendo sandbox client ID
) : MusicProvider {

    override val providerId: String = "jamendo"
    override val displayName: String = "Jamendo Open Music (Creative Commons)"
    override val description: String = "Global legal Creative Commons music library for independent artists."
    override val supportedLanguages: List<String> = listOf("All", "English", "Instrumental", "World")

    fun setClientId(newKey: String) {
        clientId = newKey
    }

    override fun isConfigured(): Boolean = clientId.isNotBlank()

    override fun getConfigurationMessage(): String =
        if (isConfigured()) "Configured with Client ID ($clientId)."
        else "API Key required. Please enter Jamendo Client ID in Settings."

    override suspend fun search(
        query: String,
        category: SearchCategory,
        language: String?
    ): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Jamendo provider is not configured. Please supply an API Client ID in App Settings.")
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&limit=25&namesearch=$encodedQuery&include=musicinfo"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Jamendo API responded with status ${response.code}: ${response.message}")
                )
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<SongItem>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val id = item.optLong("id", System.currentTimeMillis() + i)
                val name = item.optString("name", "Untitled")
                val artist = item.optString("artist_name", "Unknown Artist")
                val album = item.optString("album_name", "")
                val durationSec = item.optLong("duration", 180L)
                val audioUrl = item.optString("audio", "")
                val image = item.optString("image", "")

                if (audioUrl.isNotBlank()) {
                    list.add(
                        SongItem(
                            id = id,
                            title = name,
                            artist = artist,
                            album = album,
                            durationMs = durationSec * 1000L,
                            uriString = audioUrl,
                            artworkUrl = image.ifBlank { null },
                            isVoiceMp3 = false,
                            isOnline = true
                        )
                    )
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
