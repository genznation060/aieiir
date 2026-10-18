package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SoundPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSound(pcmSamples: ShortArray) {
        scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    AudioSynthesizer.SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, pcmSamples.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(AudioSynthesizer.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                audioTrack.write(pcmSamples, 0, pcmSamples.size)
                // Short wait for track to flush
                val durationMs = (pcmSamples.size * 1000L) / AudioSynthesizer.SAMPLE_RATE
                kotlinx.coroutines.delay(durationMs + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Audio preview error ignored safely
            }
        }
    }

    fun playTick(volume: Float = 0.8f) {
        playSound(AudioSynthesizer.generateTickPcm(volume = volume))
    }

    fun playChime(volume: Float = 1.0f) {
        playSound(AudioSynthesizer.generateChimePcm(volume = volume))
    }

    fun playWhoosh(volume: Float = 0.7f) {
        playSound(AudioSynthesizer.generateWhooshPcm(volume = volume))
    }
}
