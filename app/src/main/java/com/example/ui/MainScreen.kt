package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.ui.components.AccordionSection
import com.example.ui.components.LivePreviewPlayer
import com.example.ui.sections.ColumnMappingSection
import com.example.ui.sections.DataSourceSection
import com.example.ui.sections.ExportSettingsSection
import com.example.ui.sections.OverlaySection
import com.example.ui.sections.SoundEffectsSection
import com.example.ui.sections.StyleThemeSection
import com.example.ui.sections.TimingSection
import com.example.ui.sections.WatermarkSection
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalBackground
import com.example.ui.theme.MedicalSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track accordion expansion states
    var expandedSection by remember { mutableStateOf<Int?>(0) } // Default to Data Source open

    val totalDuration = viewModel.totalDurationSeconds
    val isLongVideo = totalDuration > 600f

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MCQ Video Studio Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "High-Performance Quiz Video Maker",
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MedicalSurface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isExporting) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startExport() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Render Video",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = EmeraldPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("fab_render_video")
                )
            }
        },
        containerColor = MedicalBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // 1. Top Live 16:9 Preview Player
            LivePreviewPlayer(
                questions = uiState.questions,
                config = uiState.config,
                currentTimeSeconds = uiState.currentTimeSeconds,
                totalDurationSeconds = totalDuration,
                isPlaying = uiState.isPlayingPreview,
                sideImageBitmap = uiState.sideImageBitmap,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekToQuestion = { viewModel.seekToQuestion(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Accordion Sections
            // Section 1: Data Source
            AccordionSection(
                title = "1. Data Source (CSV / TSV / Presets)",
                icon = Icons.Default.Dataset,
                badgeText = "${uiState.questions.size} Questions",
                isExpanded = expandedSection == 0,
                onToggle = { expandedSection = if (expandedSection == 0) null else 0 },
                testTagId = "accordion_data_source"
            ) {
                DataSourceSection(
                    questions = uiState.questions,
                    rawCsvText = uiState.csvRawText,
                    onLoadCsvContent = { viewModel.parseAndLoadCsv(it) },
                    onLoadSampleQuestions = { viewModel.loadSampleQuestions() }
                )
            }

            // Section 2: Column Mapping
            AccordionSection(
                title = "2. Column Mapping",
                icon = Icons.Default.Category,
                badgeText = "Question, Options, Answer, Expl.",
                isExpanded = expandedSection == 1,
                onToggle = { expandedSection = if (expandedSection == 1) null else 1 },
                testTagId = "accordion_column_mapping"
            ) {
                ColumnMappingSection(
                    mapping = uiState.columnMapping,
                    onUpdateMapping = { viewModel.updateColumnMapping(it) }
                )
            }

            // Section 3: Timing & Animation
            AccordionSection(
                title = "3. Timing & Animation",
                icon = Icons.Default.Timer,
                badgeText = "${uiState.config.timing.countdownSeconds.toInt()}s Thinking + ${uiState.config.timing.answerHoldSeconds.toInt()}s Reveal",
                isExpanded = expandedSection == 2,
                onToggle = { expandedSection = if (expandedSection == 2) null else 2 },
                testTagId = "accordion_timing"
            ) {
                TimingSection(
                    timing = uiState.config.timing,
                    questionCount = uiState.questions.size,
                    onUpdateTiming = { newTiming -> viewModel.updateConfig { it.copy(timing = newTiming) } }
                )
            }

            // Section 4: Image & Overlay
            AccordionSection(
                title = "4. Image & Media Overlay",
                icon = Icons.Default.Image,
                badgeText = if (uiState.config.overlay.enableSideImage) "Enabled (Split Screen)" else "Disabled",
                isExpanded = expandedSection == 3,
                onToggle = { expandedSection = if (expandedSection == 3) null else 3 },
                testTagId = "accordion_overlay"
            ) {
                OverlaySection(
                    overlay = uiState.config.overlay,
                    imageUri = uiState.sideImageUri,
                    onUpdateOverlay = { newOverlay -> viewModel.updateConfig { it.copy(overlay = newOverlay) } },
                    onSelectImage = { viewModel.setSideImageUri(it) }
                )
            }

            // Section 5: Style & Theme
            AccordionSection(
                title = "5. Style & Theme",
                icon = Icons.Default.Palette,
                badgeText = uiState.config.style.theme.title,
                isExpanded = expandedSection == 4,
                onToggle = { expandedSection = if (expandedSection == 4) null else 4 },
                testTagId = "accordion_style_theme"
            ) {
                StyleThemeSection(
                    style = uiState.config.style,
                    onUpdateStyle = { newStyle -> viewModel.updateConfig { it.copy(style = newStyle) } }
                )
            }

            // Section 6: Watermark
            AccordionSection(
                title = "6. Channel Watermark",
                icon = Icons.Default.BrandingWatermark,
                badgeText = if (uiState.config.watermark.enabled) uiState.config.watermark.title else "Disabled",
                isExpanded = expandedSection == 5,
                onToggle = { expandedSection = if (expandedSection == 5) null else 5 },
                testTagId = "accordion_watermark"
            ) {
                WatermarkSection(
                    watermark = uiState.config.watermark,
                    onUpdateWatermark = { newWm -> viewModel.updateConfig { it.copy(watermark = newWm) } }
                )
            }

            // Section 7: Sound Effects
            AccordionSection(
                title = "7. Sound Effects (SFX)",
                icon = Icons.Default.VolumeUp,
                badgeText = "Ticks, Chimes, Whooshes",
                isExpanded = expandedSection == 6,
                onToggle = { expandedSection = if (expandedSection == 6) null else 6 },
                testTagId = "accordion_sound_effects"
            ) {
                SoundEffectsSection(
                    audio = uiState.config.audio,
                    onUpdateAudio = { newAudio -> viewModel.updateConfig { it.copy(audio = newAudio) } }
                )
            }

            // Section 8: Export Settings + Render
            AccordionSection(
                title = "8. Export Settings & Render",
                icon = Icons.Default.MovieCreation,
                badgeText = "${uiState.config.export.resolution.label.split(" ")[0]} • ${uiState.config.export.bitrateMbps} Mbps",
                isExpanded = expandedSection == 7,
                onToggle = { expandedSection = if (expandedSection == 7) null else 7 },
                testTagId = "accordion_export_settings"
            ) {
                ExportSettingsSection(
                    exportConfig = uiState.config.export,
                    isLongVideo = isLongVideo,
                    onUpdateExportConfig = { newExp -> viewModel.updateConfig { it.copy(export = newExp) } },
                    onStartRender = { viewModel.startExport() }
                )
            }

            Spacer(modifier = Modifier.height(90.dp)) // Extra space for FAB
        }
    }

    // Active Export Progress Dialog
    if (uiState.isExporting) {
        ExportProgressDialog(
            progress = uiState.exportProgress,
            onStopAndSaveEarly = { viewModel.stopAndSaveEarly() },
            onCancel = { viewModel.cancelExport() }
        )
    }

    // Export Success Dialog
    uiState.exportResult?.let { result ->
        ExportSuccessDialog(
            result = result,
            onDismiss = { viewModel.dismissExportResult() }
        )
    }
}
