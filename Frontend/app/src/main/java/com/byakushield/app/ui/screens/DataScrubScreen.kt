package com.byakushield.app.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.model.ThreatResponse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

import java.io.File
import java.util.Locale


@Composable
fun DataScrubScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val innerCard = Color(0xFF17241D)

    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorColor = Color(0xFFFF6B6B)
    val warningColor = Color(0xFFFFB74D)

    var selectedFile by remember {
        mutableStateOf<File?>(null)
    }

    var selectedFileName by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf<ThreatResponse?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * -------------------------------------------------------
     * IMAGE PICKER
     * -------------------------------------------------------
     */

    val filePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            result = null
            errorMessage = null
            selectedFile = null
            selectedFileName = null

            scope.launch {

                try {

                    val file =
                        withContext(Dispatchers.IO) {

                            val inputStream =
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?: throw Exception(
                                        "Unable to open the selected image."
                                    )

                            val originalName =
                                getDisplayName(
                                    context,
                                    uri
                                )

                            val extension =
                                originalName
                                    ?.substringAfterLast(
                                        '.',
                                        "jpg"
                                    )
                                    ?.lowercase()
                                    ?.let {

                                        if (
                                            it.length <= 5 &&
                                            it.matches(
                                                Regex("[a-z0-9]+")
                                            )
                                        ) {
                                            it
                                        } else {
                                            "jpg"
                                        }
                                    }
                                    ?: "jpg"

                            val temporaryFile =
                                File(
                                    context.cacheDir,
                                    "datascrub_${System.currentTimeMillis()}.$extension"
                                )

                            inputStream.use { input ->

                                temporaryFile
                                    .outputStream()
                                    .use { output ->

                                        input.copyTo(output)
                                    }
                            }

                            Pair(
                                temporaryFile,
                                originalName
                                    ?: "Selected image"
                            )
                        }

                    selectedFile = file.first
                    selectedFileName = file.second

                } catch (e: Exception) {

                    errorMessage =
                        e.localizedMessage
                            ?: "Unable to read the selected image."
                }
            }
        }

    /*
     * -------------------------------------------------------
     * SAVE SANITIZED IMAGE
     * -------------------------------------------------------
     */

    fun saveSanitizedImage(
        threat: ThreatResponse
    ) {

        val encodedData =
            threat.sanitizedData

        if (encodedData.isNullOrBlank()) {

            Toast.makeText(
                context,
                "Sanitized image is not available.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (isSaving) {
            return
        }

        isSaving = true

        scope.launch {

            try {

                withContext(Dispatchers.IO) {

                    val imageBytes =
                        Base64.decode(
                            encodedData,
                            Base64.DEFAULT
                        )

                    if (imageBytes.isEmpty()) {
                        throw Exception(
                            "Sanitized image contains no data."
                        )
                    }

                    val filename =
                        threat.sanitizedFilename
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "byakushield_sanitized.jpg"

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                    ) {

                        val values =
                            ContentValues().apply {

                                put(
                                    MediaStore.Images.Media.DISPLAY_NAME,
                                    filename
                                )

                                put(
                                    MediaStore.Images.Media.MIME_TYPE,
                                    "image/jpeg"
                                )

                                put(
                                    MediaStore.Images.Media.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES +
                                            "/ByakuShield"
                                )

                                put(
                                    MediaStore.Images.Media.IS_PENDING,
                                    1
                                )
                            }

                        val resolver =
                            context.contentResolver

                        val uri =
                            resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                            )
                                ?: throw Exception(
                                    "Unable to create the image file."
                                )

                        try {

                            resolver
                                .openOutputStream(uri)
                                ?.use { output ->

                                    output.write(imageBytes)
                                    output.flush()
                                }
                                ?: throw Exception(
                                    "Unable to write the image file."
                                )

                            values.clear()

                            values.put(
                                MediaStore.Images.Media.IS_PENDING,
                                0
                            )

                            resolver.update(
                                uri,
                                values,
                                null,
                                null
                            )

                        } catch (e: Exception) {

                            resolver.delete(
                                uri,
                                null,
                                null
                            )

                            throw e
                        }

                    } else {

                        val picturesDirectory =
                            Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES
                            )

                        val byakuShieldDirectory =
                            File(
                                picturesDirectory,
                                "ByakuShield"
                            )

                        if (
                            !byakuShieldDirectory.exists() &&
                            !byakuShieldDirectory.mkdirs()
                        ) {

                            throw Exception(
                                "Unable to create the ByakuShield folder."
                            )
                        }

                        val outputFile =
                            File(
                                byakuShieldDirectory,
                                filename
                            )

                        outputFile.outputStream().use { output ->

                            output.write(imageBytes)
                            output.flush()
                        }
                    }
                }

                Toast.makeText(
                    context,
                    "Sanitized image saved to Pictures/ByakuShield.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    context,
                    "Unable to save the sanitized image.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                isSaving = false
            }
        }
    }

    /*
     * -------------------------------------------------------
     * RESULT SCREEN
     * -------------------------------------------------------
     */

    result?.let { threat ->

        val severityColor =
            when (
                threat.threatLevel.uppercase(Locale.US)
            ) {

                "HIGH" ->
                    Color(0xFFFF5252)

                "MEDIUM" ->
                    warningColor

                else ->
                    green
            }

        /*
         * Detect the optional Reality Defender analysis
         * added by the updated backend.
         *
         * This is deliberately kept separate from the
         * DataScrub privacy risk score.
         */
        val hasAiAnalysis =
            threat.details.contains(
                "Reality Defender",
                ignoreCase = true
            )

        val aiStatus =
            when {

                threat.details.contains(
                    "Detection status: AUTHENTIC",
                    ignoreCase = true
                ) ->
                    "AUTHENTIC"

                threat.details.contains(
                    "Detection status: MANIPULATED",
                    ignoreCase = true
                ) ->
                    "MANIPULATED"

                threat.details.contains(
                    "Detection status:",
                    ignoreCase = true
                ) ->
                    "INCONCLUSIVE"

                else ->
                    "UNAVAILABLE"
            }

        val aiColor =
            when (aiStatus) {

                "AUTHENTIC" ->
                    green

                "MANIPULATED" ->
                    Color(0xFFFF5252)

                "INCONCLUSIVE" ->
                    warningColor

                else ->
                    textSecondary
            }

        val aiIcon =
            when (aiStatus) {

                "AUTHENTIC" ->
                    Icons.Default.Verified

                "MANIPULATED" ->
                    Icons.Default.Warning

                else ->
                    Icons.Default.Security
            }

        val aiScoreText =
            extractAiScore(
                threat.details
            )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(background)
                    .safeDrawingPadding()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp)
        ) {

            /*
             * Header
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        result = null
                    },
                    enabled = !isSaving
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                Column {

                    Text(
                        text = "Threat Result",
                        color = textPrimary,
                        style =
                            MaterialTheme.typography.headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text = "DataScrub",
                        color = textSecondary,
                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * Threat summary
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = card
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(76.dp)
                                .background(
                                    color =
                                        severityColor.copy(
                                            alpha = 0.12f
                                        ),
                                    shape =
                                        RoundedCornerShape(22.dp)
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                if (
                                    threat.threatLevel
                                        .equals(
                                            "LOW",
                                            ignoreCase = true
                                        )
                                ) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Warning
                                },
                            contentDescription = null,
                            tint = severityColor,
                            modifier =
                                Modifier.size(42.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )

                    Text(
                        text =
                            threat.threatLevel.uppercase(
                                Locale.US
                            ),
                        color = severityColor,
                        style =
                            MaterialTheme.typography.displaySmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text = "Threat Level",
                        color = textSecondary,
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(30.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Privacy Risk Score",
                            color = textSecondary,
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text =
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    threat.riskScore
                                ),
                            color = textPrimary,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(
                                    color =
                                        severityColor.copy(
                                            alpha = 0.12f
                                        ),
                                    shape =
                                        RoundedCornerShape(20.dp)
                                )
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(
                                        threat.riskScore
                                            .coerceIn(
                                                0.0,
                                                1.0
                                            )
                                            .toFloat()
                                    )
                                    .height(16.dp)
                                    .background(
                                        color =
                                            severityColor,
                                        shape =
                                            RoundedCornerShape(
                                                20.dp
                                            )
                                    )
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "${(threat.riskScore * 100).toInt()}% privacy risk",
                        color = textSecondary,
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * Privacy Analysis
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = card
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Security,
                            contentDescription = null,
                            tint = green,
                            modifier =
                                Modifier.size(30.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Text(
                            text = "Privacy Analysis",
                            color = textPrimary,
                            style =
                                MaterialTheme.typography.headlineSmall,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    ResultInfoRow(
                        label = "Module",
                        value = threat.module,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    ResultInfoRow(
                        label = "Threat Level",
                        value = threat.threatLevel.uppercase(
                            Locale.US
                        ),
                        valueColor = severityColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )

                    Text(
                        text = "Metadata Findings",
                        color = textPrimary,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            if (hasAiAnalysis) {
                                removeAiAnalysisFromDetails(
                                    threat.details
                                )
                            } else {
                                threat.details
                            },
                        color = textSecondary,
                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                }
            }

            /*
             * ------------------------------------------------
             * REALITY DEFENDER AI CARD
             * ------------------------------------------------
             *
             * This is intentionally separate from the
             * DataScrub privacy score.
             */
            if (hasAiAnalysis) {

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    shape =
                        RoundedCornerShape(22.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = card
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = aiIcon,
                                contentDescription = null,
                                tint = aiColor,
                                modifier =
                                    Modifier.size(30.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Column {

                                Text(
                                    text =
                                        "AI Deepfake Analysis",
                                    color = textPrimary,
                                    style =
                                        MaterialTheme.typography.headlineSmall,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "Reality Defender",
                                    color = textSecondary,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(22.dp)
                        )

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(15.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        aiColor.copy(
                                            alpha = 0.08f
                                        )
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                            ) {

                                Text(
                                    text = "Detection Status",
                                    color = textSecondary,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text = aiStatus,
                                    color = aiColor,
                                    style =
                                        MaterialTheme.typography.titleLarge,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        if (
                            aiScoreText != null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(14.dp)
                            )

                            ResultInfoRow(
                                label = "AI Risk Score",
                                value = aiScoreText,
                                valueColor = aiColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        Text(
                            text =
                                when (aiStatus) {

                                    "AUTHENTIC" ->
                                        "The external AI detector found the image consistent with authentic media."

                                    "MANIPULATED" ->
                                        "The external AI detector identified characteristics consistent with an AI-generated or manipulated image."

                                    else ->
                                        "The external AI detector returned an inconclusive result."
                                },
                            color = textSecondary,
                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        Text(
                            text =
                                "AI authenticity analysis is separate from the DataScrub privacy score and is provided as an additional detection capability.",
                            color = textSecondary,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * Sanitized image card
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape =
                    RoundedCornerShape(22.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = card
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = green,
                            modifier =
                                Modifier.size(30.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Column {

                            Text(
                                text =
                                    "Sanitized Image Ready",
                                color = textPrimary,
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    if (
                                        threat.sanitizedData
                                            .isNullOrBlank()
                                    ) {
                                        "Sanitized copy unavailable"
                                    } else {
                                        "Metadata-free copy ready to save"
                                    },
                                color =
                                    if (
                                        threat.sanitizedData
                                            .isNullOrBlank()
                                    ) {
                                        warningColor
                                    } else {
                                        green
                                    },
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(14.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = innerCard
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Image,
                                contentDescription = null,
                                tint = green,
                                modifier =
                                    Modifier.size(28.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        threat.sanitizedFilename
                                            ?: "byakushield_sanitized.jpg",
                                    color = textPrimary,
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "JPEG copy re-encoded without the original embedded metadata.",
                                    color = textSecondary,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    /*
                     * SAVE BUTTON
                     */
                    Button(
                        onClick = {
                            saveSanitizedImage(threat)
                        },
                        enabled =
                            !threat.sanitizedData
                                .isNullOrBlank() &&
                                    !isSaving,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(15.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = green,
                                contentColor = Color.Black,
                                disabledContainerColor =
                                    Color(0xFF26352D),
                                disabledContentColor =
                                    Color(0xFF718078)
                            )
                    ) {

                        if (isSaving) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(10.dp)
                            )

                            Text(
                                text = "Saving...",
                                fontWeight =
                                    FontWeight.Bold
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.Download,
                                contentDescription = null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Save Sanitized Image",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    /*
                     * ORIGINAL IMAGE REMINDER
                     */
                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(15.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    warningColor.copy(
                                        alpha = 0.08f
                                    )
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                            verticalAlignment =
                                Alignment.Top
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Delete,
                                contentDescription = null,
                                tint = warningColor,
                                modifier =
                                    Modifier.size(23.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(10.dp)
                            )

                            Column {

                                Text(
                                    text =
                                        "Original image remains unchanged",
                                    color = textPrimary,
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(5.dp)
                                )

                                Text(
                                    text =
                                        threat.privacyReminder
                                            ?: "The original image was not deleted. " +
                                            "Delete the original manually if " +
                                            "you no longer want the unsanitized " +
                                            "copy on your device.",
                                    color = textSecondary,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        return
    }

    /*
     * -------------------------------------------------------
     * INPUT SCREEN
     * -------------------------------------------------------
     */

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background)
                .safeDrawingPadding()
                .verticalScroll(scrollState)
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                enabled = !isLoading
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }

            Spacer(
                modifier =
                    Modifier.width(4.dp)
            )

            Column {

                Text(
                    text = "DataScrub",
                    color = textPrimary,
                    style =
                        MaterialTheme.typography.headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Image Security & Privacy",
                    color = textSecondary,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Text(
            text =
                "Protect images from sensitive metadata and manipulated content.",
            color = textSecondary,
            style =
                MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(
                    horizontal = 24.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            shape =
                RoundedCornerShape(22.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = card
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(76.dp)
                            .background(
                                color =
                                    green.copy(
                                        alpha = 0.10f
                                    ),
                                shape =
                                    RoundedCornerShape(22.dp)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Image,
                        contentDescription = null,
                        tint = green,
                        modifier =
                            Modifier.size(40.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                Text(
                    text =
                        if (selectedFile == null) {
                            "No image selected"
                        } else {
                            "Image ready"
                        },
                    color = textPrimary,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (selectedFile == null) {
                            "Choose an image to inspect its metadata."
                        } else {
                            "The selected image can now be scanned."
                        },
                    color = textSecondary,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                selectedFileName?.let { name ->

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = innerCard
                            )
                    ) {

                        Text(
                            text = name,
                            color = textPrimary,
                            style =
                                MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            modifier =
                                Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 11.dp
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Button(
                    onClick = {

                        if (!isLoading) {
                            filePicker.launch("image/*")
                        }
                    },
                    enabled = !isLoading,
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(15.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = green,
                            contentColor = Color.Black
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Upload,
                        contentDescription = null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            if (selectedFile == null) {
                                "Choose Image"
                            } else {
                                "Choose Different Image"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            shape =
                RoundedCornerShape(18.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = card
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Security,
                        contentDescription = null,
                        tint = green,
                        modifier =
                            Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Text(
                        text = "Metadata & Privacy",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Detect sensitive EXIF/GPS metadata and create a sanitized copy with that metadata removed.",
                    color = textSecondary,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Verified,
                        contentDescription = null,
                        tint = green,
                        modifier =
                            Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Text(
                        text = "AI Deepfake Detection",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Analyze the image for signs of AI-generated or manipulated content using Reality Defender.",
                    color = textSecondary,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "One scan performs both analyses while keeping the privacy and deepfake results separate.",
            color = textSecondary,
            style =
                MaterialTheme.typography.bodySmall,
            modifier =
                Modifier.padding(horizontal = 24.dp)
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Button(
            onClick = {

                val file =
                    selectedFile
                        ?: return@Button

                isLoading = true
                result = null
                errorMessage = null

                scope.launch {

                    try {

                        val response =
                            withContext(Dispatchers.IO) {

                                val api =
                                    RetrofitClient.getClient(
                                        tokenManager
                                    )

                                val requestBody =
                                    file.asRequestBody(
                                        "image/*".toMediaType()
                                    )

                                val multipart =
                                    MultipartBody.Part
                                        .createFormData(
                                            "file",
                                            file.name,
                                            requestBody
                                        )

                                api.scanDataScrub(
                                    multipart
                                )
                            }

                        if (response.isSuccessful) {

                            val body =
                                response.body()

                            if (body != null) {

                                result = body

                            } else {

                                errorMessage =
                                    "The server returned an empty response."
                            }

                        } else {

                            errorMessage =
                                when (response.code()) {

                                    400 ->
                                        "The selected image could not be processed."

                                    401, 403 ->
                                        "Your session has expired. Please log in again."

                                    413 ->
                                        "The selected image is too large."

                                    500 ->
                                        "The DataScrub service encountered an error."

                                    else ->
                                        "DataScrub failed. Server returned ${response.code()}."
                                }
                        }

                    } catch (e: Exception) {

                        errorMessage =
                            if (
                                e.localizedMessage
                                    ?.contains(
                                        "timeout",
                                        ignoreCase = true
                                    ) == true
                            ) {

                                "The server took too long to process the image."

                            } else {

                                "Unable to connect to the ByakuShield server."
                            }

                    } finally {

                        isLoading = false

                        file.delete()
                    }
                }
            },
            enabled =
                selectedFile != null &&
                        !isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp),
            shape =
                RoundedCornerShape(15.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = green,
                    contentColor = Color.Black
                )
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Scanning...",
                    fontWeight =
                        FontWeight.Bold
                )

            } else {

                Text(
                    text = "Scan Image",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        errorMessage?.let { error ->

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            errorColor.copy(
                                alpha = 0.10f
                            )
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Warning,
                        contentDescription = null,
                        tint = errorColor,
                        modifier =
                            Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Text(
                        text = error,
                        color = errorColor,
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )
    }
}


/*
 * -----------------------------------------------------------
 * RESULT HELPERS
 * -----------------------------------------------------------
 */

/**
 * Extracts the AI risk score from the backend's
 * Reality Defender detail string.
 */
private fun extractAiScore(
    details: String
): String? {

    val regex =
        Regex(
            """Normalized AI risk score:\s*([0-9]+(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )

    val match =
        regex.find(details)
            ?: return null

    return match.groupValues[1]
}


/**
 * Removes the Reality Defender section from the normal
 * privacy-analysis details so the AI information isn't
 * duplicated in two cards.
 */
private fun removeAiAnalysisFromDetails(
    details: String
): String {

    val marker =
        "AI media authenticity analysis"

    val index =
        details.indexOf(
            marker,
            ignoreCase = true
        )

    if (index < 0) {
        return details
    }

    return details
        .substring(
            0,
            index
        )
        .trim()
        .ifBlank {
            "Metadata analysis was completed."
        }
}


/**
 * Reusable result row.
 */
@Composable
private fun ResultInfoRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    valueColor: Color = textPrimary
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            color = textSecondary,
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier =
                Modifier.width(16.dp)
        )

        Text(
            text = value,
            color = valueColor,
            style =
                MaterialTheme.typography.bodyMedium,
            fontWeight =
                FontWeight.Bold
        )
    }
}


/*
 * -----------------------------------------------------------
 * FILE HELPER
 * -----------------------------------------------------------
 */

private fun getDisplayName(
    context: android.content.Context,
    uri: Uri
): String? {

    val projection =
        arrayOf(
            android.provider.OpenableColumns.DISPLAY_NAME
        )

    context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->

        val nameIndex =
            cursor.getColumnIndex(
                android.provider.OpenableColumns.DISPLAY_NAME
            )

        if (
            nameIndex >= 0 &&
            cursor.moveToFirst()
        ) {

            return cursor.getString(nameIndex)
        }
    }

    return null
}

