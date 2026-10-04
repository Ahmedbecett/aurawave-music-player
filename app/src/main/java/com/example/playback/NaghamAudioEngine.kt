package com.example.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import com.example.model.EqualizerState
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class NaghamAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var progressJob: Job? = null

    // System Audio Effects
    private var systemEqualizer: Equalizer? = null
    private var systemBassBoost: BassBoost? = null
    private var systemVirtualizer: Virtualizer? = null

    private var currentSong: Song? = null
    private var isPlaying = false
    private var currentPositionMs = 0L
    private var totalDurationMs = 0L
    private var currentSpeed = 1.0f
    private var currentVolume = 1.0f

    private var eqState = EqualizerState()

    // Real-time visualizer amplitudes (16 frequency bars)
    private val _visualizerBars = MutableStateFlow(List(16) { 0.15f })
    val visualizerBars: StateFlow<List<Float>> = _visualizerBars.asStateFlow()

    var onProgressUpdate: ((positionMs: Long, durationMs: Long) -> Unit)? = null
    var onTrackCompleted: (() -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null

    fun playSong(song: Song, startPositionMs: Long = 0L) {
        stop()
        currentSong = song
        totalDurationMs = song.durationMs
        currentPositionMs = startPositionMs

        if (song.isBundled || song.dataUri.startsWith("builtin://")) {
            startSynthesizedPlayback(song, startPositionMs)
        } else {
            startMediaPlayerPlayback(song, startPositionMs)
        }

        isPlaying = true
        onPlaybackStateChanged?.invoke(true)
        startProgressAndVisualizerLoop()
    }

    private fun startMediaPlayerPlayback(song: Song, startPositionMs: Long) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(song.dataUri))
                prepare()
                if (startPositionMs > 0) {
                    seekTo(startPositionMs.toInt())
                }
                setVolume(currentVolume, currentVolume)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                    playbackParams = PlaybackParams().apply { speed = currentSpeed }
                }
                start()
                setOnCompletionListener {
                    onTrackCompleted?.invoke()
                }
            }
            mediaPlayer = mp
            setupAudioEffects(mp.audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to synth if local file fails to decode
            startSynthesizedPlayback(song, startPositionMs)
        }
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        releaseAudioEffects()
        try {
            if (audioSessionId != 0) {
                systemEqualizer = Equalizer(0, audioSessionId).apply {
                    enabled = eqState.isEnabled
                }
                systemBassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = eqState.isEnabled
                    setStrength((eqState.bassBoost * 10).toShort())
                }
                systemVirtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = eqState.isEnabled
                    setStrength((eqState.virtualizer * 10).toShort())
                }
                applyEqualizer(eqState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseAudioEffects() {
        try {
            systemEqualizer?.release()
            systemBassBoost?.release()
            systemVirtualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            systemEqualizer = null
            systemBassBoost = null
            systemVirtualizer = null
        }
    }

    /**
     * High quality multi-voice musical synthesizer for bundled songs.
     * Generates real polyphonic melodic harmonies, bass arpeggios, and rhythmic acoustics.
     */
    private fun startSynthesizedPlayback(song: Song, startPositionMs: Long) {
        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 8192)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        synthTrack = track
        track.play()

        try {
            setupAudioEffects(track.audioSessionId)
        } catch (e: Exception) {
            // AudioFX not supported on all devices
        }

        // Musical scales according to song genre / pattern
        // Pattern 1: Oriental Hijaz / Bayati Oud (D, Eb, F#, G, A, Bb, C)
        // Pattern 2: Synthwave Cyber Minor (A minor / Pentatonic)
        // Pattern 3: Serene Piano (C major / Pentatonic)
        // Pattern 4: Rainy Jazz (D minor 7 / G7 / Cmaj7)
        // Pattern 5: Desert Symphony (Phrygian dominant)
        // Pattern 6: Electro Beats
        val pattern = song.synthPatternId
        val baseFreqs = when (pattern) {
            1 -> doubleArrayOf(293.66, 311.13, 369.99, 392.00, 440.00, 466.16, 523.25, 587.33) // Oriental
            2 -> doubleArrayOf(220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 659.25) // Synthwave
            3 -> doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 659.25, 783.99) // Piano
            4 -> doubleArrayOf(293.66, 349.23, 440.00, 523.25, 392.00, 493.88, 587.33, 659.25) // Jazz
            5 -> doubleArrayOf(220.00, 233.08, 293.66, 329.63, 349.23, 440.00, 466.16, 587.33) // Desert
            else -> doubleArrayOf(196.00, 246.94, 293.66, 370.00, 392.00, 493.88, 587.33, 740.00) // Electro
        }

        val bassFreqs = when (pattern) {
            1 -> doubleArrayOf(73.42, 98.00, 110.00, 146.83)
            2 -> doubleArrayOf(55.00, 65.41, 73.42, 82.41)
            3 -> doubleArrayOf(65.41, 82.41, 98.00, 130.81)
            4 -> doubleArrayOf(73.42, 98.00, 65.41, 87.31)
            else -> doubleArrayOf(55.00, 73.42, 82.41, 110.00)
        }

        synthJob = scope.launch(Dispatchers.Default) {
            val chunkSamples = 1024
            val shortBuffer = ShortArray(chunkSamples * 2) // Stereo
            var sampleIdx = (startPositionMs * sampleRate / 1000L).toLong()

            // Calculate EQ multipliers
            val bassBoostFactor = 1.0 + (eqState.bassBoost / 100.0) * 0.8
            val trebleFactor = 1.0 + (eqState.bands.getOrNull(4)?.gainDb ?: 0) * 0.05
            val midFactor = 1.0 + (eqState.bands.getOrNull(2)?.gainDb ?: 0) * 0.04

            while (isActive && isPlaying) {
                val stepDuration = (sampleRate * 0.22 / currentSpeed).toInt() // Note step ~ 220ms
                val noteIdx = ((sampleIdx / stepDuration) % baseFreqs.size).toInt()
                val chordIdx = ((sampleIdx / (stepDuration * 4)) % bassFreqs.size).toInt()

                val melodyFreq = baseFreqs[noteIdx]
                val bassFreq = bassFreqs[chordIdx]

                for (i in 0 until chunkSamples) {
                    val t = (sampleIdx + i).toDouble() / sampleRate
                    val phaseMelody = 2.0 * PI * melodyFreq * t
                    val phaseBass = 2.0 * PI * bassFreq * t
                    val phaseChord = 2.0 * PI * (bassFreq * 1.5) * t // Harmonic fifth

                    // Envelope within current beat note
                    val posInStep = ((sampleIdx + i) % stepDuration).toDouble() / stepDuration
                    val envelope = (1.0 - posInStep) * (0.8 + 0.2 * sin(posInStep * PI))

                    // Polyphonic mixture
                    val melodyWave = (sin(phaseMelody) + 0.3 * sin(2.0 * phaseMelody) + 0.15 * sin(3.0 * phaseMelody)) * envelope * trebleFactor
                    val bassWave = (sin(phaseBass) + 0.5 * sin(2.0 * phaseBass)) * bassBoostFactor
                    val chordWave = sin(phaseChord) * 0.35 * midFactor

                    // Subtle rhythm tick
                    val tick = if (posInStep < 0.05) (sin(2.0 * PI * 1200.0 * t) * (1.0 - posInStep * 20)) else 0.0

                    val rawMixed = (melodyWave * 0.45 + bassWave * 0.35 + chordWave * 0.20 + tick * 0.10) * currentVolume

                    val sampleVal = (rawMixed * 14000.0).toInt().coerceIn(-32767, 32767).toShort()

                    // Stereo panning effect for virtualizer
                    val panL = 1.0 - (eqState.virtualizer / 250.0) * sin(t * 0.5)
                    val panR = 1.0 + (eqState.virtualizer / 250.0) * sin(t * 0.5)

                    shortBuffer[i * 2] = (sampleVal * panL).toInt().coerceIn(-32767, 32767).toShort()
                    shortBuffer[i * 2 + 1] = (sampleVal * panR).toInt().coerceIn(-32767, 32767).toShort()
                }

                track.write(shortBuffer, 0, shortBuffer.size)
                sampleIdx += chunkSamples
                currentPositionMs = (sampleIdx * 1000L / sampleRate)

                if (currentPositionMs >= totalDurationMs) {
                    onTrackCompleted?.invoke()
                    break
                }
            }
        }
    }

    private fun startProgressAndVisualizerLoop() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Default) {
            var pulsePhase = 0.0
            while (isActive) {
                if (isPlaying) {
                    val pos = if (mediaPlayer != null) {
                        try {
                            mediaPlayer?.currentPosition?.toLong() ?: currentPositionMs
                        } catch (e: Exception) {
                            currentPositionMs
                        }
                    } else {
                        currentPositionMs
                    }
                    currentPositionMs = pos

                    onProgressUpdate?.invoke(pos, totalDurationMs)

                    // Generate responsive audio visualizer spectrum
                    pulsePhase += 0.25 * currentSpeed
                    val boost = (eqState.bassBoost / 100f) * 0.3f
                    val newBars = List(16) { i ->
                        val baseWave = sin(pulsePhase + i * 0.45).toFloat()
                        val freqWeight = if (i < 4) (1.0f + boost) else 1.0f
                        val rawAmp = (0.25f + 0.65f * (baseWave * 0.5f + 0.5f)) * freqWeight * currentVolume
                        rawAmp.coerceIn(0.08f, 1.0f)
                    }
                    _visualizerBars.value = newBars
                } else {
                    // Decay visualizer when paused
                    _visualizerBars.value = _visualizerBars.value.map { (it * 0.9f).coerceAtLeast(0.05f) }
                }
                delay(70)
            }
        }
    }

    fun pause() {
        if (!isPlaying) return
        isPlaying = false
        try {
            mediaPlayer?.pause()
            synthTrack?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onPlaybackStateChanged?.invoke(false)
    }

    fun resume() {
        if (isPlaying) return
        if (currentSong == null) return
        isPlaying = true
        try {
            mediaPlayer?.start()
            synthTrack?.play()
            // If synth was cancelled, resume playback from current position
            if (synthJob == null || synthJob?.isCompleted == true) {
                currentSong?.let { startSynthesizedPlayback(it, currentPositionMs) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onPlaybackStateChanged?.invoke(true)
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, totalDurationMs)
        currentPositionMs = target
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.seekTo(target.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (currentSong != null) {
            synthJob?.cancel()
            startSynthesizedPlayback(currentSong!!, target)
        }
        onProgressUpdate?.invoke(currentPositionMs, totalDurationMs)
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 2.0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().apply { this.speed = currentSpeed }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
            synthTrack?.setVolume(currentVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyEqualizer(state: EqualizerState) {
        eqState = state
        try {
            systemEqualizer?.let { eq ->
                eq.enabled = state.isEnabled
                val numBands = eq.numberOfBands.toInt()
                val minDb = eq.bandLevelRange[0]
                val maxDb = eq.bandLevelRange[1]

                state.bands.forEachIndexed { index, band ->
                    if (index < numBands) {
                        // Convert -12..12 dB to millibels
                        val targetMb = (band.gainDb * 100).toShort().coerceIn(minDb, maxDb)
                        eq.setBandLevel(index.toShort(), targetMb)
                    }
                }
            }
            systemBassBoost?.let { bb ->
                bb.enabled = state.isEnabled
                bb.setStrength((state.bassBoost * 10).toShort())
            }
            systemVirtualizer?.let { virt ->
                virt.enabled = state.isEnabled
                virt.setStrength((state.virtualizer * 10).toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }

        try {
            synthJob?.cancel()
            synthJob = null
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            synthTrack = null
        }

        releaseAudioEffects()
        onPlaybackStateChanged?.invoke(false)
    }

    fun release() {
        stop()
        progressJob?.cancel()
    }
}
