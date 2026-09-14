package com.example.audio.recording

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.audio.mic.LiveMicEngine
import com.example.data.model.VoiceRecording
import com.example.data.repository.NfcRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

enum class RecordingState {
    IDLE, RECORDING, PAUSED
}

class VoiceRecordingManager(
    private val context: Context,
    private val repository: NfcRepository,
    private val liveMicEngine: LiveMicEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    private var tempPcmFile: File? = null
    private var pcmOutputStream: FileOutputStream? = null
    private var standaloneRecord: AudioRecord? = null
    private var standaloneThread: Thread? = null
    private val isStandaloneRecording = AtomicBoolean(false)

    private var durationTimerJob: Job? = null
    private var recordingStartTime = 0L
    private var accumulatedDuration = 0L

    init {
        // Hook up to live mic if live mic is active
        liveMicEngine.rawPcmListener = { pcmData, count ->
            if (_recordingState.value == RecordingState.RECORDING) {
                writeRawPcm(pcmData, count)
            }
        }
    }

    @Synchronized
    private fun writeRawPcm(pcmData: ShortArray, count: Int) {
        try {
            val byteBuffer = ByteArray(count * 2)
            for (i in 0 until count) {
                val sample = pcmData[i].toInt()
                byteBuffer[i * 2] = (sample and 0x00FF).toByte()
                byteBuffer[i * 2 + 1] = ((sample shr 8) and 0x00FF).toByte()
            }
            pcmOutputStream?.write(byteBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (_recordingState.value != RecordingState.IDLE) return false

        try {
            val recDir = File(context.filesDir, "nfc_recordings").apply { mkdirs() }
            tempPcmFile = File(recDir, "temp_recording_${System.currentTimeMillis()}.pcm")
            pcmOutputStream = FileOutputStream(tempPcmFile)

            recordingStartTime = System.currentTimeMillis()
            accumulatedDuration = 0L
            _recordingDurationMs.value = 0L
            _recordingError.value = null

            // If live mic is not already running, start standalone AudioRecord for recording
            if (!liveMicEngine.isMicActive.value) {
                startStandaloneAudioRecord()
            }

            _recordingState.value = RecordingState.RECORDING
            startDurationTimer()
            return true
        } catch (e: Exception) {
            _recordingError.value = "Failed to start recording: ${e.message}"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startStandaloneAudioRecord() {
        val minBuf = AudioRecord.getMinBufferSize(
            LiveMicEngine.SAMPLE_RATE,
            LiveMicEngine.CHANNEL_CONFIG_IN,
            LiveMicEngine.AUDIO_FORMAT
        )
        val bufSize = (minBuf * 2).coerceAtLeast(2048)

        standaloneRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            LiveMicEngine.SAMPLE_RATE,
            LiveMicEngine.CHANNEL_CONFIG_IN,
            LiveMicEngine.AUDIO_FORMAT,
            bufSize
        )

        if (standaloneRecord?.state != AudioRecord.STATE_INITIALIZED) {
            _recordingError.value = "Microphone failed to initialize"
            return
        }

        isStandaloneRecording.set(true)
        standaloneThread = Thread {
            val rec = standaloneRecord ?: return@Thread
            try {
                rec.startRecording()
                val buffer = ShortArray(512)
                while (isStandaloneRecording.get()) {
                    val read = rec.read(buffer, 0, buffer.size)
                    if (read > 0 && _recordingState.value == RecordingState.RECORDING) {
                        writeRawPcm(buffer, read)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    rec.stop()
                    rec.release()
                } catch (_: Exception) {}
            }
        }.apply {
            name = "NfcRecordingThread"
            start()
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            accumulatedDuration += System.currentTimeMillis() - recordingStartTime
            _recordingState.value = RecordingState.PAUSED
            durationTimerJob?.cancel()
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.RECORDING
            startDurationTimer()
        }
    }

    suspend fun stopAndSaveRecording(suggestedTitle: String? = null): VoiceRecording? = withContext(Dispatchers.IO) {
        if (_recordingState.value == RecordingState.IDLE) return@withContext null

        if (_recordingState.value == RecordingState.RECORDING) {
            accumulatedDuration += System.currentTimeMillis() - recordingStartTime
        }

        durationTimerJob?.cancel()
        _recordingState.value = RecordingState.IDLE

        // Stop standalone audio record if running
        isStandaloneRecording.set(false)
        standaloneThread?.interrupt()
        standaloneThread = null
        standaloneRecord = null

        try {
            pcmOutputStream?.flush()
            pcmOutputStream?.close()
            pcmOutputStream = null
        } catch (_: Exception) {}

        val pcm = tempPcmFile
        if (pcm == null || !pcm.exists() || pcm.length() == 0L) {
            _recordingError.value = "No voice audio was captured"
            pcm?.delete()
            return@withContext null
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val finalTitle = suggestedTitle?.ifBlank { null }
            ?: "NFC Voice $timestamp"

        val recDir = File(context.filesDir, "nfc_recordings").apply { mkdirs() }
        val wavFile = File(recDir, "NFC_${timestamp}.wav")

        val converted = convertPcmToWav(
            pcmFile = pcm,
            wavFile = wavFile,
            sampleRate = LiveMicEngine.SAMPLE_RATE,
            channels = 1,
            bitsPerSample = 16
        )

        // Delete temporary PCM
        pcm.delete()

        if (!converted || !wavFile.exists()) {
            _recordingError.value = "Failed to convert recording to audio file"
            return@withContext null
        }

        val duration = accumulatedDuration.coerceAtLeast(1000L)
        val recording = VoiceRecording(
            title = finalTitle,
            filePath = wavFile.absolutePath,
            durationMs = duration,
            fileSizeBytes = wavFile.length(),
            dateRecorded = System.currentTimeMillis()
        )

        val id = repository.insertRecording(recording)
        val saved = recording.copy(id = id)
        _recordingDurationMs.value = 0L
        return@withContext saved
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                val current = accumulatedDuration + (System.currentTimeMillis() - recordingStartTime)
                _recordingDurationMs.value = current
                delay(100)
            }
        }
    }

    private fun convertPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): Boolean {
        try {
            val pcmDataLength = pcmFile.length()
            val totalDataLen = pcmDataLength + 36
            val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

            val header = ByteArray(44)
            // RIFF/WAVE header
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xffL).toByte()
            header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
            header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
            header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 16 for PCM
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // Audio format 1 = PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xffL).toByte()
            header[29] = ((byteRate shr 8) and 0xffL).toByte()
            header[30] = ((byteRate shr 16) and 0xffL).toByte()
            header[31] = ((byteRate shr 24) and 0xffL).toByte()
            header[32] = (channels * bitsPerSample / 8).toByte() // block align
            header[33] = 0
            header[34] = bitsPerSample.toByte() // bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (pcmDataLength and 0xffL).toByte()
            header[41] = ((pcmDataLength shr 8) and 0xffL).toByte()
            header[42] = ((pcmDataLength shr 16) and 0xffL).toByte()
            header[43] = ((pcmDataLength shr 24) and 0xffL).toByte()

            FileOutputStream(wavFile).use { out ->
                out.write(header, 0, 44)
                FileInputStream(pcmFile).use { inStream ->
                    val buffer = ByteArray(2048)
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun clearError() {
        _recordingError.value = null
    }
}
