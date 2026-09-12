package com.byakushield.app.ui.screens

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AudioFile
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

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


@Composable
fun VoiceShieldScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorColor = Color(0xFFFF6B6B)

    var isUploading by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf<ThreatResponse?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var selectedFileName by remember {
        mutableStateOf<String?>(null)
    }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            errorMessage = null
            result = null
            selectedFileName = null

            scope.launch {

                isUploading = true

                var wavFile: File? = null

                try {

                    val originalName =
                        withContext(Dispatchers.IO) {
                            getDisplayName(
                                context,
                                uri
                            )
                        }

                    selectedFileName =
                        originalName ?: "Selected audio"

                    wavFile =
                        withContext(Dispatchers.IO) {
                            convertAudioToWav(
                                context = context,
                                uri = uri
                            )
                        }

                    val response =
                        withContext(Dispatchers.IO) {

                            val requestBody =
                                wavFile.asRequestBody(
                                    "audio/wav".toMediaType()
                                )

                            val multipart =
                                MultipartBody.Part.createFormData(
                                    "file",
                                    wavFile.name,
                                    requestBody
                                )

                            val api =
                                RetrofitClient.getClient(
                                    tokenManager
                                )

                            api.scanAudio(multipart)
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
                                    "The selected audio file could not be processed."

                                401, 403 ->
                                    "Your session has expired. Please log in again."

                                413 ->
                                    "The audio file is too large."

                                500 ->
                                    "The VoiceShield service encountered an error."

                                else ->
                                    "Audio analysis failed. Server returned ${response.code()}."
                            }
                    }

                } catch (e: UnsupportedAudioFormatException) {

                    errorMessage =
                        e.message
                            ?: "This audio format is not supported."

                } catch (e: IllegalArgumentException) {

                    errorMessage =
                        e.message
                            ?: "The selected audio file could not be processed."

                } catch (e: Exception) {

                    errorMessage =
                        if (
                            e.localizedMessage
                                ?.contains(
                                    "timeout",
                                    ignoreCase = true
                                ) == true
                        ) {
                            "The AI audio analysis took too long to complete."
                        } else {
                            "Failed to connect to the ByakuShield server."
                        }

                } finally {

                    wavFile?.delete()
                    isUploading = false
                }
            }
        }

    result?.let { threat ->

        ThreatResultScreen(
            result = threat,
            onBack = {
                result = null
                errorMessage = null
                selectedFileName = null
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .verticalScroll(scrollState)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                enabled = !isUploading
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
                    text = "VoiceShield",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "AI Audio Threat Analysis",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = card
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = green.copy(alpha = 0.10f)
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(42.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "Analyze Suspicious Audio",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Upload an audio file you received for VoiceShield AI analysis.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Reality Defender analyzes the audio for characteristics associated with AI-generated, cloned or manipulated speech.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "MP3, WAV, M4A, AAC, OGG and other Android-supported audio formats are converted to WAV before analysis.",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                selectedFileName?.let { fileName ->

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Selected: $fileName",
                        color = green,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                if (isUploading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = green,
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "Analyzing audio with AI...",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "VoiceShield is checking the audio for characteristics associated with AI-generated or manipulated speech.",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                } else {

                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = green,
                            contentColor = Color.Black
                        )
                    ) {

                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "Select Audio File",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        errorMessage?.let { error ->

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = errorColor.copy(
                        alpha = 0.10f
                    )
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
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


private fun getDisplayName(
    context: Context,
    uri: Uri
): String? {

    return context.contentResolver
        .query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        ?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {
                    cursor.getString(index)
                } else {
                    null
                }

            } else {
                null
            }
        }
}


private class UnsupportedAudioFormatException(
    message: String
) : Exception(message)


private fun convertAudioToWav(
    context: Context,
    uri: Uri
): File {

    val directory =
        context.getExternalFilesDir(
            Environment.DIRECTORY_MUSIC
        )
            ?: throw IllegalStateException(
                "Unable to access temporary audio storage."
            )

    if (!directory.exists()) {
        directory.mkdirs()
    }

    val rawPcmFile =
        File(
            directory,
            "decoded_${System.currentTimeMillis()}.pcm"
        )

    val wavFile =
        File(
            directory,
            "converted_${System.currentTimeMillis()}.wav"
        )

    var extractor: MediaExtractor? = null
    var decoder: MediaCodec? = null

    try {

        extractor = MediaExtractor()

        val fileDescriptor =
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException(
                    "Unable to open the selected audio file."
                )

        fileDescriptor.use { descriptor ->

            extractor.setDataSource(
                descriptor.fileDescriptor
            )
        }

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {

            val format =
                extractor.getTrackFormat(i)

            val mime =
                format.getString(
                    MediaFormat.KEY_MIME
                )

            if (
                mime != null &&
                mime.startsWith("audio/")
            ) {

                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (
            audioTrackIndex < 0 ||
            audioFormat == null
        ) {

            throw UnsupportedAudioFormatException(
                "No supported audio track was found in the selected file."
            )
        }

        val mime =
            audioFormat.getString(
                MediaFormat.KEY_MIME
            )
                ?: throw UnsupportedAudioFormatException(
                    "Unable to determine the audio format."
                )

        val sampleRate =
            if (
                audioFormat.containsKey(
                    MediaFormat.KEY_SAMPLE_RATE
                )
            ) {
                audioFormat.getInteger(
                    MediaFormat.KEY_SAMPLE_RATE
                )
            } else {
                44100
            }

        val channelCount =
            if (
                audioFormat.containsKey(
                    MediaFormat.KEY_CHANNEL_COUNT
                )
            ) {
                audioFormat.getInteger(
                    MediaFormat.KEY_CHANNEL_COUNT
                )
            } else {
                1
            }

        extractor.selectTrack(audioTrackIndex)

        decoder =
            try {

                MediaCodec.createDecoderByType(mime)

            } catch (_: Exception) {

                throw UnsupportedAudioFormatException(
                    "This audio format is not supported by the device."
                )
            }

        decoder.configure(
            audioFormat,
            null,
            null,
            0
        )

        decoder.start()

        FileOutputStream(rawPcmFile).use { rawOutput ->

            val bufferInfo =
                MediaCodec.BufferInfo()

            var inputFinished = false
            var outputFinished = false

            while (!outputFinished) {

                if (!inputFinished) {

                    val inputIndex =
                        decoder.dequeueInputBuffer(
                            10_000
                        )

                    if (inputIndex >= 0) {

                        val inputBuffer =
                            decoder.getInputBuffer(
                                inputIndex
                            )

                        if (inputBuffer != null) {

                            inputBuffer.clear()

                            val sampleSize =
                                extractor.readSampleData(
                                    inputBuffer,
                                    0
                                )

                            if (sampleSize < 0) {

                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )

                                inputFinished = true

                            } else {

                                val presentationTime =
                                    extractor.sampleTime

                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    presentationTime,
                                    0
                                )

                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex =
                    decoder.dequeueOutputBuffer(
                        bufferInfo,
                        10_000
                    )

                when {

                    outputIndex >= 0 -> {

                        val outputBuffer =
                            decoder.getOutputBuffer(
                                outputIndex
                            )

                        if (
                            outputBuffer != null &&
                            bufferInfo.size > 0
                        ) {

                            outputBuffer.position(
                                bufferInfo.offset
                            )

                            outputBuffer.limit(
                                bufferInfo.offset +
                                        bufferInfo.size
                            )

                            val pcmBytes =
                                ByteArray(
                                    bufferInfo.size
                                )

                            outputBuffer.get(
                                pcmBytes
                            )

                            rawOutput.write(
                                pcmBytes
                            )
                        }

                        decoder.releaseOutputBuffer(
                            outputIndex,
                            false
                        )

                        if (
                            bufferInfo.flags and
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                            0
                        ) {

                            outputFinished = true
                        }
                    }

                    outputIndex ==
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                        val outputFormat =
                            decoder.outputFormat

                        val actualSampleRate =
                            if (
                                outputFormat.containsKey(
                                    MediaFormat.KEY_SAMPLE_RATE
                                )
                            ) {
                                outputFormat.getInteger(
                                    MediaFormat.KEY_SAMPLE_RATE
                                )
                            } else {
                                sampleRate
                            }

                        val actualChannels =
                            if (
                                outputFormat.containsKey(
                                    MediaFormat.KEY_CHANNEL_COUNT
                                )
                            ) {
                                outputFormat.getInteger(
                                    MediaFormat.KEY_CHANNEL_COUNT
                                )
                            } else {
                                channelCount
                            }

                        writeWavFile(
                            rawPcmFile = rawPcmFile,
                            wavFile = wavFile,
                            sampleRate = actualSampleRate,
                            channels = actualChannels,
                            bitsPerSample = 16
                        )
                    }
                }
            }
        }

        if (!wavFile.exists()) {

            writeWavFile(
                rawPcmFile = rawPcmFile,
                wavFile = wavFile,
                sampleRate = sampleRate,
                channels = channelCount,
                bitsPerSample = 16
            )
        }

        if (
            !wavFile.exists() ||
            wavFile.length() <= 44
        ) {

            throw IllegalArgumentException(
                "The selected audio file could not be converted."
            )
        }

        return wavFile

    } finally {

        try {
            decoder?.stop()
        } catch (_: Exception) {
        }

        try {
            decoder?.release()
        } catch (_: Exception) {
        }

        try {
            extractor?.release()
        } catch (_: Exception) {
        }

        rawPcmFile.delete()
    }
}


private fun writeWavFile(
    rawPcmFile: File,
    wavFile: File,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int
) {

    val dataLength =
        rawPcmFile.length()

    if (dataLength <= 0) {
        return
    }

    FileOutputStream(wavFile).use { fileOutput ->

        val output =
            BufferedOutputStream(fileOutput)

        writeWavHeader(
            output = output,
            sampleRate = sampleRate,
            channels = channels,
            bitsPerSample = bitsPerSample,
            dataLength = dataLength
        )

        FileInputStream(
            rawPcmFile
        ).use { input ->
            input.copyTo(output)
        }

        output.flush()
    }
}


private fun writeWavHeader(
    output: BufferedOutputStream,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int,
    dataLength: Long
) {

    output.write("RIFF".toByteArray())

    writeIntLE(
        output,
        (36 + dataLength).toInt()
    )

    output.write("WAVE".toByteArray())
    output.write("fmt ".toByteArray())

    writeIntLE(
        output,
        16
    )

    writeShortLE(
        output,
        1
    )

    writeShortLE(
        output,
        channels
    )

    writeIntLE(
        output,
        sampleRate
    )

    val byteRate =
        sampleRate *
                channels *
                bitsPerSample /
                8

    writeIntLE(
        output,
        byteRate
    )

    val blockAlign =
        channels *
                bitsPerSample /
                8

    writeShortLE(
        output,
        blockAlign
    )

    writeShortLE(
        output,
        bitsPerSample
    )

    output.write("data".toByteArray())

    writeIntLE(
        output,
        dataLength.toInt()
    )
}


private fun writeIntLE(
    output: BufferedOutputStream,
    value: Int
) {

    output.write(
        value and 0xFF
    )

    output.write(
        value shr 8 and 0xFF
    )

    output.write(
        value shr 16 and 0xFF
    )

    output.write(
        value shr 24 and 0xFF
    )
}


private fun writeShortLE(
    output: BufferedOutputStream,
    value: Int
) {

    output.write(
        value and 0xFF
    )

    output.write(
        value shr 8 and 0xFF
    )
}

