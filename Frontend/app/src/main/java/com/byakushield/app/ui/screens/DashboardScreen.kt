package com.byakushield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onStartScan: () -> Unit
) {
    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val greenDark = Color(0xFF173525)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)

    var totalScans by remember {
        mutableStateOf(0L)
    }

    var threatsFound by remember {
        mutableStateOf(0L)
    }

    var statsLoading by remember {
        mutableStateOf(true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    fun refreshStats() {
        scope.launch {
            statsLoading = true

            try {
                val api = RetrofitClient.getClient(tokenManager)

                val firstResponse = api.getAuditHistory(
                    page = 0,
                    size = 100
                )

                if (firstResponse.isSuccessful) {

                    val firstPage = firstResponse.body()

                    if (firstPage != null) {

                        totalScans = firstPage.totalElements

                        var threats = firstPage.content.count {
                            val level = it.threatLevel.uppercase()

                            level == "HIGH" || level == "MEDIUM"
                        }.toLong()

                        if (firstPage.totalPages > 1) {

                            for (page in 1 until firstPage.totalPages) {

                                val response = api.getAuditHistory(
                                    page = page,
                                    size = 100
                                )

                                if (response.isSuccessful) {

                                    val body = response.body()

                                    if (body != null) {

                                        threats += body.content.count {
                                            val level =
                                                it.threatLevel.uppercase()

                                            level == "HIGH" ||
                                                    level == "MEDIUM"
                                        }.toLong()
                                    }
                                }
                            }
                        }

                        threatsFound = threats
                    }
                }

            } catch (_: Exception) {
                // Keep the dashboard usable if history cannot be loaded.
            } finally {
                statsLoading = false
            }
        }
    }

    DisposableEffect(
        lifecycleOwner
    ) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStats()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "ByakuShield",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Your Digital Shield Against Modern Threats.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = onLogout
            ) {

                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = card,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security",
                    tint = green,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = greenDark,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(29.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column {

                    Text(
                        text = "Protection Active",
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Your security modules are ready",
                        color = green,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(26.dp)
        )

        Text(
            text = "Security Overview",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Total Scans",
                value = if (statsLoading) {
                    "..."
                } else {
                    totalScans.toString()
                },
                icon = Icons.Default.Security,
                green = green,
                card = card,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            OverviewCard(
                modifier = Modifier.weight(1f),
                title = "Threats Found",
                value = if (statsLoading) {
                    "..."
                } else {
                    threatsFound.toString()
                },
                icon = Icons.Default.Warning,
                green = green,
                card = card,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        Spacer(
            modifier = Modifier.height(26.dp)
        )

        Text(
            text = "Protection Modules",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                ModuleRow(
                    icon = Icons.Default.Security,
                    title = "TextArmor",
                    description = "Detect suspicious messages and URLs",
                    green = green,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ModuleRow(
                    icon = Icons.Default.Security,
                    title = "QuishGuard",
                    description = "Scan suspicious QR codes",
                    green = green,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ModuleRow(
                    icon = Icons.Default.AudioFile,
                    title = "VoiceShield",
                    description = "Inspect suspicious audio",
                    green = green,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ModuleRow(
                    icon = Icons.Default.Image,
                    title = "DataScrub",
                    description = "Detect sensitive image metadata",
                    green = green,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }

        Spacer(
            modifier = Modifier.height(26.dp)
        )

        Text(
            text = "Quick Scan",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onStartScan
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = greenDark,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Choose a security module",
                            color = textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Select the type of content you want to analyze",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Analyze suspicious messages, QR codes, audio files or images with the appropriate ByakuShield module.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = green,
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(
                            horizontal = 18.dp,
                            vertical = 15.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Choose a Scan",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .height(18.dp)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                )
        )
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    green: Color,
    card: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = card
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(26.dp)
            )

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            if (value == "...") {

                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = green,
                    strokeWidth = 3.dp
                )

            } else {

                Text(
                    text = value,
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = title,
                color = textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ModuleRow(
    icon: ImageVector,
    title: String,
    description: String,
    green: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = green.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = description,
                color = textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}