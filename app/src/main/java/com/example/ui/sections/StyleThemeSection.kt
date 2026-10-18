package com.example.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorTheme
import com.example.model.StyleConfig
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant
import kotlin.math.roundToInt

@Composable
fun StyleThemeSection(
    style: StyleConfig,
    onUpdateStyle: (StyleConfig) -> Unit
) {
    val accentColors = listOf(
        "#10B981" to "Emerald",
        "#06B6D4" to "Cyan",
        "#34D399" to "Mint",
        "#FBBF24" to "Amber",
        "#F43F5E" to "Ruby"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Video Theme Palette",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Themes Grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorTheme.values().forEach { theme ->
                val isSelected = style.theme == theme
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateStyle(style.copy(theme = theme, customAccentHex = theme.accentHex)) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MedicalSurfaceVariant else MedicalSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EmeraldPrimary else MedicalSurfaceHighlight
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(android.graphics.Color.parseColor(theme.bgPrimaryHex)), CircleShape)
                                    .border(1.dp, Color(android.graphics.Color.parseColor(theme.accentHex)), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = theme.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Accent Color Row
        Text(
            text = "Accent Color (Highlight & Timer)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accentColors.forEach { (hex, name) ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = style.customAccentHex.equals(hex, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onUpdateStyle(style.copy(customAccentHex = hex)) },
                    shape = CircleShape,
                    color = color,
                    border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.Transparent)
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Font Scale Slider
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
                    Text("Typography Scale", color = Color.White, fontSize = 13.sp)
                    Text(
                        "${(style.fontScale * 100).toInt()}%",
                        color = EmeraldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = style.fontScale,
                    onValueChange = { onUpdateStyle(style.copy(fontScale = (it * 100f).roundToInt() / 100f)) },
                    valueRange = 0.85f..1.25f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = MedicalSurfaceHighlight
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Toggles
        StyleToggleRow("Circular Countdown Clock", style.showCountdownCircle) {
            onUpdateStyle(style.copy(showCountdownCircle = it))
        }
        StyleToggleRow("Top Progress Bar", style.showProgressBar) {
            onUpdateStyle(style.copy(showProgressBar = it))
        }
        StyleToggleRow("Category Badge Pill", style.showCategoryBadge) {
            onUpdateStyle(style.copy(showCategoryBadge = it))
        }
    }
}

@Composable
private fun StyleToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
        border = BorderStroke(1.dp, MedicalSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 13.sp)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary,
                    uncheckedTrackColor = MedicalSurfaceHighlight
                )
            )
        }
    }
}
