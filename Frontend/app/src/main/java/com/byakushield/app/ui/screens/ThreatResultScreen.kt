package com.byakushield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byakushield.app.model.ThreatResponse
import java.util.Locale

@Composable
fun ThreatResultScreen(
    result: ThreatResponse,
    onBack: () -> Unit
) {
    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)

    val level = result.threatLevel
        .uppercase(Locale.US)
        .ifBlank { "UNKNOWN" }

    val levelColor =
        when (level) {
            "HIGH" -> Color(0xFFFF5252)
            "MEDIUM" -> Color(0xFFFFB74D)
            "LOW" -> green
            else -> textSecondary
        }

    val levelIcon =
        when (level) {
            "LOW" -> Icons.Default.CheckCircle
            else -> Icons.Default.Warning
        }

    val riskScore =
        result.riskScore.coerceIn(0.0, 1.0)

    val riskPercentage =
        (riskScore * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Column {

                Text(
                    text = "Threat Result",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = result.module,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Verdict card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .background(
                            color = levelColor.copy(alpha = 0.13f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = levelIcon,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = level,
                    color = levelColor,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Threat Level",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Risk Score",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "%.2f",
                            riskScore
                        ),
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(9.dp)
                )

                LinearProgressIndicator(
                    progress = { riskScore.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = levelColor,
                    trackColor = levelColor.copy(alpha = 0.12f)
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "$riskPercentage% risk",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // Analysis details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(23.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )

                    Text(
                        text = "Analysis",
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                ResultRow(
                    label = "Module",
                    value = result.module,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                ResultRow(
                    label = "Threat Level",
                    value = level,
                    valueColor = levelColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Details",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = result.details.ifBlank {
                        "No additional analysis details were provided."
                    },
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}


@Composable
private fun ResultRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    valueColor: Color = textPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            color = textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.width(16.dp)
        )

        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}