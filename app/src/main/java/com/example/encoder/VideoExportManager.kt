package com.example.encoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMuxer
import android.os.Environment
import com.example.audio.AudioSynthesizer
import com.example.model.McqQuestion
import com.example.model.ProjectConfig
import com.example.model.VideoResolution
import com.example.renderer.FrameRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class ExportProgress(
    val currentFrame: Int = 0,
    val totalFrames: Int = 1,
    val percentage: Int = 0,
    val currentQuestion: Int = 1,
    val totalQuestions: Int = 1,
    val phase: String = "Initializing...",
    val fpsEstimate: Float = 0f,
    val elapsedTimeSec: Float = 0f,
    val etaSeconds: Float = 0f
)

data class ExportResult(
    val file: File,
    val durationSeconds: Float,
    val fileSizeBytes: Long,
    val resolution: String,
    val wasEarlySaved: Boolean
)

class VideoExportManager(private val context: Context) {
    private val isCancelled = AtomicBoolean(false)
    private val isStopEarly = AtomicBoolean(false)

    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress

    fun cancel() {
        isCancelled.set(true)
    }

    fun stopAndSaveEarly() {
        isStopEarly.set(true)
    }

    suspend fun exportVideo(
        questions: List<McqQuestion>,
        config: ProjectConfig,
        sideImageBitmap: Bitmap? = null
    ): ExportResult = withContext(Dispatchers.Default) {
        isCancelled.set(false)
        isStopEarly.set(false)

        val timing = config.timing
        val qDuration = timing.totalQuestionDuration
        val totalExpectedDuration = questions.size * qDuration
        val isLongVideo = totalExpectedDuration > 600f // 10+ minutes

        // Long Video Safe Mode resolution & bitrate adjustment
        val targetResolution = if (config.export.longVideoSafeMode && isLongVideo) {
            VideoResolution.HD_720P
        } else {
            config.export.resolution
        }

        val targetBitrate = if (config.export.longVideoSafeMode && isLongVideo) {
            minOf(config.export.bitrateMbps, 6) * 1_000_000
        } else {
            config.export.bitrateMbps * 1_000_000
        }

        val width = targetResolution.width
        val height = targetResolution.height
        val fps = timing.fps
        val totalFrames = (totalExpectedDuration * fps).toInt()

        // Prepare output MP4 file
        val outputDir = File(context.cacheDir, "videos").apply { mkdirs() }
        val outputFile = File(outputDir, "MCQ_Quiz_${System.currentTimeMillis()}.mp4")

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // Initialize Encoders and OpenGL Pipeline
        val videoEncoderCore = VideoEncoderCore(
            width = width,
            height = height,
            bitRate = targetBitrate,
            fps = fps,
            muxer = muxer
        )

        val eglCore = EglCore()
        val windowSurface = WindowSurface(eglCore, videoEncoderCore.inputSurface)
        windowSurface.makeCurrent()
        val textureRenderer = TextureRenderer(width, height)

        val hasAudio = config.audio.enableTicks || config.audio.enableChime || config.audio.enableWhoosh
        val audioEncoderCore = if (hasAudio) {
            AudioEncoderCore(
                sampleRate = AudioSynthesizer.SAMPLE_RATE,
                channelCount = 1,
                bitRate = 128000,
                muxer = muxer
            )
        } else null

        // Generate full PCM audio track
        val masterPcm = if (hasAudio) {
            AudioSynthesizer.generateFullAudioTrack(questions, timing, config.audio)
        } else {
            ShortArray(0)
        }

        val reusableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(reusableBitmap)

        var muxerStarted = false
        fun checkMuxerReady(): Boolean {
            val videoReady = videoEncoderCore.trackIndex >= 0
            val audioReady = audioEncoderCore == null || audioEncoderCore.trackIndex >= 0
            if (videoReady && audioReady && !muxerStarted) {
                muxerStarted = true
                videoEncoderCore.isMuxerStarted = true
                audioEncoderCore?.isMuxerStarted = true
                return true
            }
            return false
        }

        // Prime the pipeline with 1 frame and 1 audio chunk to establish tracks
        FrameRenderer.renderFrame(canvas, width, height, questions, config, 0f, sideImageBitmap)
        textureRenderer.loadBitmap(reusableBitmap)
        textureRenderer.draw()
        windowSurface.setPresentationTime(0L)
        windowSurface.swapBuffers()
        videoEncoderCore.drainEncoder(false) { checkMuxerReady() }

        val pcmSamplesPerFrame = AudioSynthesizer.SAMPLE_RATE / fps
        if (hasAudio && masterPcm.isNotEmpty()) {
            val primeCount = minOf(pcmSamplesPerFrame, masterPcm.size)
            audioEncoderCore?.feedPcmData(masterPcm, 0, primeCount, 0L, false)
            audioEncoderCore?.drainEncoder(false) { checkMuxerReady() }
        }

        // Ensure muxer is started
        var waitLoops = 0
        while (!muxerStarted && waitLoops < 50) {
            videoEncoderCore.drainEncoder(false) { checkMuxerReady() }
            audioEncoderCore?.drainEncoder(false) { checkMuxerReady() }
            waitLoops++
        }

        val startTimeMs = System.currentTimeMillis()
        var renderedFrames = 1
        var audioPcmOffset = pcmSamplesPerFrame

        try {
            for (frame in 1 until totalFrames) {
                if (isCancelled.get()) {
                    throw RuntimeException("Export cancelled by user")
                }
                if (isStopEarly.get()) {
                    // Break loop early to finalize video up to this frame
                    break
                }

                val timeSeconds = frame.toFloat() / fps
                val ptsNs = frame * (1_000_000_000L / fps)
                val qIdx = (timeSeconds / qDuration).toInt().coerceIn(0, questions.lastIndex)
                val timeInQ = timeSeconds - (qIdx * qDuration)

                val phase = when {
                    timeInQ < timing.countdownSeconds -> "Thinking / Countdown"
                    timeInQ < timing.countdownSeconds + timing.answerHoldSeconds -> "Answer Revealed"
                    else -> "Transitioning"
                }

                // Render Canvas onto Reusable Bitmap
                FrameRenderer.renderFrame(canvas, width, height, questions, config, timeSeconds, sideImageBitmap)

                // Upload to Texture and Draw to Surface
                textureRenderer.loadBitmap(reusableBitmap)
                textureRenderer.draw()
                windowSurface.setPresentationTime(ptsNs)
                windowSurface.swapBuffers()

                // Drain Video Encoder
                videoEncoderCore.drainEncoder(false) { checkMuxerReady() }

                // Feed Audio Segment for this frame
                if (hasAudio && audioPcmOffset < masterPcm.size) {
                    val count = minOf(pcmSamplesPerFrame, masterPcm.size - audioPcmOffset)
                    val ptsUs = (frame * 1_000_000L) / fps
                    audioEncoderCore?.feedPcmData(masterPcm, audioPcmOffset, count, ptsUs, false)
                    audioEncoderCore?.drainEncoder(false) { checkMuxerReady() }
                    audioPcmOffset += count
                }

                renderedFrames++

                // Update Progress and ETA
                if (frame % 5 == 0 || frame == totalFrames - 1) {
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    val elapsedSec = elapsedMs / 1000f
                    val currentFps = (renderedFrames / elapsedSec).coerceAtLeast(1f)
                    val remainingFrames = totalFrames - renderedFrames
                    val etaSec = remainingFrames / currentFps
                    val pct = ((renderedFrames.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)

                    _progress.value = ExportProgress(
                        currentFrame = renderedFrames,
                        totalFrames = totalFrames,
                        percentage = pct,
                        currentQuestion = qIdx + 1,
                        totalQuestions = questions.size,
                        phase = "Q${qIdx + 1}: $phase",
                        fpsEstimate = currentFps,
                        elapsedTimeSec = elapsedSec,
                        etaSeconds = etaSec
                    )
                }

                // Keep UI thread responsive during long rendering
                if (frame % 20 == 0) {
                    yield()
                }
            }

            // Finish Video Stream
            videoEncoderCore.signalEndOfInputStream()
            videoEncoderCore.drainEncoder(true) { checkMuxerReady() }

            // Finish Audio Stream
            if (hasAudio && audioEncoderCore != null) {
                audioEncoderCore.feedPcmData(ShortArray(0), 0, 0, 0L, true)
                audioEncoderCore.drainEncoder(true) { checkMuxerReady() }
            }

            if (muxerStarted) {
                muxer.stop()
            }
        } catch (e: Exception) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw e
        } finally {
            reusableBitmap.recycle()
            try { windowSurface.release() } catch (e: Exception) {}
            try { textureRenderer.release() } catch (e: Exception) {}
            try { eglCore.release() } catch (e: Exception) {}
            try { videoEncoderCore.release() } catch (e: Exception) {}
            try { audioEncoderCore?.release() } catch (e: Exception) {}
            try { muxer.release() } catch (e: Exception) {}
        }

        val finalDuration = renderedFrames.toFloat() / fps
        val fileSize = if (outputFile.exists()) outputFile.length() else 0L

        return@withContext ExportResult(
            file = outputFile,
            durationSeconds = finalDuration,
            fileSizeBytes = fileSize,
            resolution = "${width}x${height} @ ${fps}fps",
            wasEarlySaved = isStopEarly.get()
        )
    }
}
