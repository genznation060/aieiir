package com.example.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimingConfig
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun TimingSection(
    timing: TimingConfig,
    questionCount: Int,
    onUpdateTiming: (TimingConfig) -> Unit
) {
    val totalPerQ = timing.totalQuestionDuration
    val totalVideo = totalPerQ * questionCount
    val totalFrames = (totalVideo * timing.fps).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Calculation Breakdown Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Per Question: ${String.format("%.1f", totalPerQ)}s (${timing.countdownSeconds.toInt()}s thinking + ${timing.answerHoldSeconds.toInt()}s reveal + ${timing.transitionSeconds}s transition)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Video: ${String.format("%.1f", totalVideo)}s (${formatTime(totalVideo)}) • $totalFrames frames @ ${timing.fps}fps",
                    color = EmeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Countdown / Thinking Slider
        TimingSliderRow(
            label = "Thinking / Countdown Time",
            valueText = "${timing.countdownSeconds.toInt()} sec",
            value = timing.countdownSeconds,
            range = 3f..15f,
            steps = 11,
            testTagId = "slider_countdown_time"
        ) {
            onUpdateTiming(timing.copy(countdownSeconds = it.roundToInt().toFloat()))
        }

        // Answer Hold Slider
        TimingSliderRow(
            label = "Answer Reveal Hold Time",
            valueText = "${timing.answerHoldSeconds.toInt()} sec",
            value = timing.answerHoldSeconds,
            range = 1f..10f,
            steps = 8,
            testTagId = "slider_answer_hold_time"
        ) {
            onUpdateTiming(timing.copy(answerHoldSeconds = it.roundToInt().toFloat()))
        }

        // Transition Slider
        TimingSliderRow(
            label = "Transition Slide Duration",
            valueText = "${String.format("%.1f", timing.transitionSeconds)} sec",
            value = timing.transitionSeconds,
            range = 0.2f..1.5f,
            steps = 12,
            testTagId = "slider_transition_duration"
        ) {
            onUpdateTiming(timing.copy(transitionSeconds = (it * 10f).roundToInt() / 10f))
        }
    }
}

@Composable
private fun TimingSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    testTagId: String,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
        border = BorderStroke(1.dp, MedicalSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = valueText,
                    color = EmeraldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldPrimary,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = MedicalSurfaceHighlight
                ),
                modifier = Modifier.testTag(testTagId)
            )
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}
