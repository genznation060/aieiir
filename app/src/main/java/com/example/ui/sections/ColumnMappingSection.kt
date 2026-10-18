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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.model.ColumnMapping
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalSurfaceHighlight
import com.example.ui.theme.MedicalSurfaceVariant

@Composable
fun ColumnMappingSection(
    mapping: ColumnMapping,
    onUpdateMapping: (ColumnMapping) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Map input CSV / TSV column index (0-based) to each field:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(12.dp))

        ColumnPickerRow("Question Text", mapping.questionIndex) {
            onUpdateMapping(mapping.copy(questionIndex = it))
        }
        ColumnPickerRow("Option A", mapping.optionAIndex) {
            onUpdateMapping(mapping.copy(optionAIndex = it))
        }
        ColumnPickerRow("Option B", mapping.optionBIndex) {
            onUpdateMapping(mapping.copy(optionBIndex = it))
        }
        ColumnPickerRow("Option C", mapping.optionCIndex) {
            onUpdateMapping(mapping.copy(optionCIndex = it))
        }
        ColumnPickerRow("Option D", mapping.optionDIndex) {
            onUpdateMapping(mapping.copy(optionDIndex = it))
        }
        ColumnPickerRow("Correct Answer (Key)", mapping.correctAnswerIndex) {
            onUpdateMapping(mapping.copy(correctAnswerIndex = it))
        }
        ColumnPickerRow("Clinical Explanation", mapping.explanationIndex) {
            onUpdateMapping(mapping.copy(explanationIndex = it))
        }
        ColumnPickerRow("Side Image / Diagram", mapping.imageIndex) {
            onUpdateMapping(mapping.copy(imageIndex = it))
        }
    }
}

@Composable
private fun ColumnPickerRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceVariant),
        border = BorderStroke(1.dp, MedicalSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (value > 0) onValueChange(value - 1) },
                    enabled = value > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (value > 0) EmeraldPrimary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MedicalSurfaceHighlight,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Col $value",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = { onValueChange(value + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
