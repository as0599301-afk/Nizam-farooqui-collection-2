package com.example.audio.engine

import com.example.data.model.DuckingSettings
import com.example.data.model.VoiceSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioDuckingEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val activeSources = Collections.newSetFromMap(ConcurrentHashMap<VoiceSourceType, Boolean>())

    private val _activeVoiceSources = MutableStateFlow<Set<VoiceSourceType>>(emptySet())
    val activeVoiceSources: StateFlow<Set<VoiceSourceType>> = _activeVoiceSources.asStateFlow()

    private val _isDucked = MutableStateFlow(false)
    val isDucked: StateFlow<Boolean> = _isDucked.asStateFlow()

    // Ducking multiplier from 0.0f to 1.0f
    private val _currentDuckingFactor = MutableStateFlow(1.0f)
    val currentDuckingFactor: StateFlow<Float> = _currentDuckingFactor.asStateFlow()

    // Effective absolute volume (0.0f to 1.0f) combining user music volume and ducking
    private val _effectiveMusicVolume = MutableStateFlow(1.0f)
    val effectiveMusicVolume: StateFlow<Float> = _effectiveMusicVolume.asStateFlow()

    private var settings = DuckingSettings()
    private var rampJob: Job? = null
    private var testPreviewJob: Job? = null

    // Listener called when volume changes
    var onVolumeChangedListener: ((Float) -> Unit)? = null

    fun updateSettings(newSettings: DuckingSettings) {
        settings = newSettings
        recalculateTargetVolume()
    }

    fun getSettings(): DuckingSettings = settings

    fun onVoiceSourceStarted(source: VoiceSourceType) {
        activeSources.add(source)
        _activeVoiceSources.value = activeSources.toSet()
        _isDucked.value = settings.isDuckingEnabled && activeSources.isNotEmpty()
        recalculateTargetVolume()
    }

    fun onVoiceSourceStopped(source: VoiceSourceType) {
        activeSources.remove(source)
        _activeVoiceSources.value = activeSources.toSet()
        _isDucked.value = settings.isDuckingEnabled && activeSources.isNotEmpty()
        recalculateTargetVolume()
    }

    fun isSourceActive(source: VoiceSourceType): Boolean = activeSources.contains(source)

    private fun recalculateTargetVolume() {
        val userBaseVolume = (settings.musicVolumePercent.coerceIn(0, 100)) / 100f

        val targetFactor = if (settings.isDuckingEnabled && activeSources.isNotEmpty()) {
            (settings.duckingLevelPercent.coerceIn(0, 100)) / 100f
        } else {
            1.0f
        }

        val targetEffectiveVolume = userBaseVolume * targetFactor

        // Determine fade duration
        val isFadingDown = targetEffectiveVolume < _effectiveMusicVolume.value
        val fadeDuration = if (isFadingDown) {
            max(50L, settings.fadeDownSpeedMs)
        } else {
            max(50L, settings.fadeUpSpeedMs)
        }

        startSmoothRamp(targetFactor, targetEffectiveVolume, fadeDuration)
    }

    private fun startSmoothRamp(
        targetFactor: Float,
        targetEffectiveVolume: Float,
        durationMs: Long
    ) {
        rampJob?.cancel()
        rampJob = scope.launch {
            val stepTimeMs = 20L
            val steps = max(1, (durationMs / stepTimeMs).toInt())
            val startFactor = _currentDuckingFactor.value
            val startVolume = _effectiveMusicVolume.value

            val factorDiff = targetFactor - startFactor
            val volumeDiff = targetEffectiveVolume - startVolume

            for (i in 1..steps) {
                delay(stepTimeMs)
                val progress = i.toFloat() / steps.toFloat()
                // Ease-in-out curve for natural audio transition
                val smoothProgress = progress * progress * (3f - 2f * progress)

                val newFactor = (startFactor + factorDiff * smoothProgress).coerceIn(0f, 1f)
                val newVolume = (startVolume + volumeDiff * smoothProgress).coerceIn(0f, 1f)

                _currentDuckingFactor.value = newFactor
                _effectiveMusicVolume.value = newVolume
                onVolumeChangedListener?.invoke(newVolume)
            }

            // Ensure final precise target
            _currentDuckingFactor.value = targetFactor
            _effectiveMusicVolume.value = targetEffectiveVolume
            onVolumeChangedListener?.invoke(targetEffectiveVolume)
        }
    }

    // Interactive Preview/Test feature requested in Ducking Settings
    fun triggerTestPreview() {
        testPreviewJob?.cancel()
        testPreviewJob = scope.launch {
            onVoiceSourceStarted(VoiceSourceType.PREVIEW_TEST)
            delay(3500) // Keep ducked for 3.5s so user can hear the test ducking
            onVoiceSourceStopped(VoiceSourceType.PREVIEW_TEST)
        }
    }
}
