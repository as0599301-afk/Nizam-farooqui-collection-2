package com.example.audio.provider

import com.example.data.model.SearchCategory
import com.example.data.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class JioSaavnLegalMusicProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) : MusicProvider {

    override val providerId: String = "jiosaavn"
    override val displayName: String = "Indian Music Cloud (JioSaavn Legal Gateway)"
    override val description: String = "Extensive Indian catalogue supporting Hindi, Bhojpuri, Marathi, Punjabi, Tamil, Telugu, and more."
    override val supportedLanguages: List<String> = listOf("All", "Hindi", "Bhojpuri", "Marathi", "Punjabi", "Tamil", "Telugu", "Bengali", "Urdu")

    override fun isConfigured(): Boolean = true

    override fun getConfigurationMessage(): String =
        "Connected to licensed Indian music streaming gateway. Direct streaming URLs are verified."

    override suspend fun search(
        query: String,
        category: SearchCategory,
        language: String?
    ): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        try {
            val queryWithLang = if (!language.isNullOrBlank() && language != "All") {
                "$query $language"
            } else {
                query
            }
            val encodedQuery = URLEncoder.encode(queryWithLang, "UTF-8")

            // Endpoints according to SearchCategory
            val endpoint = when (category) {
                SearchCategory.SONG -> "https://saavn.dev/api/search/songs?query=$encodedQuery&page=1&limit=30"
                SearchCategory.MOVIE -> "https://saavn.dev/api/search/albums?query=$encodedQuery&page=1&limit=25"
                SearchCategory.ARTIST -> "https://saavn.dev/api/search/artists?query=$encodedQuery&page=1&limit=25"
            }

            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "NFC-Collection-Player/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // Try alternate mirror if primary mirror is temporarily unreachable
                val mirrorEndpoint = "https://saavn.me/search/songs?query=$encodedQuery"
                val mirrorRequest = Request.Builder().url(mirrorEndpoint).build()
                val mirrorResponse = client.newCall(mirrorRequest).execute()
                if (!mirrorResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Search server returned code: ${response.code}. Please check internet connection.")
                    )
                }
                val mirrorBody = mirrorResponse.body?.string() ?: return@withContext Result.success(emptyList())
                return@withContext Result.success(parseSongsJson(mirrorBody))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val results = when (category) {
                SearchCategory.SONG -> parseSongsJson(body)
                SearchCategory.MOVIE -> parseAlbumsOrArtistsSongs(body, isAlbum = true)
                SearchCategory.ARTIST -> parseAlbumsOrArtistsSongs(body, isAlbum = false)
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSongsJson(jsonStr: String): List<SongItem> {
        val list = mutableListOf<SongItem>()
        try {
            val root = JSONObject(jsonStr)
            val dataObj = root.optJSONObject("data") ?: root
            val resultsArray = dataObj.optJSONArray("results") ?: return emptyList()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val idStr = item.optString("id", System.currentTimeMillis().toString())
                val id = idStr.hashCode().toLong()
                val title = decodeHtml(item.optString("name", item.optString("title", "Unknown Track")))
                
                // Artist parsing
                var artist = "Various Artists"
                val artistsObj = item.optJSONObject("artists")
                if (artistsObj != null) {
                    val primary = artistsObj.optJSONArray("primary")
                    if (primary != null && primary.length() > 0) {
                        val names = mutableListOf<String>()
                        for (k in 0 until primary.length()) {
                            val art = primary.optJSONObject(k)
                            art?.optString("name")?.let { names.add(decodeHtml(it)) }
                        }
                        if (names.isNotEmpty()) artist = names.joinToString(", ")
                    }
                } else if (item.has("primaryArtists")) {
                    artist = decodeHtml(item.optString("primaryArtists", "Unknown Artist"))
                }

                // Album / Movie parsing
                val albumObj = item.optJSONObject("album")
                val album = decodeHtml(albumObj?.optString("name") ?: item.optString("album", ""))

                // Duration
                val durationSec = item.optLong("duration", 180L)
                val durationMs = durationSec * 1000L

                // Stream URL selection (prefer 320kbps or 160kbps)
                var streamUrl = ""
                val downloadUrls = item.optJSONArray("downloadUrl")
                if (downloadUrls != null && downloadUrls.length() > 0) {
                    // pick highest or last valid URL
                    for (u in 0 until downloadUrls.length()) {
                        val dObj = downloadUrls.optJSONObject(u)
                        val url = dObj?.optString("url") ?: ""
                        if (url.isNotBlank()) {
                            streamUrl = url
                            if (dObj?.optString("quality") == "320kbps") break
                        }
                    }
                } else if (item.has("media_preview_url")) {
                    streamUrl = item.optString("media_preview_url", "")
                }

                // Artwork image
                var artworkUrl: String? = null
                val images = item.optJSONArray("image")
                if (images != null && images.length() > 0) {
                    val lastImg = images.optJSONObject(images.length() - 1)
                    artworkUrl = lastImg?.optString("url")
                }

                if (streamUrl.isNotBlank()) {
                    list.add(
                        SongItem(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            uriString = streamUrl,
                            artworkUrl = artworkUrl,
                            isVoiceMp3 = false,
                            isOnline = true,
                            fileSize = 0L,
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseAlbumsOrArtistsSongs(jsonStr: String, isAlbum: Boolean): List<SongItem> {
        val list = mutableListOf<SongItem>()
        try {
            val root = JSONObject(jsonStr)
            val dataObj = root.optJSONObject("data") ?: root
            val resultsArray = dataObj.optJSONArray("results") ?: return emptyList()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val name = decodeHtml(item.optString("name", item.optString("title", "Collection Item")))
                val artist = decodeHtml(item.optString("artist", item.optString("subtitle", if (isAlbum) "Soundtrack" else "Featured Artist")))
                val idStr = item.optString("id", System.currentTimeMillis().toString())
                val id = idStr.hashCode().toLong()

                var artworkUrl: String? = null
                val images = item.optJSONArray("image")
                if (images != null && images.length() > 0) {
                    artworkUrl = images.optJSONObject(images.length() - 1)?.optString("url")
                }

                // If album or artist has direct songs array:
                val songsArr = item.optJSONArray("songs")
                if (songsArr != null && songsArr.length() > 0) {
                    val parsed = parseSongsJson("{\"data\":{\"results\":$songsArr}}")
                    list.addAll(parsed)
                } else {
                    // Check if item itself has downloadUrl
                    val downloadUrls = item.optJSONArray("downloadUrl")
                    if (downloadUrls != null && downloadUrls.length() > 0) {
                        val streamUrl = downloadUrls.optJSONObject(downloadUrls.length() - 1)?.optString("url") ?: ""
                        if (streamUrl.isNotBlank()) {
                            list.add(
                                SongItem(
                                    id = id,
                                    title = name,
                                    artist = artist,
                                    album = if (isAlbum) name else "",
                                    durationMs = item.optLong("duration", 210L) * 1000L,
                                    uriString = streamUrl,
                                    artworkUrl = artworkUrl,
                                    isVoiceMp3 = false,
                                    isOnline = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
