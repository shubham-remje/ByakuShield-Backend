package com.byakushield.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.model.AuditHistoryResponse
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AuditHistoryScreen(
    tokenManager: TokenManager
) {
    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorColor = Color(0xFFFF6B6B)

    val scope = rememberCoroutineScope()

    var records by remember {
        mutableStateOf<List<AuditHistoryResponse>>(emptyList())
    }

    var currentPage by remember {
        mutableStateOf(0)
    }

    var totalPages by remember {
        mutableStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun loadHistory(page: Int) {
        if (page < 0) return

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val api = RetrofitClient.getClient(tokenManager)

                val response = api.getAuditHistory(
                    page = page,
                    size = 10
                )

                if (response.isSuccessful) {

                    val body = response.body()

                    if (body != null) {

                        records = body.content
                        currentPage = body.number
                        totalPages = body.totalPages

                    } else {

                        errorMessage =
                            "The server returned an empty response."
                    }

                } else {

                    errorMessage = when (response.code()) {

                        401 ->
                            "Your session has expired. Please log in again."

                        403 ->
                            "You are not authorized to view audit history."

                        404 ->
                            "Audit history is currently unavailable."

                        500 ->
                            "The server encountered an error."

                        else ->
                            "Unable to load audit history (${response.code()})."
                    }
                }

            } catch (e: Exception) {

                errorMessage =
                    "Unable to connect to the server. Please check your connection."

            } finally {

                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHistory(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = green.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Audit History",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your recent security analysis records",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = {
                    loadHistory(currentPage)
                },
                enabled = !isLoading
            ) {

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh audit history",
                    tint = if (isLoading) {
                        textSecondary
                    } else {
                        textPrimary
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        when {

            isLoading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator(
                            color = green,
                            strokeWidth = 3.dp
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = "Loading audit history...",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            errorMessage != null -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = card
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = errorColor,
                                modifier = Modifier.size(42.dp)
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Unable to load history",
                                color = textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(7.dp)
                            )

                            Text(
                                text = errorMessage!!,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Button(
                                onClick = {
                                    loadHistory(currentPage)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = green,
                                    contentColor = background
                                )
                            ) {

                                Text(
                                    text = "Try Again",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            records.isEmpty() -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
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
                                    .size(70.dp)
                                    .background(
                                        color = green.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(21.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = green,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Text(
                                text = "No audit records yet",
                                color = textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "Your completed security scans will appear here.",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            else -> {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = if (totalPages > 1) {
                                "Page ${currentPage + 1} of $totalPages"
                            } else {
                                "${records.size} records"
                            },
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "Latest first",
                            color = green,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        items(
                            items = records,
                            key = {
                                it.id
                            }
                        ) { record ->

                            AuditRecordCard(
                                record = record,
                                card = card,
                                green = green,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }

                    if (totalPages > 1) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            OutlinedButton(
                                onClick = {
                                    loadHistory(currentPage - 1)
                                },
                                enabled = currentPage > 0 && !isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = green,
                                    disabledContentColor =
                                        textSecondary.copy(alpha = 0.4f)
                                )
                            ) {

                                Text(
                                    text = "Previous"
                                )
                            }

                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                color = textPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = {
                                    loadHistory(currentPage + 1)
                                },
                                enabled =
                                    currentPage < totalPages - 1 &&
                                            !isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = green,
                                    disabledContentColor =
                                        textSecondary.copy(alpha = 0.4f)
                                )
                            ) {

                                Text(
                                    text = "Next"
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditRecordCard(
    record: AuditHistoryResponse,
    card: Color,
    green: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val level =
        record.threatLevel
            .uppercase(Locale.US)
            .ifBlank {
                "UNKNOWN"
            }

    val threatColor =
        when (level) {
            "HIGH" -> Color(0xFFFF5252)
            "MEDIUM" -> Color(0xFFFFB74D)
            "LOW" -> green
            else -> textSecondary
        }

    val riskScore =
        record.riskScore.coerceIn(0.0, 1.0)

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = threatColor.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = threatColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = record.module,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = record.timestamp
                            ?: "Unknown time",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = level,
                    color = threatColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Risk Score",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "%.2f",
                        riskScore
                    ),
                    color = textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = record.details.ifBlank {
                    "No additional analysis details were provided."
                },
                color = textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}