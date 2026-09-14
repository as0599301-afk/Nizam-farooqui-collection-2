package com.example.audio.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicProviderRegistry {

    private val providers = mutableMapOf<String, MusicProvider>()

    private val _activeProvider = MutableStateFlow<MusicProvider>(JioSaavnLegalMusicProvider())
    val activeProvider: StateFlow<MusicProvider> = _activeProvider.asStateFlow()

    init {
        val saavn = JioSaavnLegalMusicProvider()
        val jamendo = JamendoLegalMusicProvider()
        registerProvider(saavn)
        registerProvider(jamendo)
        _activeProvider.value = saavn
    }

    fun registerProvider(provider: MusicProvider) {
        providers[provider.providerId] = provider
    }

    fun getAvailableProviders(): List<MusicProvider> = providers.values.toList()

    fun setActiveProvider(providerId: String): Boolean {
        val provider = providers[providerId] ?: return false
        _activeProvider.value = provider
        return true
    }

    fun getProvider(providerId: String): MusicProvider? = providers[providerId]
}
