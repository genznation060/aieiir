package com.example.ui.sections

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.WatermarkConfig
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun WatermarkSection(
    watermark: WatermarkConfig,
    onUpdateWatermark: (WatermarkConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
            border = BorderStroke(1.dp, MedicalSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Studio Watermark",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Displays channel branding in top header",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = watermark.enabled,
                    onCheckedChange = { onUpdateWatermark(watermark.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedTrackColor = MedicalSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("switch_watermark")
                )
            }
        }

        AnimatedVisibility(visible = watermark.enabled) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                OutlinedTextField(
                    value = watermark.title,
                    onValueChange = { onUpdateWatermark(watermark.copy(title = it)) },
                    label = { Text("Channel / Studio Name") },
                    modifier = Modifier.fillMaxWidth().testTag("watermark_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MedicalSurfaceHighlight,
                        focusedContainerColor = MedicalSurfaceVariant,
                        unfocusedContainerColor = MedicalSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = watermark.subtitle,
                    onValueChange = { onUpdateWatermark(watermark.copy(subtitle = it)) },
                    label = { Text("Subtitle / Series Tag") },
                    modifier = Modifier.fillMaxWidth().testTag("watermark_subtitle_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MedicalSurfaceHighlight,
                        focusedContainerColor = MedicalSurfaceVariant,
                        unfocusedContainerColor = MedicalSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opacity Slider
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
                    border = BorderStroke(1.dp, MedicalSurfaceHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Watermark Opacity", color = Color.White, fontSize = 13.sp)
                            Text(
                                "${(watermark.opacity * 100).toInt()}%",
                                color = EmeraldPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = watermark.opacity,
                            onValueChange = { onUpdateWatermark(watermark.copy(opacity = (it * 100f).roundToInt() / 100f)) },
                            valueRange = 0.40f..1.0f,
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
    }
}
