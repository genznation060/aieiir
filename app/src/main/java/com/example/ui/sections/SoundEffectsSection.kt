package com.example.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundPlayer
import com.example.model.AudioConfig
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun SoundEffectsSection(
    audio: AudioConfig,
    onUpdateAudio: (AudioConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Banner info
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = EmeraldContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Synthesized High-Fidelity Audio",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sampled at 44.1kHz • Hardware AAC 128kbps export",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Test Buttons Row
        Text(
            text = "Test Audio Effects Live",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { SoundPlayer.playTick(audio.tickVolume) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, MedicalSurfaceHighlight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("test_tick_sfx_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tick (1s)", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { SoundPlayer.playChime(audio.chimeVolume) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, MedicalSurfaceHighlight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("test_chime_sfx_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chime (Key)", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { SoundPlayer.playWhoosh(audio.whooshVolume) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, MedicalSurfaceHighlight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("test_whoosh_sfx_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Whoosh", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound Effect Toggles & Volume Sliders
        SfxControlCard(
            title = "Countdown Ticks",
            subtitle = "Woodblock click on every second of thinking time",
            enabled = audio.enableTicks,
            volume = audio.tickVolume,
            onToggle = { onUpdateAudio(audio.copy(enableTicks = it)) },
            onVolumeChange = { onUpdateAudio(audio.copy(tickVolume = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SfxControlCard(
            title = "Correct Answer Chime",
            subtitle = "Resonant C-Major harmonic chime on answer reveal",
            enabled = audio.enableChime,
            volume = audio.chimeVolume,
            onToggle = { onUpdateAudio(audio.copy(enableChime = it)) },
            onVolumeChange = { onUpdateAudio(audio.copy(chimeVolume = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SfxControlCard(
            title = "Question Transition Whoosh",
            subtitle = "Air sweep sound when sliding to next question",
            enabled = audio.enableWhoosh,
            volume = audio.whooshVolume,
            onToggle = { onUpdateAudio(audio.copy(enableWhoosh = it)) },
            onVolumeChange = { onUpdateAudio(audio.copy(whooshVolume = it)) }
        )
    }
}

@Composable
private fun SfxControlCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    volume: Float,
    onToggle: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
        border = BorderStroke(1.dp, MedicalSurfaceHighlight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedTrackColor = MedicalSurfaceHighlight
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Volume", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text("${(volume * 100).toInt()}%", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = volume,
                    onValueChange = { onVolumeChange((it * 100f).roundToInt() / 100f) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = MedicalSurfaceHighlight
                    )
                )
            }
        }
    }
}
