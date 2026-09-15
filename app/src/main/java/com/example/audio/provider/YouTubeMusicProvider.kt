package com.example.audio.provider

import com.example.data.model.SearchCategory
import com.example.data.model.SongItem
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class YouTubeMusicProvider(
    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY
) : MusicProvider {

    override val providerId: String = "youtube"
    override val displayName: String = "YouTube"

    override val description: String =
        "Search YouTube videos and play them through the official YouTube player."

    override val supportedLanguages: List<String> =
        listOf("All", "Hindi", "English", "Marathi", "Gujarati", "Bengali", "Tamil", "Malayalam", "Urdu", "Bhojpuri")

    override fun isConfigured(): Boolean =
        apiKey.isNotBlank() && apiKey != "YOUR_YOUTUBE_API_KEY"

    override fun getConfigurationMessage(): String =
        if (isConfigured()) {
            "YouTube API is configured."
        } else {
            "YouTube API key is not configured."
        }

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()

    override suspend fun search(
        query: String,
        category: SearchCategory,
        language: String?
    ): Result<List<SongItem>> {
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("YouTube API key is not configured.")
            )
        }

        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet" +
                    "&type=video" +
                    "&maxResults=25" +
                    "&q=$encoded" +
                    "&key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "YouTube API error ${response.code}: ${response.message}"
                        )
                    )
                }

                val body = response.body?.string()
                    ?: return Result.failure(
                        IllegalStateException("Empty YouTube response")
                    )

                val json = moshi.adapter(YouTubeSearchResponse::class.java)
                    .fromJson(body)
                    ?: return Result.success(emptyList())

                Result.success(
                    json.items.mapNotNull { item ->
                        val videoId = item.id?.videoId ?: return@mapNotNull null

                        SongItem(
                            title = item.snippet?.title ?: "YouTube Video",
                            artist = item.snippet?.channelTitle ?: "YouTube",
                            uriString = "youtube:$videoId",
                            durationMs = 0L,
                            album = "YouTube",
                            artworkUrl = item.snippet?.thumbnails?.default?.url,
                            isOnline = true
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @JsonClass(generateAdapter = true)
    data class YouTubeSearchResponse(
        val items: List<YouTubeItem> = emptyList()
    )

    @JsonClass(generateAdapter = true)
    data class YouTubeItem(
        val id: YouTubeId? = null,
        val snippet: YouTubeSnippet? = null
    )

    @JsonClass(generateAdapter = true)
    data class YouTubeId(
        val videoId: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class YouTubeSnippet(
        val title: String? = null,
        val channelTitle: String? = null,
        val thumbnails: YouTubeThumbnails? = null
    )

    @JsonClass(generateAdapter = true)
    data class YouTubeThumbnails(
        val default: YouTubeThumbnail? = null
    )

    @JsonClass(generateAdapter = true)
    data class YouTubeThumbnail(
        val url: String? = null
    )
}
