package com.example.audio.mic

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import com.example.audio.engine.AudioDuckingEngine
import com.example.data.model.VoiceEffectType
import com.example.data.model.VoiceSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt

class LiveMicEngine(
    private val context: Context,
    private val duckingEngine: AudioDuckingEngine
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var speechDuckingActive = false
    private var speechAboveCount = 0
    private var speechBelowCount = 0

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive.asStateFlow()

    private val _audioMeterLevel = MutableStateFlow(0f)
    val audioMeterLevel: StateFlow<Float> = _audioMeterLevel.asStateFlow()

    private val _currentEffect = MutableStateFlow(VoiceEffectType.ORIGINAL_VOICE)
    val currentEffect: StateFlow<VoiceEffectType> = _currentEffect.asStateFlow()

    private val _micVolume = MutableStateFlow(100) // 0 - 100%
    val micVolume: StateFlow<Int> = _micVolume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Listener for raw un-effected PCM audio for recording
    var rawPcmListener: ((ShortArray, Int) -> Unit)? = null

    // Echo circular buffers
    private var echoBuffer = FloatArray(44100)
    private var echoIndex = 0

    // Pitch shift buffers for Child Voice effect
    private val pitchGrainSize = 1024
    private val pitchBuffer = FloatArray(4096)
    private var pitchWriteIndex = 0
    private var pitchReadIndex1 = 0f
    private var pitchReadIndex2 = (pitchGrainSize / 2).toFloat()
    private val pitchRatio = 1.45f // Real-time child voice transposition

    fun setEffect(effect: VoiceEffectType) {
        _currentEffect.value = effect
    }

    fun setMicVolume(volumePercent: Int) {
        _micVolume.value = volumePercent.coerceIn(0, 100)
    }

    @SuppressLint("MissingPermission")
    fun startMic(): Boolean {
        if (isRunning.get()) return true

        val minRecBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
        if (minRecBuf == AudioRecord.ERROR || minRecBuf == AudioRecord.ERROR_BAD_VALUE) {
            _errorMessage.value = "Hardware audio record buffer size not supported"
            return false
        }

        val recBufferSize = (minRecBuf * 2).coerceAtLeast(2048)

        val minTrackBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
        val trackBufferSize = (minTrackBuf * 2).coerceAtLeast(2048)

        val audioRecord: AudioRecord
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                recBufferSize
            )
        } catch (e: SecurityException) {
            _errorMessage.value = "Microphone permission RECORD_AUDIO is required to use live mic"
            return false
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize microphone: ${e.message}"
            return false
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            _errorMessage.value = "Microphone hardware failed to initialize"
            try { audioRecord.release() } catch (_: Exception) {}
            return false
        }

        val audioTrack: AudioTrack
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize audio output track: ${e.message}"
            try { audioRecord.release() } catch (_: Exception) {}
            return false
        }

        // Hardware echo cancellation and noise suppression for open-speaker use.
        // Keep these attached to the same AudioRecord session so Bluetooth routing
        // and the existing voice effects/ducking pipeline remain unchanged.
        try {
            echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply {
                enabled = true
            }
        } catch (_: Exception) {
            echoCanceler = null
        }
        try {
            noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)?.apply {
                enabled = true
            }
        } catch (_: Exception) {
            noiseSuppressor = null
        }

        isRunning.set(true)
        _isMicActive.value = true
        _errorMessage.value = null
        // Music ducks only when actual speech is detected.
        speechDuckingActive = false
        speechAboveCount = 0
        speechBelowCount = 0

        audioThread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                audioRecord.startRecording()
                audioTrack.play()

                val bufferChunkSize = 512 // Low latency small buffer (~11.6ms chunks)
                val inBuffer = ShortArray(bufferChunkSize)
                val outBuffer = ShortArray(bufferChunkSize)

                // Reset effect state
                echoBuffer.fill(0f)
                echoIndex = 0
                pitchBuffer.fill(0f)
                pitchWriteIndex = 0
                pitchReadIndex1 = 0f
                pitchReadIndex2 = (pitchGrainSize / 2).toFloat()

                while (isRunning.get()) {
                    val readSamples = audioRecord.read(inBuffer, 0, bufferChunkSize)
                    if (readSamples <= 0) continue

                    // 1. Send raw un-effected PCM to voice recorder if active
                    rawPcmListener?.invoke(inBuffer, readSamples)

                    // 2. Calculate RMS Audio Level for real live meter
                    var sumSquares = 0.0
                    for (i in 0 until readSamples) {
                        val sample = inBuffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / readSamples) / 32768.0
                    val normalizedMeter = (rms * 4.0).coerceIn(0.0, 1.0).toFloat()
                    _audioMeterLevel.value = normalizedMeter

                    // Speech-triggered ducking: Mic ON alone never ducks music.
                    val speechThreshold = 0.08f

                    if (normalizedMeter >= speechThreshold) {
                        speechAboveCount++
                        speechBelowCount = 0

                        if (!speechDuckingActive && speechAboveCount >= 3) {
                            speechDuckingActive = true
                            duckingEngine.onVoiceSourceStarted(VoiceSourceType.LIVE_MIC)
                        }
                    } else {
                        speechBelowCount++
                        speechAboveCount = 0

                        if (speechDuckingActive && speechBelowCount >= 12) {
                            speechDuckingActive = false
                            duckingEngine.onVoiceSourceStopped(VoiceSourceType.LIVE_MIC)
                        }
                    }

                    // 3. Process Live Voice Effect (EXACTLY 5 EFFECTS)
                    val currentVolMultiplier = (_micVolume.value / 100f)
                    val activeEffect = _currentEffect.value

                    processVoiceEffect(
                        effect = activeEffect,
                        inSamples = inBuffer,
                        outSamples = outBuffer,
                        count = readSamples,
                        volume = currentVolMultiplier
                    )

                    // 4. Output processed audio to AudioTrack
                    audioTrack.write(outBuffer, 0, readSamples)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { echoCanceler?.release() } catch (_: Exception) {}
                try { noiseSuppressor?.release() } catch (_: Exception) {}
                echoCanceler = null
                noiseSuppressor = null
                try {
                    audioRecord.stop()
                    audioRecord.release()
                } catch (_: Exception) {}
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        }.apply {
            name = "NfcLiveMicThread"
            start()
        }

        return true
    }

    private fun processVoiceEffect(
        effect: VoiceEffectType,
        inSamples: ShortArray,
        outSamples: ShortArray,
        count: Int,
        volume: Float
    ) {
        when (effect) {
            VoiceEffectType.ORIGINAL_VOICE -> {
                // Direct clean pass-through
                for (i in 0 until count) {
                    val sample = (inSamples[i] * volume).coerceIn(-32768f, 32767f)
                    outSamples[i] = sample.toInt().toShort()
                }
            }
            VoiceEffectType.HIGH_ECHO -> {
                // High Echo: delay 280ms (12348 samples), decay 0.65
                applyEcho(inSamples, outSamples, count, delaySamples = 12348, decay = 0.65f, volume = volume)
            }
            VoiceEffectType.MEDIUM_ECHO -> {
                // Medium Echo: delay 180ms (7938 samples), decay 0.45
                applyEcho(inSamples, outSamples, count, delaySamples = 7938, decay = 0.45f, volume = volume)
            }
            VoiceEffectType.LOW_ECHO -> {
                // Low Echo: delay 90ms (3969 samples), decay 0.25
                applyEcho(inSamples, outSamples, count, delaySamples = 3969, decay = 0.25f, volume = volume)
            }
            VoiceEffectType.CHILD_VOICE -> {
                // Child Voice: real-time pitch transposition upwards
                applyChildVoicePitchShift(inSamples, outSamples, count, volume)
            }
        }
    }

    private fun applyEcho(
        inSamples: ShortArray,
        outSamples: ShortArray,
        count: Int,
        delaySamples: Int,
        decay: Float,
        volume: Float
    ) {
        val maxDelay = echoBuffer.size
        val effectiveDelay = delaySamples.coerceIn(100, maxDelay - 1)

        for (i in 0 until count) {
            val inVal = inSamples[i].toFloat()
            val readIdx = (echoIndex - effectiveDelay + maxDelay) % maxDelay
            val delayedVal = echoBuffer[readIdx]

            val mixed = inVal + delayedVal * decay
            echoBuffer[echoIndex] = mixed
            echoIndex = (echoIndex + 1) % maxDelay

            val finalSample = (mixed * volume).coerceIn(-32768f, 32767f)
            outSamples[i] = finalSample.toInt().toShort()
        }
    }

    private fun applyChildVoicePitchShift(
        inSamples: ShortArray,
        outSamples: ShortArray,
        count: Int,
        volume: Float
    ) {
        val bufSize = pitchBuffer.size
        val grainSize = pitchGrainSize
        val halfGrain = grainSize / 2

        for (i in 0 until count) {
            // Write incoming sample to circular buffer
            pitchBuffer[pitchWriteIndex] = inSamples[i].toFloat()

            // Calculate dual read pointers with crossfade window
            val r1 = pitchReadIndex1.toInt() % bufSize
            val r2 = pitchReadIndex2.toInt() % bufSize

            val sample1 = pitchBuffer[r1]
            val sample2 = pitchBuffer[r2]

            // Triangular crossfade window based on phase
            val phase1 = (pitchReadIndex1 % grainSize) / grainSize
            val window1 = if (phase1 < 0.5f) phase1 * 2f else (1f - phase1) * 2f

            val phase2 = (pitchReadIndex2 % grainSize) / grainSize
            val window2 = if (phase2 < 0.5f) phase2 * 2f else (1f - phase2) * 2f

            val blended = (sample1 * window1 + sample2 * window2)

            // Advance read pointers at faster rate (1.45x) for child-like higher pitch
            pitchReadIndex1 += pitchRatio
            if (pitchReadIndex1 >= bufSize) pitchReadIndex1 -= bufSize

            pitchReadIndex2 += pitchRatio
            if (pitchReadIndex2 >= bufSize) pitchReadIndex2 -= bufSize

            pitchWriteIndex = (pitchWriteIndex + 1) % bufSize

            val finalSample = (blended * volume).coerceIn(-32768f, 32767f)
            outSamples[i] = finalSample.toInt().toShort()
        }
    }

    fun stopMic() {
        if (!isRunning.getAndSet(false)) return

        _isMicActive.value = false
        _audioMeterLevel.value = 0f

        // Release ducking only if speech had actually triggered it.
        if (speechDuckingActive) {
            speechDuckingActive = false
            duckingEngine.onVoiceSourceStopped(VoiceSourceType.LIVE_MIC)
        }

        speechAboveCount = 0
        speechBelowCount = 0

        try {
            audioThread?.interrupt()
            audioThread = null
        } catch (_: Exception) {}
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun release() {
        stopMic()
    }
}
