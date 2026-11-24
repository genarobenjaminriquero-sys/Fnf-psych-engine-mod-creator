package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import com.example.model.AudioSourceMode
import com.example.model.ComposerTrackData
import com.example.model.SongProject
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

object AudioSynthEngine {
    const val SAMPLE_RATE = 44100

    fun stepDurationMs(bpm: Float): Double = (60_000.0 / bpm.coerceIn(40f, 300f)) / 4.0

    fun songTotalDurationMs(song: SongProject): Long {
        return if (song.audioSourceMode == AudioSourceMode.IMPORTED_AUDIO && song.importedDurationMs > 0L) {
            song.importedDurationMs
        } else {
            val steps = song.composerData.totalSteps.coerceAtLeast(song.sections.size * 16)
            (steps * stepDurationMs(song.bpm)).toLong().coerceAtLeast(4000L)
        }
    }

    fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    data class RenderedStems(
        val instPcm: ShortArray,
        val voicesPcm: ShortArray,
        val mixPcm: ShortArray,
        val waveformPeaks: List<Float>
    )

    fun renderComposerStems(data: ComposerTrackData, bpm: Float): RenderedStems {
        val stepSec = (60.0 / bpm.coerceIn(40f, 300f)) / 4.0
        val stepSamples = (stepSec * SAMPLE_RATE).toInt().coerceAtLeast(1000)
        val totalSamples = data.totalSteps * stepSamples + SAMPLE_RATE / 2

        val instBuf = FloatArray(totalSamples)
        val voiceBuf = FloatArray(totalSamples)

        val vols = data.trackVolumes
        val mutes = data.trackMutes
        val drumVol = if (mutes.getOrElse(0) { false }) 0f else vols.getOrElse(0) { 0.9f }
        val bassVol = if (mutes.getOrElse(1) { false }) 0f else vols.getOrElse(1) { 0.8f }
        val leadVol = if (mutes.getOrElse(2) { false }) 0f else vols.getOrElse(2) { 0.7f }
        val bfVol = if (mutes.getOrElse(3) { false }) 0f else vols.getOrElse(3) { 0.9f }
        val dadVol = if (mutes.getOrElse(4) { false }) 0f else vols.getOrElse(4) { 0.9f }

        // Drums
        if (drumVol > 0f) {
            val kicks = data.drumGrid[0].orEmpty()
            val snares = data.drumGrid[1].orEmpty()
            val hats = data.drumGrid[2].orEmpty()
            val claps = data.drumGrid[3].orEmpty()
            for (s in kicks) addKick(instBuf, s * stepSamples, drumVol * 0.85f)
            for (s in snares) addSnare(instBuf, s * stepSamples, drumVol * 0.65f)
            for (s in hats) addHiHat(instBuf, s * stepSamples, drumVol * 0.35f)
            for (s in claps) addClap(instBuf, s * stepSamples, drumVol * 0.55f)
        }

        // Bass
        if (bassVol > 0f) {
            for ((step, midi) in data.bassNotes) {
                if (step in 0 until data.totalSteps) {
                    addBassNote(instBuf, step * stepSamples, (stepSamples * 1.4).toInt(), midiToFreq(midi), bassVol * 0.55f)
                }
            }
        }

        // Lead
        if (leadVol > 0f) {
            for ((step, midi) in data.leadNotes) {
                if (step in 0 until data.totalSteps) {
                    addLeadNote(instBuf, step * stepSamples, (stepSamples * 1.6).toInt(), midiToFreq(midi), leadVol * 0.38f)
                }
            }
        }

        // BF Vocals ("Beep/Boop" formant synth)
        if (bfVol > 0f) {
            for ((step, midi) in data.bfVocalNotes) {
                if (step in 0 until data.totalSteps) {
                    addVocalNote(voiceBuf, step * stepSamples, (stepSamples * 1.5).toInt(), midiToFreq(midi), isBf = true, gain = bfVol * 0.65f)
                }
            }
        }

        // Opponent Vocals (deeper synth formant)
        if (dadVol > 0f) {
            for ((step, midi) in data.dadVocalNotes) {
                if (step in 0 until data.totalSteps) {
                    addVocalNote(voiceBuf, step * stepSamples, (stepSamples * 1.6).toInt(), midiToFreq(midi), isBf = false, gain = dadVol * 0.65f)
                }
            }
        }

        val instShort = ShortArray(totalSamples)
        val voiceShort = ShortArray(totalSamples)
        val mixShort = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val iv = tanh(instBuf[i].toDouble()).toFloat()
            val vv = tanh(voiceBuf[i].toDouble()).toFloat()
            val mv = tanh((instBuf[i] + voiceBuf[i]).toDouble()).toFloat()
            instShort[i] = (iv * 32000f).toInt().coerceIn(-32767, 32767).toShort()
            voiceShort[i] = (vv * 32000f).toInt().coerceIn(-32767, 32767).toShort()
            mixShort[i] = (mv * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }

        val peaks = computePeaksFromPcm(mixShort, 96)
        return RenderedStems(instShort, voiceShort, mixShort, peaks)
    }

    fun renderPreviewHit(trackIndex: Int, midiOrDrumLane: Int): ShortArray {
        val len = (SAMPLE_RATE * 0.24).toInt()
        val buf = FloatArray(len)
        when (trackIndex) {
            0 -> when (midiOrDrumLane % 4) {
                0 -> addKick(buf, 0, 0.9f)
                1 -> addSnare(buf, 0, 0.75f)
                2 -> addHiHat(buf, 0, 0.55f)
                else -> addClap(buf, 0, 0.7f)
            }
            1 -> addBassNote(buf, 0, len, midiToFreq(midiOrDrumLane), 0.7f)
            2 -> addLeadNote(buf, 0, len, midiToFreq(midiOrDrumLane), 0.55f)
            3 -> addVocalNote(buf, 0, len, midiToFreq(midiOrDrumLane), isBf = true, gain = 0.75f)
            4 -> addVocalNote(buf, 0, len, midiToFreq(midiOrDrumLane), isBf = false, gain = 0.75f)
            else -> addHitTick(buf, 0, 0.6f)
        }
        return ShortArray(len) { i ->
            (tanh(buf[i].toDouble()) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun addHitTick(buf: FloatArray, start: Int, gain: Float) {
        val len = min(buf.size - start, (SAMPLE_RATE * 0.045).toInt())
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 95.0)
            buf[start + i] += (sin(2.0 * PI * 1150.0 * t) * env * gain).toFloat()
        }
    }

    private fun addKick(buf: FloatArray, start: Int, gain: Float) {
        val len = min(buf.size - start, (SAMPLE_RATE * 0.18).toInt())
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 48.0 + 115.0 * exp(-t * 36.0)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = exp(-t * 16.0)
            buf[start + i] += (sin(phase) * env * gain).toFloat()
        }
    }

    private fun addSnare(buf: FloatArray, start: Int, gain: Float) {
        val len = min(buf.size - start, (SAMPLE_RATE * 0.16).toInt())
        val rng = Random(42)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val body = sin(2.0 * PI * 195.0 * t) * exp(-t * 28.0) * 0.55
            val noise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 22.0) * 0.75
            buf[start + i] += ((body + noise) * gain).toFloat()
        }
    }

    private fun addHiHat(buf: FloatArray, start: Int, gain: Float) {
        val len = min(buf.size - start, (SAMPLE_RATE * 0.06).toInt())
        val rng = Random(13)
        var prev = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val raw = rng.nextDouble() * 2.0 - 1.0
            val hp = raw - prev * 0.85
            prev = raw
            buf[start + i] += (hp * exp(-t * 68.0) * gain).toFloat()
        }
    }

    private fun addClap(buf: FloatArray, start: Int, gain: Float) {
        val len = min(buf.size - start, (SAMPLE_RATE * 0.18).toInt())
        val rng = Random(99)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val burst = if (t < 0.025) abs(sin(t * 240.0)) else 1.0
            val noise = (rng.nextDouble() * 2.0 - 1.0) * burst * exp(-t * 19.0)
            buf[start + i] += (noise * gain).toFloat()
        }
    }

    private fun addBassNote(buf: FloatArray, start: Int, durSamples: Int, freq: Double, gain: Float) {
        val len = min(buf.size - start, durSamples)
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / len
            phase += freq / SAMPLE_RATE
            val p = phase - phase.toInt()
            val saw = p * 2.0 - 1.0
            val sub = sin(2.0 * PI * p)
            val env = min(1.0, i / 120.0) * exp(-progress * 2.6)
            val sample = (saw * 0.45 + sub * 0.75) * env * gain
            buf[start + i] += sample.toFloat()
        }
    }

    private fun addLeadNote(buf: FloatArray, start: Int, durSamples: Int, freq: Double, gain: Float) {
        val len = min(buf.size - start, durSamples)
        var phase = 0.0
        for (i in 0 until len) {
            val progress = i.toDouble() / len
            val vib = 1.0 + 0.004 * sin(2.0 * PI * 5.5 * (i.toDouble() / SAMPLE_RATE))
            phase += (freq * vib) / SAMPLE_RATE
            val p = phase - phase.toInt()
            val square = if (p < 0.42) 1.0 else -1.0
            val env = min(1.0, i / 160.0) * (1.0 - progress * 0.75)
            buf[start + i] += (square * env * gain).toFloat()
        }
    }

    private fun addVocalNote(
        buf: FloatArray,
        start: Int,
        durSamples: Int,
        freq: Double,
        isBf: Boolean,
        gain: Float
    ) {
        val len = min(buf.size - start, durSamples)
        var phase = 0.0
        val f1 = if (isBf) 720.0 else 440.0
        val f2 = if (isBf) 1850.0 else 1080.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / len
            // Slight FNF pitch glide at attack
            val glide = if (t < 0.028) (if (isBf) 0.90 + 0.10 * (t / 0.028) else 1.06 - 0.06 * (t / 0.028)) else 1.0
            val vibrato = 1.0 + 0.007 * sin(2.0 * PI * 6.0 * t)
            phase += (freq * glide * vibrato) / SAMPLE_RATE
            val p = phase - phase.toInt()
            // Harmonic pulse + formant resonance
            val carrier = (sin(2.0 * PI * p) * 0.6 + sin(2.0 * PI * p * 2.0) * 0.3 + sin(2.0 * PI * p * 3.0) * 0.2)
            val formant = 0.65 * sin(2.0 * PI * f1 * (p / freq.coerceAtLeast(40.0))) +
                0.35 * sin(2.0 * PI * f2 * (p / freq.coerceAtLeast(40.0)))
            val raw = carrier * 0.75 + formant * 0.25
            val env = min(1.0, i / 180.0) * min(1.0, (len - i) / 260.0) * (1.0 - progress * 0.25)
            buf[start + i] += (raw * env * gain).toFloat()
        }
    }

    fun computePeaksFromPcm(pcm: ShortArray, barCount: Int): List<Float> {
        if (pcm.isEmpty() || barCount <= 0) return List(barCount.coerceAtLeast(1)) { 0.1f }
        val chunk = max(1, pcm.size / barCount)
        return List(barCount) { idx ->
            val from = idx * chunk
            val to = min(pcm.size, from + chunk)
            var peak = 0f
            val step = max(1, (to - from) / 64)
            var i = from
            while (i < to) {
                val amp = abs(pcm[i].toInt()) / 32768f
                if (amp > peak) peak = amp
                i += step
            }
            peak.coerceIn(0.05f, 1f)
        }
    }

    fun writeWavFile(pcm: ShortArray, outFile: File) {
        outFile.parentFile?.mkdirs()
        val byteLen = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + byteLen)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(byteLen)
        }.array()

        FileOutputStream(outFile).use { fos ->
            fos.write(header)
            val dataBuf = ByteBuffer.allocate(byteLen).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) dataBuf.putShort(s)
            fos.write(dataBuf.array())
        }
    }

    data class ImportedAudioAnalysis(
        val durationMs: Long,
        val waveformPeaks: List<Float>
    )

    suspend fun analyzeImportedAudio(context: Context, filePath: String): ImportedAudioAnalysis =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            var durationMs = 30_000L
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 30_000L
                retriever.release()
            }

            val peaks = extractWaveformPeaks(file.absolutePath, 96) ?: List(96) { idx ->
                val v = 0.25f + 0.55f * abs(sin(idx * 0.42).toFloat())
                v.coerceIn(0.1f, 0.95f)
            }
            ImportedAudioAnalysis(durationMs, peaks)
        }

    private fun extractWaveformPeaks(path: String, bars: Int): List<Float>? {
        return runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(path)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = f
                    break
                }
            }
            if (audioTrack < 0 || format == null) {
                extractor.release()
                return null
            }
            extractor.selectTrack(audioTrack)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val maxSamples = 250_000 // sample cap for fast waveform scan
            val samples = ArrayList<Short>(4096)
            var inputDone = false
            var outputDone = false
            var strideCounter = 0

            while (!outputDone && samples.size < maxSamples) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val read = extractor.readSampleData(inBuf, 0)
                        if (read < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining() && samples.size < maxSamples) {
                            val s = shortBuf.get()
                            if (strideCounter++ % 16 == 0) {
                                samples.add(s)
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }
            codec.stop()
            codec.release()
            extractor.release()

            if (samples.isEmpty()) return null
            computePeaksFromPcm(samples.toShortArray(), bars)
        }.getOrNull()
    }
}

class SongPlaybackController(private val scope: CoroutineScope) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var instPlayer: MediaPlayer? = null
    private var voicesPlayer: MediaPlayer? = null
    private var playJob: Job? = null
    private var startSystemMs: Long = 0L
    private var baseOffsetMs: Long = 0L
    private var totalDurationMs: Long = 30_000L

    fun playSong(song: SongProject, startAtMs: Long = 0L, onCompleted: () -> Unit = {}) {
        stop()
        totalDurationMs = AudioSynthEngine.songTotalDurationMs(song)
        baseOffsetMs = startAtMs.coerceIn(0L, totalDurationMs)
        _positionMs.value = baseOffsetMs

        if (song.audioSourceMode == AudioSourceMode.IMPORTED_AUDIO && !song.importedInstPath.isNullOrBlank()) {
            startImportedPlayback(song.importedInstPath, song.importedVoicesPath, baseOffsetMs, onCompleted)
        } else {
            startSynthPlayback(song, baseOffsetMs, onCompleted)
        }
    }

    private fun startSynthPlayback(song: SongProject, startAtMs: Long, onCompleted: () -> Unit) {
        _isPlaying.value = true
        playJob = scope.launch(Dispatchers.Default) {
            val stems = AudioSynthEngine.renderComposerStems(song.composerData, song.bpm)
            val pcm = stems.mixPcm
            val startSample = ((startAtMs / 1000.0) * AudioSynthEngine.SAMPLE_RATE).toInt()
                .coerceIn(0, max(0, pcm.size - 1))

            val minBuf = AudioTrack.getMinBufferSize(
                AudioSynthEngine.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(AudioSynthEngine.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            audioTrack = track
            track?.play()
            startSystemMs = System.currentTimeMillis()

            val chunkSize = 2048
            var offset = startSample
            val updater = launch {
                while (isActive && _isPlaying.value) {
                    val elapsed = System.currentTimeMillis() - startSystemMs
                    val pos = (startAtMs + elapsed).coerceAtMost(totalDurationMs)
                    _positionMs.value = pos
                    delay(16L)
                }
            }

            while (isActive && _isPlaying.value && offset < pcm.size) {
                val count = min(chunkSize, pcm.size - offset)
                if (track != null) {
                    track.write(pcm, offset, count)
                } else {
                    delay((count * 1000L) / AudioSynthEngine.SAMPLE_RATE)
                }
                offset += count
            }

            updater.cancel()
            withContext(Dispatchers.Main) {
                if (_isPlaying.value) {
                    stop()
                    onCompleted()
                }
            }
        }
    }

    private fun startImportedPlayback(
        instPath: String,
        voicesPath: String?,
        startAtMs: Long,
        onCompleted: () -> Unit
    ) {
        val instFile = File(instPath)
        if (!instFile.exists()) {
            _isPlaying.value = false
            return
        }
        runCatching {
            val mpInst = MediaPlayer().apply {
                setDataSource(instFile.absolutePath)
                prepare()
                seekTo(startAtMs.toInt())
                setOnCompletionListener {
                    stop()
                    onCompleted()
                }
            }
            instPlayer = mpInst

            val mpVoices = if (!voicesPath.isNullOrBlank() && File(voicesPath).exists()) {
                MediaPlayer().apply {
                    setDataSource(File(voicesPath).absolutePath)
                    prepare()
                    seekTo(startAtMs.toInt())
                }
            } else null
            voicesPlayer = mpVoices

            mpInst.start()
            mpVoices?.start()
            _isPlaying.value = true

            playJob = scope.launch {
                while (isActive && _isPlaying.value) {
                    val pos = runCatching { instPlayer?.currentPosition?.toLong() }.getOrNull() ?: 0L
                    _positionMs.value = pos
                    delay(16L)
                }
            }
        }.onFailure {
            stop()
        }
    }

    fun playOneShot(trackIndex: Int, noteValue: Int) {
        scope.launch(Dispatchers.Default) {
            val pcm = AudioSynthEngine.renderPreviewHit(trackIndex, noteValue)
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(AudioSynthEngine.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                delay(280L)
                track.stop()
                track.release()
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        playJob?.cancel()
        playJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        runCatching {
            instPlayer?.stop()
            instPlayer?.release()
        }
        instPlayer = null
        runCatching {
            voicesPlayer?.stop()
            voicesPlayer?.release()
        }
        voicesPlayer = null
    }
}
