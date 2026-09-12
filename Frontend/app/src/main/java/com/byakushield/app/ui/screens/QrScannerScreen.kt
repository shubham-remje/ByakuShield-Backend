package com.byakushield.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.model.QuishGuardRequest
import com.byakushield.app.model.ThreatResponse

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

import kotlinx.coroutines.launch

import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


@Composable
fun QrScannerScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorColor = Color(0xFFFF6B6B)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isScanning by remember {
        mutableStateOf(true)
    }

    var isAnalyzing by remember {
        mutableStateOf(false)
    }

    var decodedText by remember {
        mutableStateOf<String?>(null)
    }

    var result by remember {
        mutableStateOf<ThreatResponse?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCameraPermission = granted

            if (granted) {
                errorMessage = null
                isScanning = true
            } else {
                errorMessage =
                    "Camera permission is required to scan QR codes."
            }
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    result?.let { threat ->

        ThreatResultScreen(
            result = threat,
            onBack = {
                result = null
                decodedText = null
                errorMessage = null
                isScanning = true
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                enabled = !isAnalyzing
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
                    text = "QR Scanner",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Scan a QR code for threat analysis",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (!hasCameraPermission) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
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

                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = "Camera Access Required",
                            color = textPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "ByakuShield needs camera access to detect and analyze QR codes. Please enable camera permission in app settings.",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Button(
                            onClick = {

                                val intent =
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data =
                                            Uri.parse(
                                                "package:${context.packageName}"
                                            )
                                    }

                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = green,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(15.dp)
                        ) {

                            Text(
                                text = "Open App Settings",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {

            QrCameraPreview(
                enabled = isScanning && !isAnalyzing,
                onQrDetected = { qrText ->

                    if (!isScanning || isAnalyzing) {
                        return@QrCameraPreview
                    }

                    isScanning = false
                    isAnalyzing = true
                    decodedText = qrText
                    errorMessage = null

                    scope.launch {

                        try {

                            val api =
                                RetrofitClient.getClient(
                                    tokenManager
                                )

                            /*
                             * ML Kit has already decoded the QR payload.
                             *
                             * Instead of taking another image and asking
                             * ZXing to decode it again, send the decoded
                             * payload directly to QuishGuard's existing
                             * URL analysis endpoint.
                             */
                            val response =
                                api.scanUrl(
                                    QuishGuardRequest(
                                        url = qrText
                                    )
                                )

                            if (response.isSuccessful) {

                                val body =
                                    response.body()

                                if (body != null) {

                                    result = body

                                } else {

                                    errorMessage =
                                        "The server returned an empty response."

                                    isScanning = true
                                }

                            } else {

                                errorMessage =
                                    when (response.code()) {

                                        401, 403 ->
                                            "Your session has expired. Please log in again."

                                        413 ->
                                            "The QR payload is too large."

                                        500 ->
                                            "The QR security service encountered an error."

                                        else ->
                                            "QR analysis failed. Server returned ${response.code()}."
                                    }

                                isScanning = true
                            }

                        } catch (e: Exception) {

                            e.printStackTrace()

                            errorMessage =
                                when {

                                    e.localizedMessage
                                        ?.contains(
                                            "timeout",
                                            ignoreCase = true
                                        ) == true ->

                                        "The server took too long to analyze the QR code."

                                    e is ConnectException ||
                                            e is UnknownHostException ->

                                        "Failed to connect to the ByakuShield server."

                                    else ->
                                        "QR analysis failed. Please try again."
                                }

                            isScanning = true

                        } finally {

                            isAnalyzing = false
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(35.dp)
                    .border(
                        width = 3.dp,
                        color = green,
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            if (isScanning && !isAnalyzing) {

                Text(
                    text = "Align the QR code inside the frame",
                    color = textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                )
            }

            if (isAnalyzing) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.60f),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = card
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                horizontal = 28.dp,
                                vertical = 24.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            CircularProgressIndicator(
                                color = green
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            Text(
                                text = "Analyzing QR code...",
                                color = textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Checking the QR payload for suspicious indicators",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        decodedText?.let { text ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = card
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "QR Code Detected",
                            color = textPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = text,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        errorMessage?.let { error ->

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = errorColor.copy(alpha = 0.10f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
            modifier = Modifier.height(12.dp)
        )
    }
}


@Composable
private fun QrCameraPreview(
    enabled: Boolean,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {

        PreviewView(context).apply {
            scaleType =
                PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val scannerOptions = remember {

        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                256
            )
            .build()
    }

    val scanner = remember {

        BarcodeScanning.getClient(
            scannerOptions
        )
    }

    val detectionInProgress = remember {
        AtomicBoolean(false)
    }

    DisposableEffect(Unit) {

        onDispose {

            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = {
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(enabled) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider =
                                previewView.surfaceProvider
                        }

                val imageAnalysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                imageAnalysis.setAnalyzer(
                    cameraExecutor
                ) { imageProxy: ImageProxy ->

                    if (!enabled) {

                        imageProxy.close()
                        return@setAnalyzer
                    }

                    if (detectionInProgress.get()) {

                        imageProxy.close()
                        return@setAnalyzer
                    }

                    detectionInProgress.set(true)

                    val mediaImage =
                        imageProxy.image

                    if (mediaImage == null) {

                        detectionInProgress.set(false)
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val inputImage =
                        InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                    scanner.process(inputImage)

                        .addOnSuccessListener { barcodes ->

                            val barcode =
                                barcodes.firstOrNull {
                                    !it.rawValue
                                        .isNullOrBlank()
                                }

                            val qrText =
                                barcode?.rawValue

                            if (!qrText.isNullOrBlank()) {

                                onQrDetected(qrText)
                            }
                        }

                        .addOnFailureListener {
                            // Ignore individual frame failures.
                        }

                        .addOnCompleteListener {

                            imageProxy.close()
                            detectionInProgress.set(false)
                        }
                }

                try {

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}