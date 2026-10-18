package com.example.model

enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    FHD_1080P(1920, 1080, "1080p Full HD (1920x1080)"),
    HD_720P(1280, 720, "720p HD (1280x720)")
}

data class TimingConfig(
    val countdownSeconds: Float = 5f,
    val answerHoldSeconds: Float = 3f,
    val transitionSeconds: Float = 0.6f,
    val fps: Int = 30
) {
    val totalQuestionDuration: Float
        get() = countdownSeconds + answerHoldSeconds + transitionSeconds

    val totalFramesPerQuestion: Int
        get() = (totalQuestionDuration * fps).toInt()
}

enum class ColorTheme(
    val title: String,
    val bgPrimaryHex: String,
    val bgSecondaryHex: String,
    val cardBgHex: String,
    val accentHex: String,
    val textPrimaryHex: String,
    val textSecondaryHex: String
) {
    MEDICAL_DARK_SLATE(
        title = "Medical Dark Slate",
        bgPrimaryHex = "#0B111E",
        bgSecondaryHex = "#051A18",
        cardBgHex = "#131E2F",
        accentHex = "#10B981",
        textPrimaryHex = "#F8FAFC",
        textSecondaryHex = "#94A3B8"
    ),
    DEEP_NAVY_HOSPITAL(
        title = "Deep Navy Hospital",
        bgPrimaryHex = "#061024",
        bgSecondaryHex = "#091B33",
        cardBgHex = "#0E2442",
        accentHex = "#06B6D4",
        textPrimaryHex = "#FFFFFF",
        textSecondaryHex = "#A5B4FC"
    ),
    EMERALD_FOREST(
        title = "Clinical Emerald",
        bgPrimaryHex = "#03140E",
        bgSecondaryHex = "#062A1E",
        cardBgHex = "#0B3B2B",
        accentHex = "#34D399",
        textPrimaryHex = "#F0FDF4",
        textSecondaryHex = "#86EFAC"
    ),
    CHARCOAL_MINIMAL(
        title = "Charcoal Minimal",
        bgPrimaryHex = "#121214",
        bgSecondaryHex = "#1A1A1E",
        cardBgHex = "#24242A",
        accentHex = "#10B981",
        textPrimaryHex = "#FAFAFA",
        textSecondaryHex = "#A1A1AA"
    )
}

data class StyleConfig(
    val theme: ColorTheme = ColorTheme.MEDICAL_DARK_SLATE,
    val customAccentHex: String = "#10B981",
    val fontScale: Float = 1.0f,
    val showOptionLetters: Boolean = true,
    val showProgressBar: Boolean = true,
    val showCountdownCircle: Boolean = true,
    val showCategoryBadge: Boolean = true,
    val cardCornerRadius: Float = 16f
)

data class OverlayConfig(
    val enableSideImage: Boolean = false,
    val imagePosition: String = "RIGHT", // RIGHT, TOP
    val splitRatio: Float = 0.35f, // 35% of width for side media
    val imageUri: String? = null,
    val fallbackPlaceholderText: String = "Clinical Case Image"
)

data class WatermarkConfig(
    val enabled: Boolean = true,
    val title: String = "MED QUIZ PRO",
    val subtitle: String = "High-Yield Clinical Review",
    val position: String = "TOP_LEFT", // TOP_LEFT, TOP_RIGHT
    val opacity: Float = 0.9f
)

data class AudioConfig(
    val enableTicks: Boolean = true,
    val enableChime: Boolean = true,
    val enableWhoosh: Boolean = true,
    val tickVolume: Float = 0.8f,
    val chimeVolume: Float = 1.0f,
    val whooshVolume: Float = 0.7f
)

data class ExportConfig(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val bitrateMbps: Int = 8,
    val longVideoSafeMode: Boolean = true,
    val autoAdjustIfLong: Boolean = true
)

data class ProjectConfig(
    val timing: TimingConfig = TimingConfig(),
    val style: StyleConfig = StyleConfig(),
    val overlay: OverlayConfig = OverlayConfig(),
    val watermark: WatermarkConfig = WatermarkConfig(),
    val audio: AudioConfig = AudioConfig(),
    val export: ExportConfig = ExportConfig()
)
