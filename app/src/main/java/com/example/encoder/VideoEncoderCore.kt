package com.example.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.nio.ByteBuffer

class VideoEncoderCore(
    val width: Int,
    val height: Int,
    bitRate: Int,
    val fps: Int = 30,
    private val muxer: MediaMuxer
) {
    private val bufferInfo = MediaCodec.BufferInfo()
    private val encoder: MediaCodec
    val inputSurface: Surface
    var trackIndex = -1
        private set
    var isMuxerStarted = false

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
        }

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        encoder.start()
    }

    /**
     * Drains available encoded output buffers into the MediaMuxer.
     */
    fun drainEncoder(endOfStream: Boolean, checkMuxerReady: () -> Boolean) {
        val timeoutUs = if (endOfStream) 10000L else 0L

        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (trackIndex >= 0) {
                    throw RuntimeException("video format changed twice")
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
                if (encodedData == null) {
                    throw RuntimeException("encoderOutputBuffer $encoderStatus was null")
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!isMuxerStarted) {
                        throw RuntimeException("muxer hasn't started")
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

    fun signalEndOfInputStream() {
        encoder.signalEndOfInputStream()
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
