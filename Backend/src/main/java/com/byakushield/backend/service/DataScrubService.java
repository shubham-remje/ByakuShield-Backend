package com.byakushield.backend.service;

import ai.realitydefender.RealityDefender;
import ai.realitydefender.exceptions.RealityDefenderException;
import ai.realitydefender.models.DetectionResult;

import com.byakushield.backend.dto.ThreatResponse;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class DataScrubService {

    public ThreatResponse scrub(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            return new ThreatResponse(
                    "DataScrub",
                    "CLEARED",
                    0.0,
                    "No media file was provided for metadata analysis."
            );
        }

        File tempFile = null;

        try {

            /*
             * Read the uploaded file once.
             *
             * This prevents problems where transferTo() moves
             * the underlying temporary file.
             */
            byte[] originalBytes =
                    file.getBytes();

            if (originalBytes.length == 0) {

                return new ThreatResponse(
                        "DataScrub",
                        "CLEARED",
                        0.0,
                        "The uploaded media file is empty."
                );
            }

            /*
             * Create a temporary copy for:
             *
             * 1. metadata-extractor
             * 2. Reality Defender
             */
            tempFile =
                    createTempFile(file);

            java.nio.file.Files.write(
                    tempFile.toPath(),
                    originalBytes
            );

            /*
             * Analyze metadata.
             */
            Metadata metadata =
                    ImageMetadataReader.readMetadata(
                            tempFile
                    );

            List<String> sensitiveTags =
                    findSensitiveTags(metadata);

            double score =
                    calculateRiskScore(
                            sensitiveTags,
                            metadata
                    );

            String level =
                    calculateThreatLevel(score);

            /*
             * Generate sanitized image.
             */
            String sanitizedData =
                    createSanitizedData(
                            originalBytes
                    );

            String sanitizedFilename =
                    buildSanitizedFilename(
                            file.getOriginalFilename()
                    );

            String privacyReminder =
                    "The original image was not deleted. " +
                            "Delete the original manually if you no longer " +
                            "want the unsanitized copy on your device.";

            /*
             * Optional Reality Defender AI demonstration.
             *
             * This does NOT affect the DataScrub privacy score.
             */
            String aiAnalysis =
                    analyzeWithRealityDefender(
                            tempFile
                    );

            String details;

            if (sanitizedData == null) {

                details =
                        buildDetails(
                                sensitiveTags
                        ) +
                                " However, the sanitized copy could not " +
                                "be generated.";

            } else {

                details =
                        buildDetails(
                                sensitiveTags
                        );
            }

            /*
             * Append AI authenticity information.
             *
             * The existing DataScrub privacy score remains
             * completely independent.
             */
            if (
                    aiAnalysis != null &&
                            !aiAnalysis.isBlank()
            ) {

                details +=
                        " " +
                                aiAnalysis;
            }

            return new ThreatResponse(
                    "DataScrub",
                    level,
                    score,
                    details,
                    sanitizedData,
                    sanitizedFilename,
                    privacyReminder
            );

        } catch (Exception e) {

            System.err.println(
                    "DataScrub: Media processing failed."
            );

            e.printStackTrace();

            return new ThreatResponse(
                    "DataScrub",
                    "LOW",
                    0.0,
                    "The media file could not be inspected for metadata."
            );

        } finally {

            if (
                    tempFile != null &&
                            tempFile.exists()
            ) {

                tempFile.delete();
            }
        }
    }

    /**
     * Optional AI media-authenticity analysis using
     * Reality Defender.
     *
     * This feature is only a demonstration feature.
     * If Reality Defender fails, the normal DataScrub
     * metadata/privacy functionality continues working.
     */
    private String analyzeWithRealityDefender(
            File imageFile
    ) {

        if (
                imageFile == null ||
                        !imageFile.exists()
        ) {

            return
                    "AI media analysis was unavailable because " +
                            "the temporary image file could not be accessed.";
        }

        try {

            String apiKey =
                    System.getenv(
                            "REALITY_DEFENDER_API_KEY"
                    );

            if (
                    apiKey == null ||
                            apiKey.isBlank()
            ) {

                return
                        "AI media authenticity analysis was skipped " +
                                "because the Reality Defender API key " +
                                "is not configured.";
            }

            RealityDefender client =
                    RealityDefender.builder()
                            .apiKey(apiKey)
                            .build();

            try (client) {

                /*
                 * Upload the image and wait for the detection result.
                 */
                DetectionResult result =
                        client.detectFile(
                                imageFile
                        );

                /*
                 * All SDK result-accessor calls are isolated inside
                 * buildRealityDefenderDetails().
                 *
                 * That method declares Exception because the current
                 * SDK exposes Jackson checked exceptions from its
                 * result accessors.
                 */
                return buildRealityDefenderDetails(
                        result
                );
            }

        } catch (Exception e) {

            /*
             * Reality Defender is an optional demo capability.
             *
             * Any API/SDK/Jackson problem must not break the
             * existing DataScrub privacy functionality.
             */
            System.err.println(
                    "DataScrub: Reality Defender image analysis " +
                            "was unavailable."
            );

            e.printStackTrace();

            return
                    "AI media authenticity analysis was unavailable. " +
                            "The existing metadata privacy analysis " +
                            "and sanitization were still completed.";
        }
    }

    /**
     * Converts the Reality Defender result into a concise,
     * human-readable DataScrub detail.
     *
     * IMPORTANT:
     *
     * The current Reality Defender SDK exposes Jackson checked
     * exceptions from some of its result accessors. Therefore,
     * this method deliberately declares Exception instead of
     * trying to catch JsonProcessingException around individual
     * SDK methods.
     */
    private String buildRealityDefenderDetails(
            DetectionResult result
    ) throws Exception {

        if (result == null) {

            return
                    "AI media authenticity analysis returned no result.";
        }

        /*
         * Reality Defender status.
         */
        String status =
                result.getStatus();

        if (
                status == null ||
                        status.isBlank()
        ) {

            status = "UNKNOWN";
        }

        /*
         * We intentionally use a conservative demonstration
         * score derived from the detection status.
         *
         * This is NOT the official Reality Defender numeric
         * score and does not affect DataScrub's privacy score.
         */
        double aiScore =
                scoreFromStatus(
                        status
                );

        aiScore =
                Math.round(
                        aiScore * 100.0
                ) / 100.0;

        StringBuilder details =
                new StringBuilder();

        details.append(
                "AI media authenticity analysis was performed " +
                        "using Reality Defender. "
        );

        details.append(
                "Detection status: "
                        + status
                        + ". "
        );

        details.append(
                "Normalized AI risk score: "
                        + aiScore
                        + ". "
        );

        if (
                "MANIPULATED".equalsIgnoreCase(
                        status
                )
        ) {

            details.append(
                    "The external AI detector identified " +
                            "characteristics consistent with an " +
                            "AI-generated or manipulated image. "
            );

        } else if (
                "AUTHENTIC".equalsIgnoreCase(
                        status
                )
        ) {

            details.append(
                    "The external AI detector found the image " +
                            "consistent with authentic media. "
            );

        } else {

            details.append(
                    "The external AI detector returned an " +
                            "inconclusive or non-standard result. "
            );
        }

        /*
         * Add individual model statuses when available.
         *
         * Numeric model scores are deliberately not accessed.
         */
        if (
                result.getModels() != null &&
                        !result.getModels().isEmpty()
        ) {

            details.append(
                    "Individual detection models: "
            );

            boolean firstModel = true;

            for (
                    DetectionResult.ModelResult model :
                    result.getModels()
            ) {

                if (model == null) {
                    continue;
                }

                if (!firstModel) {

                    details.append(
                            "; "
                    );
                }

                details.append(
                        model.getName()
                                + "="
                                + model.getStatus()
                );

                firstModel = false;
            }

            details.append(
                    ". "
            );
        }

        return details.toString();
    }

    /**
     * Conservative status-only fallback for the demo AI score.
     */
    private double scoreFromStatus(
            String status
    ) {

        if (
                "MANIPULATED".equalsIgnoreCase(
                        status
                )
        ) {

            return 0.80;
        }

        if (
                "AUTHENTIC".equalsIgnoreCase(
                        status
                )
        ) {

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

    /**
     * Creates a sanitized JPEG from the supplied image bytes
     * and returns the result as Base64.
     *
     * The image is decoded and re-encoded into a new JPEG,
     * preventing the original EXIF metadata from being copied
     * into the sanitized file.
     */
    public String createSanitizedData(
            byte[] originalBytes
    ) {

        if (
                originalBytes == null ||
                        originalBytes.length == 0
        ) {

            return null;
        }

        try {

            BufferedImage image =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    originalBytes
                            )
                    );

            if (image == null) {

                System.err.println(
                        "DataScrub: ImageIO could not decode " +
                                "the uploaded image."
                );

                return null;
            }

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            boolean written =
                    ImageIO.write(
                            image,
                            "jpg",
                            outputStream
                    );

            if (!written) {

                System.err.println(
                        "DataScrub: No JPEG ImageIO writer " +
                                "was available."
                );

                return null;
            }

            byte[] sanitizedBytes =
                    outputStream.toByteArray();

            if (sanitizedBytes.length == 0) {

                System.err.println(
                        "DataScrub: Sanitized image contained " +
                                "zero bytes."
                );

                return null;
            }

            System.out.println(
                    "DataScrub: Sanitized image generated successfully. " +
                            "Original bytes=" +
                            originalBytes.length +
                            ", sanitized bytes=" +
                            sanitizedBytes.length
            );

            return Base64.getEncoder()
                    .encodeToString(
                            sanitizedBytes
                    );

        } catch (Exception e) {

            System.err.println(
                    "DataScrub: Failed to generate sanitized image."
            );

            e.printStackTrace();

            return null;
        }
    }

    private File createTempFile(
            MultipartFile file
    ) throws IOException {

        String extension =
                getExtension(
                        file.getOriginalFilename()
                );

        return File.createTempFile(
                "datascrub-",
                "." + extension
        );
    }

    private List<String> findSensitiveTags(
            Metadata metadata
    ) {

        List<String> sensitiveTags =
                new ArrayList<>();

        for (
                Directory directory :
                metadata.getDirectories()
        ) {

            for (
                    Tag tag :
                    directory.getTags()
            ) {

                String tagName =
                        tag.getTagName()
                                .toLowerCase();

                if (
                        tagName.contains("gps")
                                || tagName.contains("location")
                                || tagName.contains("latitude")
                                || tagName.contains("longitude")
                                || tagName.contains("camera")
                                || tagName.contains("device")
                                || tagName.contains("serial")
                                || tagName.contains("software")
                ) {

                    sensitiveTags.add(
                            tag.getTagName()
                    );
                }
            }
        }

        return sensitiveTags;
    }

    private double calculateRiskScore(
            List<String> sensitiveTags,
            Metadata metadata
    ) {

        double score = 0.0;

        if (!sensitiveTags.isEmpty()) {

            score = 0.40;
        }

        if (containsGpsMetadata(metadata)) {

            score += 0.30;
        }

        score =
                Math.min(
                        score,
                        1.0
                );

        return Math.round(
                score * 100.0
        ) / 100.0;
    }

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

    private String buildDetails(
            List<String> sensitiveTags
    ) {

        if (sensitiveTags.isEmpty()) {

            return
                    "Media metadata was inspected. No sensitive EXIF " +
                            "location or device-identification data was detected. " +
                            "A sanitized copy was generated for safe saving.";
        }

        return
                "Media metadata was inspected and sensitive EXIF " +
                        "metadata indicators were identified. The image was " +
                        "re-encoded to remove embedded metadata. A sanitized " +
                        "copy is available for saving.";
    }

    private boolean containsGpsMetadata(
            Metadata metadata
    ) {

        for (
                Directory directory :
                metadata.getDirectories()
        ) {

            String directoryName =
                    directory.getName()
                            .toLowerCase();

            if (
                    directoryName.contains("gps")
            ) {

                return true;
            }
        }

        return false;
    }

    private String getExtension(
            String filename
    ) {

        if (
                filename == null ||
                        !filename.contains(".")
        ) {

            return "tmp";
        }

        return filename.substring(
                filename.lastIndexOf('.') + 1
        );
    }

    private String buildSanitizedFilename(
            String originalFilename
    ) {

        if (
                originalFilename == null ||
                        originalFilename.isBlank()
        ) {

            return "byakushield_sanitized.jpg";
        }

        String baseName =
                originalFilename;

        int dotIndex =
                baseName.lastIndexOf('.');

        if (dotIndex > 0) {

            baseName =
                    baseName.substring(
                            0,
                            dotIndex
                    );
        }

        return baseName +
                "_byakushield_sanitized.jpg";
    }
}