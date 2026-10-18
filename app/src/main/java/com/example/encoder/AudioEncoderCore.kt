package com.example.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

class AudioEncoderCore(
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    bitRate: Int = 128000,
    private val muxer: MediaMuxer
) {
    private val bufferInfo = MediaCodec.BufferInfo()
    private val encoder: MediaCodec
    var trackIndex = -1
        private set
    var isMuxerStarted = false

    init {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
    }

    /**
     * Feeds PCM 16-bit short samples into the audio encoder.
     */
    fun feedPcmData(pcmSamples: ShortArray, offset: Int, count: Int, presentationTimeUs: Long, isEndOfStream: Boolean) {
        val inputBufferIndex = encoder.dequeueInputBuffer(10000L)
        if (inputBufferIndex >= 0) {
            val inputBuffer: ByteBuffer? = encoder.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()

            val bytesToWrite = count * 2
            if (inputBuffer != null && bytesToWrite > 0) {
                for (i in 0 until count) {
                    val sample = pcmSamples[offset + i]
                    inputBuffer.put((sample.toInt() and 0xFF).toByte())
                    inputBuffer.put(((sample.toInt() shr 8) and 0xFF).toByte())
                }
            }

            val flags = if (isEndOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(inputBufferIndex, 0, bytesToWrite, presentationTimeUs, flags)
        }
    }

    /**
     * Drains available AAC output buffers into the MediaMuxer.
     */
    fun drainEncoder(endOfStream: Boolean, checkMuxerReady: () -> Boolean) {
        val timeoutUs = if (endOfStream) 10000L else 0L

        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (trackIndex >= 0) {
                    throw RuntimeException("audio format changed twice")
                }
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                if (checkMuxerReady()) {
                    muxer.start()
                    isMuxerStarted = true
                }
            } else if (encoderStatus < 0) {
                // Ignore unexpected status
            } else {
                val encodedData: ByteBuffer? = encoder.getOutputBuffer(encoderStatus)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        if (!isMuxerStarted) {
                            throw RuntimeException("muxer hasn't started for audio")
                        }
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(encoderStatus, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    fun release() {
        try {
            encoder.stop()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            encoder.release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
