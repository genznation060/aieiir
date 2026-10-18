package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.audio.SoundPlayer
import com.example.encoder.ExportProgress
import com.example.encoder.ExportResult
import com.example.encoder.VideoExportManager
import com.example.model.ColumnMapping
import com.example.model.CsvParser
import com.example.model.McqQuestion
import com.example.model.ProjectConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val questions: List<McqQuestion> = McqQuestion.SAMPLE_QUESTIONS,
    val config: ProjectConfig = ProjectConfig(),
    val columnMapping: ColumnMapping = ColumnMapping(),
    val currentTimeSeconds: Float = 0f,
    val isPlayingPreview: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: ExportProgress = ExportProgress(),
    val exportResult: ExportResult? = null,
    val errorMessage: String? = null,
    val sideImageBitmap: Bitmap? = null,
    val sideImageUri: Uri? = null,
    val csvRawText: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val exportManager = VideoExportManager(application)
    private var previewJob: Job? = null
    private var lastTickPlayedTime = -1f
    private var lastChimePlayedTime = -1f

    val totalDurationSeconds: Float
        get() {
            val qCount = _uiState.value.questions.size
            return qCount * _uiState.value.config.timing.totalQuestionDuration
        }

    fun updateConfig(reducer: (ProjectConfig) -> ProjectConfig) {
        _uiState.value = _uiState.value.copy(config = reducer(_uiState.value.config))
    }

    fun updateQuestions(questions: List<McqQuestion>) {
        _uiState.value = _uiState.value.copy(
            questions = questions,
            currentTimeSeconds = 0f
        )
        pausePreview()
    }

    fun loadSampleQuestions() {
        updateQuestions(McqQuestion.SAMPLE_QUESTIONS)
    }

    fun parseAndLoadCsv(csvContent: String) {
        try {
            val parsed = CsvParser.parse(csvContent, _uiState.value.columnMapping)
            if (parsed.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    questions = parsed,
                    csvRawText = csvContent,
                    errorMessage = null,
                    currentTimeSeconds = 0f
                )
                pausePreview()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "No valid questions found in CSV/TSV")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Error parsing CSV: ${e.message}")
        }
    }

    fun updateColumnMapping(mapping: ColumnMapping) {
        _uiState.value = _uiState.value.copy(columnMapping = mapping)
        if (_uiState.value.csvRawText.isNotBlank()) {
            parseAndLoadCsv(_uiState.value.csvRawText)
        }
    }

    fun setSideImageUri(uri: Uri?) {
        if (uri == null) {
            _uiState.value = _uiState.value.copy(sideImageUri = null, sideImageBitmap = null)
            return
        }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                _uiState.value = _uiState.value.copy(
                    sideImageUri = uri,
                    sideImageBitmap = bitmap
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load image: ${e.message}")
            }
        }
    }

    fun seekTo(timeSeconds: Float) {
        val total = totalDurationSeconds
        val clamped = timeSeconds.coerceIn(0f, total)
        _uiState.value = _uiState.value.copy(currentTimeSeconds = clamped)
    }

    fun seekToQuestion(questionIndex: Int) {
        val timing = _uiState.value.config.timing
        val targetTime = (questionIndex * timing.totalQuestionDuration).coerceIn(0f, totalDurationSeconds)
        seekTo(targetTime)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlayingPreview) {
            pausePreview()
        } else {
            startPreview()
        }
    }

    private fun startPreview() {
        previewJob?.cancel()
        _uiState.value = _uiState.value.copy(isPlayingPreview = true)
        val audioConfig = _uiState.value.config.audio
        val timing = _uiState.value.config.timing

        previewJob = viewModelScope.launch {
            val stepMs = 33L // ~30 fps update
            while (isActive) {
                delay(stepMs)
                val current = _uiState.value.currentTimeSeconds
                val next = current + (stepMs / 1000f)
                val total = totalDurationSeconds

                if (next >= total) {
                    _uiState.value = _uiState.value.copy(currentTimeSeconds = total, isPlayingPreview = false)
                    break
                }

                // Check audio trigger during preview
                val qIdx = (next / timing.totalQuestionDuration).toInt()
                val timeInQ = next - (qIdx * timing.totalQuestionDuration)

                // Tick sound every 1s
                if (audioConfig.enableTicks && timeInQ < timing.countdownSeconds) {
                    val sec = timeInQ.toInt()
                    if (sec > 0 && (lastTickPlayedTime.toInt() != sec)) {
                        lastTickPlayedTime = timeInQ
                        SoundPlayer.playTick(audioConfig.tickVolume * 0.7f)
                    }
                }

                // Chime sound at reveal
                if (audioConfig.enableChime && timeInQ >= timing.countdownSeconds) {
                    if (lastChimePlayedTime < timing.countdownSeconds) {
                        lastChimePlayedTime = timeInQ
                        SoundPlayer.playChime(audioConfig.chimeVolume * 0.8f)
                    }
                } else if (timeInQ < timing.countdownSeconds) {
                    lastChimePlayedTime = 0f
                }

                _uiState.value = _uiState.value.copy(currentTimeSeconds = next)
            }
        }
    }

    fun pausePreview() {
        previewJob?.cancel()
        previewJob = null
        _uiState.value = _uiState.value.copy(isPlayingPreview = false)
    }

    fun startExport() {
        pausePreview()
        _uiState.value = _uiState.value.copy(
            isExporting = true,
            errorMessage = null,
            exportResult = null
        )

        viewModelScope.launch {
            // Track export progress
            val progressJob = launch {
                exportManager.progress.collect { prog ->
                    _uiState.value = _uiState.value.copy(exportProgress = prog)
                }
            }

            try {
                val result = exportManager.exportVideo(
                    questions = _uiState.value.questions,
                    config = _uiState.value.config,
                    sideImageBitmap = _uiState.value.sideImageBitmap
                )
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Export failed: ${e.message}"
                )
            } finally {
                progressJob.cancel()
            }
        }
    }

    fun stopAndSaveEarly() {
        exportManager.stopAndSaveEarly()
    }

    fun cancelExport() {
        exportManager.cancel()
        _uiState.value = _uiState.value.copy(isExporting = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }
}
