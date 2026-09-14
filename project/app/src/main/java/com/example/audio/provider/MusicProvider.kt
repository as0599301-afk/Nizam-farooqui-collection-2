package com.example.audio.provider

import com.example.data.model.SearchCategory
import com.example.data.model.SongItem

interface MusicProvider {
    val providerId: String
    val displayName: String
    val description: String
    val supportedLanguages: List<String>
    fun isConfigured(): Boolean
    fun getConfigurationMessage(): String
    suspend fun search(
        query: String,
        category: SearchCategory,
        language: String? = null
    ): Result<List<SongItem>>
}
