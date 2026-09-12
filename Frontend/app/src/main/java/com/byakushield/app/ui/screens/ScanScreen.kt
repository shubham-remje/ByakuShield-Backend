package com.byakushield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(
    onTextArmorClick: () -> Unit,
    onQuishGuardClick: () -> Unit,
    onVoiceShieldClick: () -> Unit,
    onDataScrubClick: () -> Unit
) {
    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Scan",
            color = textPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Choose a security module to analyze suspicious content.",
            color = textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Security Modules",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ScanOption(
            title = "TextArmor",
            description = "Analyze suspicious messages and text",
            icon = Icons.Default.TextSnippet,
            cardColor = card,
            iconColor = green,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = onTextArmorClick
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ScanOption(
            title = "QuishGuard",
            description = "Scan and analyze suspicious QR codes",
            icon = Icons.Default.QrCode2,
            cardColor = card,
            iconColor = green,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = onQuishGuardClick
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ScanOption(
            title = "VoiceShield",
            description = "Analyze suspicious audio recordings",
            icon = Icons.Default.Mic,
            cardColor = card,
            iconColor = green,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = onVoiceShieldClick
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ScanOption(
            title = "DataScrub",
            description = "Inspect and sanitize image metadata",
            icon = Icons.Default.Image,
            cardColor = card,
            iconColor = green,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = onDataScrubClick
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "How it works",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Choose a module, provide the suspicious content, and ByakuShield will analyze it and return a threat assessment.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun ScanOption(
    title: String,
    description: String,
    icon: ImageVector,
    cardColor: Color,
    iconColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            ModuleIcon(
                icon = icon,
                green = iconColor
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = description,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = iconColor.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModuleIcon(
    icon: ImageVector,
    green: Color
) {
    Row(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = green.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = green,
            modifier = Modifier.size(26.dp)
        )
    }
}