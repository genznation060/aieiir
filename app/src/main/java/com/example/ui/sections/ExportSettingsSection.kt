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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.model.ExportConfig
import com.example.model.VideoResolution
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun ExportSettingsSection(
    exportConfig: ExportConfig,
    isLongVideo: Boolean,
    onUpdateExportConfig: (ExportConfig) -> Unit,
    onStartRender: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // High Performance Notice
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
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Native MediaCodec Offline Encoding",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zero frame drops • Up to 8x faster than real-time • Pure H.264/AAC",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Resolution Selector
        Text("Target Video Resolution", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoResolution.values().forEach { res ->
                val isSelected = exportConfig.resolution == res
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateExportConfig(exportConfig.copy(resolution = res)) },
                    label = {
                        Text(
                            text = if (res == VideoResolution.FHD_1080P) "1080p Full HD" else "720p HD",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MedicalSurfaceVariant,
                        labelColor = Color(0xFFCBD5E1)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) EmeraldPrimary else MedicalSurfaceHighlight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("res_chip_${res.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bitrate Slider
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
                    Text("Target Bitrate", color = Color.White, fontSize = 13.sp)
                    Text(
                        "${exportConfig.bitrateMbps} Mbps",
                        color = EmeraldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = exportConfig.bitrateMbps.toFloat(),
                    onValueChange = { onUpdateExportConfig(exportConfig.copy(bitrateMbps = it.roundToInt())) },
                    valueRange = 6f..12f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = MedicalSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("slider_bitrate")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Long Video Safe Mode Switch
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
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Long Video Safe Mode",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isLongVideo) "Active: Over 10 min, sets 720p/6Mbps" else "Auto-downscales if duration exceeds 10 mins",
                            color = if (isLongVideo) EmeraldPrimary else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = exportConfig.longVideoSafeMode,
                    onCheckedChange = { onUpdateExportConfig(exportConfig.copy(longVideoSafeMode = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedTrackColor = MedicalSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("switch_long_video_safe_mode")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Render Button
        Button(
            onClick = onStartRender,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_video_export_button")
        ) {
            Icon(
                imageVector = Icons.Default.MovieCreation,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "RENDER MCQ VIDEO (FAST EXPORT)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
