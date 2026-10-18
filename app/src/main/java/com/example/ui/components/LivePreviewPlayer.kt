package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.McqQuestion
import com.example.model.ProjectConfig
import com.example.renderer.FrameRenderer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalCyan
import com.example.ui.theme.MedicalSurface
import com.example.ui.theme.MedicalSurfaceHighlight

@Composable
fun LivePreviewPlayer(
    questions: List<McqQuestion>,
    config: ProjectConfig,
    currentTimeSeconds: Float,
    totalDurationSeconds: Float,
    isPlaying: Boolean,
    sideImageBitmap: Bitmap?,
    onTogglePlay: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekToQuestion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timing = config.timing
    val qDuration = timing.totalQuestionDuration
    val currentQIndex = if (qDuration > 0f) {
        (currentTimeSeconds / qDuration).toInt().coerceIn(0, maxOf(0, questions.lastIndex))
    } else 0
    val timeInQ = currentTimeSeconds - (currentQIndex * qDuration)
    val phaseText = when {
        timeInQ < timing.countdownSeconds -> "Thinking (${(timing.countdownSeconds - timeInQ).toInt() + 1}s)"
        timeInQ < timing.countdownSeconds + timing.answerHoldSeconds -> "Answer Revealed"
        else -> "Transitioning"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MedicalSurface)
            .padding(12.dp)
    ) {
        // 16:9 Canvas Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.5.dp, MedicalSurfaceHighlight), RoundedCornerShape(12.dp))
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .testTag("live_canvas_preview")
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                drawIntoCanvas { composeCanvas ->
                    val w = size.width.toInt()
                    val h = size.height.toInt()
                    FrameRenderer.renderFrame(
                        canvas = composeCanvas.nativeCanvas,
                        targetWidth = w,
                        targetHeight = h,
                        questions = questions,
                        config = config,
                        globalTimeSeconds = currentTimeSeconds,
                        sideImageBitmap = sideImageBitmap
                    )
                }
            }

            // Top-right Preview live pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isPlaying) EmeraldPrimary else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isPlaying) "PLAYING" else "PAUSED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrubber Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatSeconds(currentTimeSeconds),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Slider(
                value = currentTimeSeconds,
                onValueChange = onSeekTo,
                valueRange = 0f..maxOf(1f, totalDurationSeconds),
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldPrimary,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = MedicalSurfaceHighlight
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("preview_scrubber_slider")
            )

            Text(
                text = formatSeconds(totalDurationSeconds),
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jump Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSeekTo(0f) },
                    modifier = Modifier.size(36.dp).testTag("preview_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onSeekTo(maxOf(0f, currentTimeSeconds - 5f)) },
                    modifier = Modifier.size(36.dp).testTag("preview_rewind_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Back 5s",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlay,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.size(44.dp).testTag("preview_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = { onSeekTo(minOf(totalDurationSeconds, currentTimeSeconds + 5f)) },
                    modifier = Modifier.size(36.dp).testTag("preview_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 5s",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Phase and Question Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q${currentQIndex + 1}/${questions.size}: ",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = phaseText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Quick Jump Question Chips
        if (questions.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(questions) { index, _ ->
                    val isSelected = index == currentQIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSeekToQuestion(index) },
                        label = {
                            Text(
                                text = "Q${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MedicalSurfaceHighlight.copy(alpha = 0.4f),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) EmeraldPrimary else Color.Transparent
                        ),
                        modifier = Modifier.height(28.dp).testTag("jump_to_q_${index + 1}")
                    )
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Float): String {
    val totalSec = seconds.toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}
