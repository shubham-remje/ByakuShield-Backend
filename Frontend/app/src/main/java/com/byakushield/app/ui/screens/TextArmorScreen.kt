package com.byakushield.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.byakushield.app.model.QuishGuardRequest
import com.byakushield.app.model.TextArmorRequest
import com.byakushield.app.model.ThreatResponse
import kotlinx.coroutines.launch
import java.util.regex.Pattern

@Composable
fun TextArmorScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorColor = Color(0xFFFF6B6B)

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var text by remember {
        mutableStateOf("")
    }

    var result by remember {
        mutableStateOf<ThreatResponse?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    result?.let { threat ->

        ThreatResultScreen(
            result = threat,
            onBack = {
                result = null
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                enabled = !isLoading
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
                    text = "TextArmor",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Text & URL threat analysis",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Analyze a message",
                            color = textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Detect spam, scams, phishing messages, and suspicious URLs contained in the message.",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.length <= 5000) {
                    text = it
                    errorMessage = null
                    result = null
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = {
                Text("Message")
            },
            placeholder = {
                Text("Paste a suspicious message here...")
            },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${text.length}/5000",
                        color = if (text.length >= 5000) {
                            errorColor
                        } else {
                            textSecondary
                        }
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            minLines = 7,
            maxLines = 10
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = {

                when {
                    text.isBlank() -> {
                        errorMessage =
                            "Please enter a message to analyze."
                    }

                    text.length > 5000 -> {
                        errorMessage =
                            "Message must not exceed 5000 characters."
                    }

                    else -> {

                        scope.launch {

                            isLoading = true
                            errorMessage = null
                            result = null

                            try {

                                val api =
                                    RetrofitClient.getClient(
                                        tokenManager
                                    )

                                /*
                                 * First analyze the complete message
                                 * using TextArmor.
                                 */
                                val textResponse =
                                    api.scanText(
                                        TextArmorRequest(
                                            message = text.trim()
                                        )
                                    )

                                if (!textResponse.isSuccessful) {

                                    errorMessage =
                                        when (textResponse.code()) {
                                            400 ->
                                                "Invalid text submitted."

                                            401, 403 ->
                                                "Your session has expired. Please log in again."

                                            500 ->
                                                "The security service encountered an error."

                                            else ->
                                                "Scan failed. Server returned ${textResponse.code()}."
                                        }

                                    return@launch
                                }

                                val textThreat =
                                    textResponse.body()

                                if (textThreat == null) {

                                    errorMessage =
                                        "The server returned an empty response."

                                    return@launch
                                }

                                /*
                                 * Extract HTTP/HTTPS URLs from the
                                 * same message. No separate URL input
                                 * is shown to the user.
                                 */
                                val urls =
                                    extractUrls(text.trim())

                                /*
                                 * Analyze every URL through the existing
                                 * URL-analysis backend functionality.
                                 */
                                val urlResults =
                                    mutableListOf<ThreatResponse>()

                                for (url in urls) {

                                    try {

                                        val urlResponse =
                                            api.scanUrl(
                                                QuishGuardRequest(
                                                    url = url
                                                )
                                            )

                                        if (urlResponse.isSuccessful) {

                                            urlResponse.body()?.let {
                                                urlResults.add(it)
                                            }
                                        }

                                    } catch (_: Exception) {
                                        /*
                                         * TextArmor result should still
                                         * be available if URL analysis
                                         * fails independently.
                                         */
                                    }
                                }

                                /*
                                 * Combine TextArmor + URL analysis.
                                 *
                                 * The highest risk is used so a highly
                                 * suspicious URL cannot be hidden by a
                                 * low-risk text score.
                                 */
                                val highestUrlRisk =
                                    urlResults.maxOfOrNull {
                                        it.riskScore
                                    } ?: 0.0

                                val combinedRisk =
                                    maxOf(
                                        textThreat.riskScore,
                                        highestUrlRisk
                                    )

                                val combinedThreatLevel =
                                    when {
                                        combinedRisk >= 0.50 ->
                                            "HIGH"

                                        combinedRisk >= 0.25 ->
                                            "MEDIUM"

                                        else ->
                                            "LOW"
                                    }

                                val combinedDetails =
                                    buildString {

                                        append(textThreat.details)

                                        if (urlResults.isNotEmpty()) {

                                            append(
                                                "\n\nURL analysis performed by TextArmor:"
                                            )

                                            urlResults.forEachIndexed {
                                                    index,
                                                    urlResult ->

                                                append(
                                                    "\nURL ${index + 1}: "
                                                )

                                                append(
                                                    urlResult.threatLevel
                                                )

                                                append(
                                                    " (risk score: "
                                                )

                                                append(
                                                    String.format(
                                                        "%.2f",
                                                        urlResult.riskScore
                                                    )
                                                )

                                                append(")")

                                                append(
                                                    "\n"
                                                )

                                                append(
                                                    urlResult.details
                                                )
                                            }
                                        }
                                    }

                                /*
                                 * Return one TextArmor result to the UI.
                                 * The user never sees a separate URL module.
                                 */
                                result =
                                    ThreatResponse(
                                        module = "TextArmor",
                                        threatLevel = combinedThreatLevel,
                                        riskScore = combinedRisk,
                                        details = combinedDetails
                                    )

                            } catch (e: Exception) {

                                errorMessage =
                                    if (
                                        e.localizedMessage
                                            ?.contains(
                                                "timeout",
                                                ignoreCase = true
                                            ) == true
                                    ) {
                                        "The server took too long to respond."
                                    } else {
                                        "Unable to connect to the ByakuShield server."
                                    }

                            } finally {

                                isLoading = false
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.Black
            )
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Analyzing...",
                    fontWeight = FontWeight.Bold
                )

            } else {

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Analyze Text",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        errorMessage?.let { error ->

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = errorColor.copy(alpha = 0.10f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = error,
                        color = errorColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

private fun extractUrls(text: String): List<String> {

    val pattern =
        Pattern.compile(
            """https?://[^\s<>"']+""",
            Pattern.CASE_INSENSITIVE
        )

    val matcher =
        pattern.matcher(text)

    val urls =
        mutableListOf<String>()

    while (matcher.find()) {

        var url =
            matcher.group()

        /*
         * Remove punctuation that commonly follows a URL
         * inside normal messages.
         */
        url =
            url.trimEnd(
                '.',
                ',',
                '!',
                '?',
                ':',
                ';',
                ')',
                ']',
                '}'
            )

        if (url.isNotBlank()) {
            urls.add(url)
        }
    }

    return urls.distinct()
}

