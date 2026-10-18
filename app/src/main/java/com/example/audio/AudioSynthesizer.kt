package com.example.audio

import com.example.model.AudioConfig
import com.example.model.McqQuestion
import com.example.model.TimingConfig
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioSynthesizer {
    const val SAMPLE_RATE = 44100

    /**
     * Crisp studio countdown tick (approx 30ms).
     */
    fun generateTickPcm(sampleRate: Int = SAMPLE_RATE, volume: Float = 0.8f): ShortArray {
        val durationMs = 35
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val freq1 = 1250.0
        val freq2 = 2500.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 120.0)
            val sample = (sin(2 * PI * freq1 * t) * 0.7 + sin(2 * PI * freq2 * t) * 0.3) * decay
            val clamped = (sample * volume * 32767.0).coerceIn(-32768.0, 32767.0)
            pcm[i] = clamped.toInt().toShort()
        }
        return pcm
    }

    /**
     * Resonant success chime triad (C-major harmonic: C5, E5, G5, C6) (approx 1000ms).
     */
    fun generateChimePcm(sampleRate: Int = SAMPLE_RATE, volume: Float = 1.0f): ShortArray {
        val durationMs = 1100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)

        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val weights = doubleArrayOf(0.35, 0.30, 0.25, 0.15)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.008).coerceAtMost(1.0)
            val decay = exp(-t * 3.2)
            var sample = 0.0
            for (f in freqs.indices) {
                sample += sin(2 * PI * freqs[f] * t) * weights[f]
            }
            val enveloped = sample * attack * decay * volume
            val clamped = (enveloped * 32767.0).coerceIn(-32768.0, 32767.0)
            pcm[i] = clamped.toInt().toShort()
        }
        return pcm
    }

    /**
     * Smooth page transition whoosh (approx 280ms).
     */
    fun generateWhooshPcm(sampleRate: Int = SAMPLE_RATE, volume: Float = 0.7f): ShortArray {
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)

        var lastNoise = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Window envelope (bell curve)
            val envelope = sin(PI * progress)
            // Filtered white noise with frequency sweep
            val white = (Math.random() * 2.0 - 1.0)
            val centerFreq = 300.0 + 800.0 * sin(PI * progress)
            val rc = 1.0 / (2 * PI * centerFreq)
            val dt = 1.0 / sampleRate
            val alpha = dt / (rc + dt)
            lastNoise += alpha * (white - lastNoise)

            val sample = lastNoise * envelope * volume
            val clamped = (sample * 32767.0).coerceIn(-32768.0, 32767.0)
            pcm[i] = clamped.toInt().toShort()
        }
        return pcm
    }

    /**
     * Generates a complete continuous mono PCM short array for the entire quiz video.
     */
    fun generateFullAudioTrack(
        questions: List<McqQuestion>,
        timing: TimingConfig,
        audioConfig: AudioConfig
    ): ShortArray {
        val totalDurationSeconds = questions.size * timing.totalQuestionDuration
        val totalSamples = (totalDurationSeconds * SAMPLE_RATE).toInt()
        val masterPcm = ShortArray(totalSamples)

        if (!audioConfig.enableTicks && !audioConfig.enableChime && !audioConfig.enableWhoosh) {
            return masterPcm // Silent
        }

        val tick = if (audioConfig.enableTicks) generateTickPcm(SAMPLE_RATE, audioConfig.tickVolume) else null
        val chime = if (audioConfig.enableChime) generateChimePcm(SAMPLE_RATE, audioConfig.chimeVolume) else null
        val whoosh = if (audioConfig.enableWhoosh) generateWhooshPcm(SAMPLE_RATE, audioConfig.whooshVolume) else null

        val questionDuration = timing.totalQuestionDuration
        val countdownDuration = timing.countdownSeconds
        val answerHoldDuration = timing.answerHoldSeconds

        questions.forEachIndexed { qIdx, _ ->
            val qStartTime = qIdx * questionDuration

            // 1. Countdown ticks
            if (tick != null) {
                val totalTicks = countdownDuration.toInt()
                for (s in 1..totalTicks) {
                    val tickTime = qStartTime + (s - 1)
                    val tickSampleIndex = (tickTime * SAMPLE_RATE).toInt()
                    mixPcm(masterPcm, tick, tickSampleIndex)
                }
            }

            // 2. Reveal chime (triggers exactly when countdown ends)
            if (chime != null) {
                val chimeTime = qStartTime + countdownDuration
                val chimeSampleIndex = (chimeTime * SAMPLE_RATE).toInt()
                mixPcm(masterPcm, chime, chimeSampleIndex)
            }

            // 3. Whoosh transition (triggers when transition begins)
            if (whoosh != null && qIdx < questions.size - 1) {
                val whooshTime = qStartTime + countdownDuration + answerHoldDuration
                val whooshSampleIndex = (whooshTime * SAMPLE_RATE).toInt()
                mixPcm(masterPcm, whoosh, whooshSampleIndex)
            }
        }

        return masterPcm
    }

    private fun mixPcm(master: ShortArray, source: ShortArray, startIndex: Int) {
        val end = minOf(master.size, startIndex + source.size)
        var sIdx = 0
        for (mIdx in startIndex until end) {
            if (mIdx >= 0) {
                val sum = master[mIdx] + source[sIdx]
                master[mIdx] = sum.coerceIn(-32768, 32767).toShort()
            }
            sIdx++
        }
    }
}
