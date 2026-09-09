package com.byakushield.backend.service;

import com.byakushield.backend.dto.ThreatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

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
            tempFile = File.createTempFile("voiceshield-", ".wav");
            file.transferTo(tempFile);

            try (AudioInputStream audioStream =
                         AudioSystem.getAudioInputStream(tempFile)) {

                AudioFormat format = audioStream.getFormat();

                double duration = 0.0;

                if (format.getFrameRate() > 0) {
                    duration =
                            audioStream.getFrameLength()
                                    / format.getFrameRate();
                }

                double score = 0.0;

                // Basic audio structure checks
                if (format.getSampleRate() < 8000) {
                    score += 0.20;
                }

                if (format.getChannels() > 2) {
                    score += 0.10;
                }

                if (duration < 1.0) {
                    score += 0.20;
                }

                // Analyze waveform amplitude
                double amplitudeVariation =
                        calculateAmplitudeVariation(audioStream, format);

                // Very low amplitude variation can indicate
                // an unusually uniform or synthetic signal.
                if (amplitudeVariation < 0.05) {
                    score += 0.25;
                }

                // Moderate variation receives a smaller suspicion score.
                else if (amplitudeVariation < 0.10) {
                    score += 0.10;
                }

                score = Math.min(score, 1.0);
                score = Math.round(score * 100.0) / 100.0;

                String level;

                if (score >= 0.60) {
                    level = "HIGH";
                } else if (score >= 0.25) {
                    level = "MEDIUM";
                } else {
                    level = "LOW";
                }

                String details =
                        "Analyzed audio duration, sample rate, channel configuration, " +
                                "waveform amplitude variation, and recording characteristics " +
                                "using heuristic checks.";

                return new ThreatResponse(
                        "VoiceShield",
                        level,
                        score,
                        details
                );
            }

        } catch (UnsupportedAudioFileException e) {

            return new ThreatResponse(
                    "VoiceShield",
                    "LOW",
                    0.0,
                    "The uploaded file format is not supported for audio analysis."
            );

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

    private double calculateAmplitudeVariation(
            AudioInputStream audioStream,
            AudioFormat format
    ) throws IOException {

        byte[] buffer = new byte[4096];

        double sum = 0.0;
        double sumSquared = 0.0;
        long sampleCount = 0;

        int bytesPerSample = format.getSampleSizeInBits() / 8;

        if (bytesPerSample <= 0) {
            return 1.0;
        }

        int bytesRead;

        while ((bytesRead = audioStream.read(buffer)) != -1) {

            for (int i = 0;
                 i + bytesPerSample <= bytesRead;
                 i += bytesPerSample) {

                int sample = 0;

                if (bytesPerSample == 2) {
                    sample = (buffer[i + 1] << 8) | (buffer[i] & 0xff);

                } else if (bytesPerSample == 1) {
                    sample = buffer[i];
                }

                double normalized =
                        Math.abs(sample) / 32768.0;

                sum += normalized;
                sumSquared += normalized * normalized;
                sampleCount++;
            }
        }

        if (sampleCount == 0) {
            return 1.0;
        }

        double mean = sum / sampleCount;

        double variance =
                (sumSquared / sampleCount) - (mean * mean);

        return Math.sqrt(Math.max(variance, 0.0));
    }
}