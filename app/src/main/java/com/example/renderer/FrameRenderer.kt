package com.example.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.ColorTheme
import com.example.model.McqQuestion
import com.example.model.ProjectConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FrameRenderer {
    const val BASE_WIDTH = 1920f
    const val BASE_HEIGHT = 1080f

    // Reusable Paints for extreme performance
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val timerProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tempRect = RectF()
    private val path = Path()

    /**
     * Renders a frame onto the given canvas at target dimensions.
     */
    fun renderFrame(
        canvas: Canvas,
        targetWidth: Int,
        targetHeight: Int,
        questions: List<McqQuestion>,
        config: ProjectConfig,
        globalTimeSeconds: Float,
        sideImageBitmap: Bitmap? = null
    ) {
        if (questions.isEmpty()) {
            drawEmptyState(canvas, targetWidth, targetHeight)
            return
        }

        val timing = config.timing
        val qDuration = timing.totalQuestionDuration
        val totalVideoDuration = questions.size * qDuration
        val clampedTime = globalTimeSeconds.coerceIn(0f, totalVideoDuration)

        val currentQIndex = (clampedTime / qDuration).toInt().coerceIn(0, questions.lastIndex)
        val timeInQuestion = clampedTime - (currentQIndex * qDuration)

        canvas.save()
        // Uniform scaling to base 1920x1080 design canvas
        val scaleX = targetWidth / BASE_WIDTH
        val scaleY = targetHeight / BASE_HEIGHT
        canvas.scale(scaleX, scaleY)

        // Check if we are in transition to the next question
        val transitionStart = timing.countdownSeconds + timing.answerHoldSeconds
        val isInTransition = timeInQuestion >= transitionStart && currentQIndex < questions.lastIndex

        if (isInTransition) {
            val transProgress = (timeInQuestion - transitionStart) / timing.transitionSeconds
            // Slide transition: current slides left, next slides in from right
            val slideOffset = transProgress * BASE_WIDTH

            canvas.save()
            canvas.translate(-slideOffset, 0f)
            drawSingleQuestionScene(
                canvas = canvas,
                question = questions[currentQIndex],
                qIndex = currentQIndex,
                totalQuestions = questions.size,
                config = config,
                timeInQuestion = transitionStart, // Frozen at reveal state
                sideImageBitmap = sideImageBitmap
            )
            canvas.restore()

            canvas.save()
            canvas.translate(BASE_WIDTH - slideOffset, 0f)
            drawSingleQuestionScene(
                canvas = canvas,
                question = questions[currentQIndex + 1],
                qIndex = currentQIndex + 1,
                totalQuestions = questions.size,
                config = config,
                timeInQuestion = 0f, // Initial state
                sideImageBitmap = sideImageBitmap
            )
            canvas.restore()
        } else {
            drawSingleQuestionScene(
                canvas = canvas,
                question = questions[currentQIndex],
                qIndex = currentQIndex,
                totalQuestions = questions.size,
                config = config,
                timeInQuestion = timeInQuestion,
                sideImageBitmap = sideImageBitmap
            )
        }

        canvas.restore()
    }

    private fun drawSingleQuestionScene(
        canvas: Canvas,
        question: McqQuestion,
        qIndex: Int,
        totalQuestions: Int,
        config: ProjectConfig,
        timeInQuestion: Float,
        sideImageBitmap: Bitmap?
    ) {
        val theme = config.style.theme
        val timing = config.timing
        val isRevealed = timeInQuestion >= timing.countdownSeconds

        val bgPrimary = parseColorSafe(theme.bgPrimaryHex, 0xFF0B111E.toInt())
        val bgSecondary = parseColorSafe(theme.bgSecondaryHex, 0xFF051A18.toInt())
        val accentColor = parseColorSafe(config.style.customAccentHex.ifBlank { theme.accentHex }, 0xFF10B981.toInt())
        val cardBg = parseColorSafe(theme.cardBgHex, 0xFF131E2F.toInt())
        val textPrimary = parseColorSafe(theme.textPrimaryHex, 0xFFF8FAFC.toInt())
        val textSecondary = parseColorSafe(theme.textSecondaryHex, 0xFF94A3B8.toInt())

        // 1. Background Gradient
        bgPaint.shader = LinearGradient(
            0f, 0f, BASE_WIDTH, BASE_HEIGHT,
            intArrayOf(bgPrimary, bgSecondary),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, BASE_WIDTH, BASE_HEIGHT, bgPaint)

        // Subtle ambient light overlay behind card
        accentPaint.color = accentColor
        accentPaint.alpha = 15
        canvas.drawCircle(BASE_WIDTH * 0.4f, BASE_HEIGHT * 0.4f, 450f, accentPaint)

        // 2. Top Header & Watermark
        drawHeader(canvas, question, qIndex, totalQuestions, config, accentColor, textPrimary, textSecondary)

        // 3. Overall Top Progress Bar
        if (config.style.showProgressBar) {
            drawTopProgressBar(canvas, timeInQuestion, timing, accentColor)
        }

        // Layout partitioning: Split screen if side image enabled
        val hasSideImage = config.overlay.enableSideImage
        val leftWidth = if (hasSideImage) BASE_WIDTH * (1f - config.overlay.splitRatio) - 40f else BASE_WIDTH - 120f
        val leftStart = 60f

        // 4. Question Card Box
        val questionBoxTop = 150f
        val questionBoxHeight = 240f
        drawQuestionCard(
            canvas = canvas,
            question = question,
            left = leftStart,
            top = questionBoxTop,
            width = leftWidth,
            height = questionBoxHeight,
            cardBg = cardBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = accentColor,
            fontScale = config.style.fontScale
        )

        // 5. Options List
        val optionsTop = questionBoxTop + questionBoxHeight + 30f
        val optionHeight = 90f
        val optionSpacing = 20f

        val correctIdx = question.correctIndex()
        for (i in 0..3) {
            val optY = optionsTop + (i * (optionHeight + optionSpacing))
            val isOptionCorrect = i == correctIdx
            drawOptionItem(
                canvas = canvas,
                letter = ('A' + i).toString(),
                text = question.getOption(i),
                left = leftStart,
                top = optY,
                width = leftWidth,
                height = optionHeight,
                isRevealed = isRevealed,
                isCorrect = isOptionCorrect,
                cardBg = cardBg,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                fontScale = config.style.fontScale
            )
        }

        // 6. Side Image / Media Box (if enabled)
        if (hasSideImage) {
            val sideStart = leftStart + leftWidth + 40f
            val sideWidth = BASE_WIDTH - sideStart - 60f
            drawSideMediaBox(
                canvas = canvas,
                bitmap = sideImageBitmap,
                left = sideStart,
                top = questionBoxTop,
                width = sideWidth,
                height = (optionHeight * 4) + (optionSpacing * 3) + questionBoxHeight + 30f,
                cardBg = cardBg,
                accentColor = accentColor,
                textSecondary = textSecondary,
                placeholderText = config.overlay.fallbackPlaceholderText
            )
        }

        // 7. Circular Countdown Timer or Checkmark Badge (Right Header)
        if (config.style.showCountdownCircle) {
            val timerCenterX = if (hasSideImage) BASE_WIDTH - 120f else BASE_WIDTH - 130f
            val timerCenterY = 95f
            drawCountdownCircle(
                canvas = canvas,
                centerX = timerCenterX,
                centerY = timerCenterY,
                radius = 48f,
                timeInQuestion = timeInQuestion,
                countdownDuration = timing.countdownSeconds,
                isRevealed = isRevealed,
                accentColor = accentColor,
                textPrimary = textPrimary
            )
        }

        // 8. Explanation Card (Slides in during reveal phase)
        if (isRevealed && question.explanation.isNotBlank()) {
            val revealTime = timeInQuestion - timing.countdownSeconds
            val revealAlpha = (revealTime / 0.4f).coerceIn(0f, 1f)
            drawExplanationBanner(
                canvas = canvas,
                explanation = question.explanation,
                left = leftStart,
                bottom = BASE_HEIGHT - 35f,
                width = if (hasSideImage) leftWidth else BASE_WIDTH - 120f,
                alpha = revealAlpha,
                cardBg = cardBg,
                accentColor = accentColor,
                textPrimary = textPrimary
            )
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        question: McqQuestion,
        qIndex: Int,
        totalQuestions: Int,
        config: ProjectConfig,
        accentColor: Int,
        textPrimary: Int,
        textSecondary: Int
    ) {
        val wm = config.watermark
        if (wm.enabled) {
            // Channel Logo / Badge
            val wmLeft = 60f
            val wmTop = 45f

            // Medical cross icon
            accentPaint.color = accentColor
            accentPaint.alpha = (wm.opacity * 255).toInt()
            val crossSize = 24f
            val cx = wmLeft + 14f
            val cy = wmTop + 14f
            canvas.drawRect(cx - 3f, cy - 12f, cx + 3f, cy + 12f, accentPaint)
            canvas.drawRect(cx - 12f, cy - 3f, cx + 12f, cy + 3f, accentPaint)

            // Watermark title
            textPaint.apply {
                color = textPrimary
                alpha = (wm.opacity * 255).toInt()
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.drawText(wm.title, wmLeft + 38f, wmTop + 18f, textPaint)

            // Watermark subtitle
            if (wm.subtitle.isNotBlank()) {
                textPaint.apply {
                    color = textSecondary
                    alpha = (wm.opacity * 200).toInt()
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    letterSpacing = 0.04f
                }
                canvas.drawText(wm.subtitle, wmLeft + 38f, wmTop + 42f, textPaint)
            }
        }

        // Question count badge (e.g. "QUESTION 01 OF 05")
        val badgeX = 550f
        val badgeY = 65f
        cardPaint.color = 0xFF1E293B.toInt()
        tempRect.set(badgeX, badgeY - 26f, badgeX + 210f, badgeY + 14f)
        canvas.drawRoundRect(tempRect, 18f, 18f, cardPaint)

        borderPaint.color = accentColor
        borderPaint.alpha = 80
        borderPaint.strokeWidth = 2f
        canvas.drawRoundRect(tempRect, 18f, 18f, borderPaint)

        textPaint.apply {
            color = accentColor
            alpha = 255
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val qLabel = String.format("QUESTION %02d / %02d", qIndex + 1, totalQuestions)
        canvas.drawText(qLabel, badgeX + 16f, badgeY - 1f, textPaint)

        // Category Tag
        if (config.style.showCategoryBadge && question.category.isNotBlank()) {
            val catX = badgeX + 230f
            textPaint.apply {
                color = textSecondary
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
            }
            canvas.drawText("•  ${question.category.uppercase()}", catX, badgeY - 1f, textPaint)
        }
    }

    private fun drawTopProgressBar(
        canvas: Canvas,
        timeInQuestion: Float,
        timing: com.example.model.TimingConfig,
        accentColor: Int
    ) {
        val barY = 120f
        val barHeight = 6f
        val barWidth = BASE_WIDTH - 120f
        val barLeft = 60f

        // Track
        cardPaint.color = 0x22FFFFFF
        tempRect.set(barLeft, barY, barLeft + barWidth, barY + barHeight)
        canvas.drawRoundRect(tempRect, 3f, 3f, cardPaint)

        // Progress
        val progress = if (timeInQuestion < timing.countdownSeconds) {
            1f - (timeInQuestion / timing.countdownSeconds)
        } else {
            0f
        }
        val activeWidth = barWidth * progress.coerceIn(0f, 1f)
        if (activeWidth > 0f) {
            accentPaint.color = accentColor
            accentPaint.alpha = 255
            tempRect.set(barLeft, barY, barLeft + activeWidth, barY + barHeight)
            canvas.drawRoundRect(tempRect, 3f, 3f, accentPaint)
        }
    }

    private fun drawQuestionCard(
        canvas: Canvas,
        question: McqQuestion,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        cardBg: Int,
        textPrimary: Int,
        textSecondary: Int,
        accentColor: Int,
        fontScale: Float
    ) {
        // Outer Card Shadow / Glow
        tempRect.set(left, top, left + width, top + height)
        cardPaint.color = cardBg
        cardPaint.alpha = 245
        canvas.drawRoundRect(tempRect, 22f, 22f, cardPaint)

        // Left accent indicator stripe
        accentPaint.color = accentColor
        accentPaint.alpha = 255
        path.reset()
        val r = 22f
        tempRect.set(left, top, left + 10f, top + height)
        canvas.drawRoundRect(tempRect, 5f, 5f, accentPaint)

        // Border
        borderPaint.color = 0x33475569
        borderPaint.strokeWidth = 2f
        tempRect.set(left, top, left + width, top + height)
        canvas.drawRoundRect(tempRect, 22f, 22f, borderPaint)

        // Question Text (Multi-line StaticLayout)
        textPaint.apply {
            color = textPrimary
            alpha = 255
            textSize = 34f * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.01f
        }

        val paddingX = 40f
        val paddingY = 28f
        val contentWidth = (width - paddingX * 2).toInt()

        val staticLayout = StaticLayout.Builder.obtain(
            question.questionText,
            0,
            question.questionText.length,
            textPaint,
            contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1.15f)
            .setIncludePad(true)
            .setMaxLines(4)
            .build()

        canvas.save()
        canvas.translate(left + paddingX, top + paddingY)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawOptionItem(
        canvas: Canvas,
        letter: String,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        isRevealed: Boolean,
        isCorrect: Boolean,
        cardBg: Int,
        accentColor: Int,
        textPrimary: Int,
        textSecondary: Int,
        fontScale: Float
    ) {
        tempRect.set(left, top, left + width, top + height)

        val fillAlpha: Int
        val borderAlpha: Int
        val currentFillColor: Int
        val currentBorderColor: Int
        val textAlpha: Int

        if (isRevealed) {
            if (isCorrect) {
                currentFillColor = accentColor
                fillAlpha = 55 // Highlighted green fill
                currentBorderColor = accentColor
                borderAlpha = 255
                textAlpha = 255
            } else {
                currentFillColor = cardBg
                fillAlpha = 120 // Dimmed
                currentBorderColor = 0x22475569
                borderAlpha = 70
                textAlpha = 120
            }
        } else {
            currentFillColor = cardBg
            fillAlpha = 210
            currentBorderColor = 0x44475569
            borderAlpha = 140
            textAlpha = 240
        }

        cardPaint.color = currentFillColor
        cardPaint.alpha = fillAlpha
        canvas.drawRoundRect(tempRect, 16f, 16f, cardPaint)

        borderPaint.color = currentBorderColor
        borderPaint.alpha = borderAlpha
        borderPaint.strokeWidth = if (isRevealed && isCorrect) 3.5f else 1.8f
        canvas.drawRoundRect(tempRect, 16f, 16f, borderPaint)

        // Option Letter Circle / Badge
        val badgeRadius = 24f
        val badgeCenterX = left + 45f
        val badgeCenterY = top + (height / 2f)

        if (isRevealed && isCorrect) {
            accentPaint.color = accentColor
            accentPaint.alpha = 255
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, accentPaint)

            // Checkmark in badge
            borderPaint.color = Color.WHITE
            borderPaint.strokeWidth = 3.5f
            path.reset()
            path.moveTo(badgeCenterX - 8f, badgeCenterY)
            path.lineTo(badgeCenterX - 2f, badgeCenterY + 7f)
            path.lineTo(badgeCenterX + 9f, badgeCenterY - 6f)
            canvas.drawPath(path, borderPaint)
        } else {
            cardPaint.color = 0xFF1E293B.toInt()
            cardPaint.alpha = textAlpha
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, cardPaint)

            labelPaint.apply {
                color = if (isRevealed && !isCorrect) textSecondary else textPrimary
                alpha = textAlpha
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(letter, badgeCenterX, badgeCenterY + 8f, labelPaint)
        }

        // Option Text
        textPaint.apply {
            color = if (isRevealed && isCorrect) Color.WHITE else textPrimary
            alpha = textAlpha
            textSize = 28f * fontScale
            typeface = if (isRevealed && isCorrect) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            textAlign = Paint.Align.LEFT
        }

        val textStartX = left + 90f
        val maxTextWidth = (width - 110f).toInt()

        val optLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            maxTextWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .build()

        val textOffsetY = top + ((height - optLayout.height) / 2f)
        canvas.save()
        canvas.translate(textStartX, textOffsetY)
        optLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawCountdownCircle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        timeInQuestion: Float,
        countdownDuration: Float,
        isRevealed: Boolean,
        accentColor: Int,
        textPrimary: Int
    ) {
        // Background track circle
        timerTrackPaint.color = 0x22FFFFFF
        timerTrackPaint.strokeWidth = 7f
        canvas.drawCircle(centerX, centerY, radius, timerTrackPaint)

        if (!isRevealed) {
            val remainingSec = (countdownDuration - timeInQuestion).coerceAtLeast(0f)
            val sweepRatio = (remainingSec / countdownDuration).coerceIn(0f, 1f)

            timerProgressPaint.color = accentColor
            timerProgressPaint.strokeWidth = 7f
            tempRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            // Arc sweeps counter-clockwise or clockwise from top (-90 degrees)
            canvas.drawArc(tempRect, -90f, sweepRatio * 360f, false, timerProgressPaint)

            // Number text
            labelPaint.apply {
                color = textPrimary
                alpha = 255
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val displaySec = (remainingSec + 0.99f).toInt()
            canvas.drawText("$displaySec", centerX, centerY + 12f, labelPaint)
        } else {
            // Answer Revealed Badge
            accentPaint.color = accentColor
            accentPaint.alpha = 240
            canvas.drawCircle(centerX, centerY, radius, accentPaint)

            // White Checkmark
            borderPaint.color = Color.WHITE
            borderPaint.strokeWidth = 5f
            path.reset()
            path.moveTo(centerX - 14f, centerY)
            path.lineTo(centerX - 4f, centerY + 12f)
            path.lineTo(centerX + 16f, centerY - 10f)
            canvas.drawPath(path, borderPaint)
        }
    }

    private fun drawExplanationBanner(
        canvas: Canvas,
        explanation: String,
        left: Float,
        bottom: Float,
        width: Float,
        alpha: Float,
        cardBg: Int,
        accentColor: Int,
        textPrimary: Int
    ) {
        val bannerHeight = 85f
        val top = bottom - bannerHeight
        tempRect.set(left, top, left + width, bottom)

        // Semi-transparent card
        cardPaint.color = 0xFF064E3B.toInt()
        cardPaint.alpha = (alpha * 230).toInt()
        canvas.drawRoundRect(tempRect, 14f, 14f, cardPaint)

        borderPaint.color = accentColor
        borderPaint.alpha = (alpha * 255).toInt()
        borderPaint.strokeWidth = 2f
        canvas.drawRoundRect(tempRect, 14f, 14f, borderPaint)

        // Badge: "KEY CLINICAL TAKEAWAY"
        labelPaint.apply {
            color = accentColor
            this.alpha = (alpha * 255).toInt()
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }
        canvas.drawText("EXPLANATION & CLINICAL RATIONALE:", left + 22f, top + 26f, labelPaint)

        // Rationale text
        textPaint.apply {
            color = Color.WHITE
            this.alpha = (alpha * 255).toInt()
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.01f
        }
        val layout = StaticLayout.Builder.obtain(
            explanation,
            0,
            explanation.length,
            textPaint,
            (width - 44f).toInt()
        ).setMaxLines(2)
            .build()

        canvas.save()
        canvas.translate(left + 22f, top + 34f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawSideMediaBox(
        canvas: Canvas,
        bitmap: Bitmap?,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        cardBg: Int,
        accentColor: Int,
        textSecondary: Int,
        placeholderText: String
    ) {
        tempRect.set(left, top, left + width, top + height)
        cardPaint.color = cardBg
        cardPaint.alpha = 240
        canvas.drawRoundRect(tempRect, 20f, 20f, cardPaint)

        borderPaint.color = 0x44475569
        borderPaint.strokeWidth = 2f
        canvas.drawRoundRect(tempRect, 20f, 20f, borderPaint)

        if (bitmap != null && !bitmap.isRecycled) {
            // Draw scaled bitmap preserving aspect ratio
            val bmpW = bitmap.width.toFloat()
            val bmpH = bitmap.height.toFloat()
            val scale = minOf(width / bmpW, height / bmpH)
            val scaledW = bmpW * scale
            val scaledH = bmpH * scale
            val destLeft = left + (width - scaledW) / 2f
            val destTop = top + (height - scaledH) / 2f

            tempRect.set(destLeft, destTop, destLeft + scaledW, destTop + scaledH)
            canvas.drawBitmap(bitmap, null, tempRect, null)
        } else {
            // Placeholder frame with medical cross and label
            accentPaint.color = accentColor
            accentPaint.alpha = 40
            canvas.drawCircle(left + (width / 2f), top + (height / 2f) - 30f, 50f, accentPaint)

            accentPaint.color = accentColor
            accentPaint.alpha = 180
            val cx = left + (width / 2f)
            val cy = top + (height / 2f) - 30f
            canvas.drawRect(cx - 6f, cy - 25f, cx + 6f, cy + 25f, accentPaint)
            canvas.drawRect(cx - 25f, cy - 6f, cx + 25f, cy + 6f, accentPaint)

            labelPaint.apply {
                color = textSecondary
                alpha = 200
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(placeholderText, left + (width / 2f), top + (height / 2f) + 50f, labelPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas, targetWidth: Int, targetHeight: Int) {
        bgPaint.color = 0xFF0B111E.toInt()
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

        labelPaint.apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No Questions Loaded", targetWidth / 2f, targetHeight / 2f, labelPaint)
    }

    private fun parseColorSafe(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }
}
