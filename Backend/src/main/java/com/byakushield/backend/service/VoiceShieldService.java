package com.byakushield.backend.service;

import ai.realitydefender.RealityDefender;
import ai.realitydefender.exceptions.RealityDefenderException;
import ai.realitydefender.models.DetectionResult;
import com.byakushield.backend.dto.ThreatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Service
public class VoiceShieldService {

    public ThreatResponse analyze(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "No audio file was provided for analysis."
            );
        }

        File tempFile = null;

        try {
            /*
             * Preserve the original file extension.
             *
             * Reality Defender supports multiple audio formats,
             * so we should not incorrectly label every upload as WAV.
             */
            String extension = getFileExtension(file.getOriginalFilename());

            tempFile = File.createTempFile(
                    "voiceshield-",
                    extension
            );

            file.transferTo(tempFile);

            /*
             * Primary detection:
             * Reality Defender deepfake / synthetic-media analysis.
             */
            try {
                RealityDefender client =
                        RealityDefender.builder()
                                .apiKey(
                                        System.getenv(
                                                "REALITY_DEFENDER_API_KEY"
                                        )
                                )
                                .build();

                try (client) {

                    DetectionResult result =
                            client.detectFile(tempFile);

                    return buildRealityDefenderResponse(
                            result,
                            tempFile
                    );
                }

            } catch (RealityDefenderException
                     | RuntimeException e) {

                /*
                 * Reality Defender is an external dependency.
                 *
                 * If it is temporarily unavailable, do not make
                 * the entire VoiceShield module fail. Fall back
                 * to the existing local acoustic analysis.
                 */
                return analyzeWithLocalFallback(
                        tempFile,
                        e
                );
            }

        } catch (IOException e) {

            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "The audio file could not be read for analysis."
            );

        } finally {

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Converts the Reality Defender result into the existing
     * ByakuShield ThreatResponse contract.
     */
    private ThreatResponse buildRealityDefenderResponse(
            DetectionResult result,
            File audioFile
    ) {

        if (result == null) {

            return analyzeWithLocalFallback(
                    audioFile,
                    new IllegalStateException(
                            "Reality Defender returned no detection result."
                    )
            );
        }

        String status = result.getStatus();

        if (status == null || status.isBlank()) {
            status = "UNKNOWN";
        }

        Double detectedScore = result.getScore();

        /*
         * Reality Defender documents its normalized score
         * in the 0–1 range.
         *
         * If the score is unavailable, use the terminal status
         * only as a conservative fallback rather than pretending
         * that we have a precise AI confidence value.
         */
        double score;

        if (detectedScore != null) {

            score = clamp(
                    detectedScore,
                    0.0,
                    1.0
            );

        } else {

            score = scoreFromStatus(status);
        }

        score =
                Math.round(score * 100.0) / 100.0;

        String level =
                calculateThreatLevel(score);

        String details =
                buildRealityDefenderDetails(
                        result,
                        status,
                        score
                );

        return new ThreatResponse(
                "VoiceShield",
                level,
                score,
                details
        );
    }

    /**
     * Builds a detailed response while keeping the original
     * VoiceShield response format.
     */
    private String buildRealityDefenderDetails(
            DetectionResult result,
            String status,
            double score
    ) {

        StringBuilder details =
                new StringBuilder();

        details.append(
                "Audio analyzed using Reality Defender deepfake "
                        + "detection. "
        );

        details.append(
                "Detection status: "
                        + status
                        + ". "
        );

        details.append(
                "Normalized AI risk score: "
                        + score
                        + ". "
        );

        if ("MANIPULATED".equalsIgnoreCase(status)) {

            details.append(
                    "The external AI detector identified characteristics "
                            + "consistent with artificially generated or "
                            + "manipulated audio. "
            );

        } else if ("AUTHENTIC".equalsIgnoreCase(status)) {

            details.append(
                    "The external AI detector found the audio consistent "
                            + "with authentic speech. "
            );

        } else {

            details.append(
                    "The external AI detector returned an inconclusive "
                            + "or non-standard result. "
            );
        }

        /*
         * Include individual model information when available.
         * This makes the backend response useful for audit logs
         * without exposing unnecessary raw API data.
         */
        if (result.getModels() != null
                && !result.getModels().isEmpty()) {

            details.append(
                    "Individual detection models: "
            );

            for (int i = 0;
                 i < result.getModels().size();
                 i++) {

                DetectionResult.ModelResult model =
                        result.getModels().get(i);

                if (model == null) {
                    continue;
                }

                if (i > 0) {
                    details.append("; ");
                }

                details.append(
                        model.getName()
                                + "="
                                + model.getStatus()
                );

                if (model.getFinalScore() != null) {

                    details.append(
                            " (finalScore="
                                    + model.getFinalScore()
                                    + ")"
                    );
                }
            }

            details.append(". ");
        }

        return details.toString();
    }

    /**
     * Local acoustic fallback.
     *
     * This does NOT claim to provide the same capability as
     * Reality Defender. It is retained so VoiceShield can
     * still function when the external detector is unavailable.
     */
    private ThreatResponse analyzeWithLocalFallback(
            File tempFile,
            Exception externalException
    ) {

        String extension =
                getFileExtension(
                        tempFile.getName()
                );

        /*
         * Java Sound's built-in decoder is primarily useful for WAV
         * and other locally supported formats. If the uploaded
         * format cannot be decoded, return an explicit fallback
         * result rather than pretending the audio was analyzed.
         */
        if (!".wav".equalsIgnoreCase(extension)) {

            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "Reality Defender external analysis was unavailable. "
                            + "Local acoustic fallback could not analyze "
                            + "the uploaded "
                            + extension
                            + " file because the local Java audio decoder "
                            + "does not support this format. "
                            + "External detector error: "
                            + safeErrorMessage(externalException)
            );
        }

        try (AudioInputStream audioStream =
                     AudioSystem.getAudioInputStream(tempFile)) {

            AudioFormat format =
                    audioStream.getFormat();

            double duration = 0.0;

            if (format.getFrameRate() > 0) {

                duration =
                        audioStream.getFrameLength()
                                / format.getFrameRate();
            }

            double score = 0.0;

            /*
             * Existing local acoustic checks retained.
             */
            if (format.getSampleRate() < 8000) {
                score += 0.20;
            }

            if (format.getChannels() > 2) {
                score += 0.10;
            }

            if (duration < 1.0) {
                score += 0.20;
            }

            double amplitudeVariation =
                    calculateAmplitudeVariation(
                            audioStream,
                            format
                    );

            if (amplitudeVariation < 0.05) {

                score += 0.25;

            } else if (amplitudeVariation < 0.10) {

                score += 0.10;
            }

            score =
                    Math.min(
                            score,
                            1.0
                    );

            score =
                    Math.round(score * 100.0)
                            / 100.0;

            String level =
                    calculateThreatLevel(score);

            String details =
                    "Reality Defender external analysis was unavailable. "
                            + "Local acoustic fallback was used. "
                            + "Analyzed audio duration, sample rate, "
                            + "channel configuration, waveform amplitude "
                            + "variation, and recording characteristics "
                            + "using heuristic checks. "
                            + "External detector error: "
                            + safeErrorMessage(externalException);

            return new ThreatResponse(
                    "VoiceShield",
                    level,
                    score,
                    details
            );

        } catch (UnsupportedAudioFileException e) {

            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "Reality Defender external analysis was unavailable "
                            + "and the uploaded WAV file could not be "
                            + "decoded by the local audio analyzer."
            );

        } catch (IOException e) {

            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "Reality Defender external analysis was unavailable "
                            + "and the audio file could not be read by "
                            + "the local fallback analyzer."
            );
        }
    }

    private double calculateAmplitudeVariation(
            AudioInputStream audioStream,
            AudioFormat format
    ) throws IOException {

        byte[] buffer =
                new byte[4096];

        double sum = 0.0;
        double sumSquared = 0.0;
        long sampleCount = 0;

        int bytesPerSample =
                format.getSampleSizeInBits() / 8;

        if (bytesPerSample <= 0) {
            return 1.0;
        }

        int bytesRead;

        while ((bytesRead =
                audioStream.read(buffer)) != -1) {

            for (int i = 0;
                 i + bytesPerSample <= bytesRead;
                 i += bytesPerSample) {

                int sample = 0;

                if (bytesPerSample == 2) {

                    sample =
                            (buffer[i + 1] << 8)
                                    | (buffer[i] & 0xff);

                } else if (bytesPerSample == 1) {

                    sample =
                            buffer[i];
                }

                double normalized =
                        Math.abs(sample) / 32768.0;

                sum += normalized;

                sumSquared +=
                        normalized * normalized;

                sampleCount++;
            }
        }

        if (sampleCount == 0) {
            return 1.0;
        }

        double mean =
                sum / sampleCount;

        double variance =
                (sumSquared / sampleCount)
                        - (mean * mean);

        return Math.sqrt(
                Math.max(
                        variance,
                        0.0
                )
        );
    }

    /**
     * Maps the normalized AI risk score to the existing
     * ByakuShield threat levels.
     */
    private String calculateThreatLevel(
            double score
    ) {

        if (score >= 0.60) {
            return "HIGH";
        }

        if (score >= 0.25) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**
     * Conservative status-only fallback when Reality Defender
     * does not provide a numeric score.
     */
    private double scoreFromStatus(
            String status
    ) {

        if ("MANIPULATED".equalsIgnoreCase(status)) {
            return 0.80;
        }

        if ("AUTHENTIC".equalsIgnoreCase(status)) {
            return 0.05;
        }

        return 0.0;
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private String getFileExtension(
            String filename
    ) {

        if (filename == null
                || filename.isBlank()) {

            return ".wav";
        }

        String lower =
                filename.toLowerCase(
                        Locale.ROOT
                );

        int dotIndex =
                lower.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == lower.length() - 1) {

            return ".wav";
        }

        return lower.substring(dotIndex);
    }

    private String safeErrorMessage(
            Exception exception
    ) {

        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "Unknown external detection error.";
        }

        return exception.getMessage();
    }
}