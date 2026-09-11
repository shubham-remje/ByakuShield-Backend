package com.byakushield.backend.service;

import com.byakushield.backend.dto.QuishGuardRequest;
import com.byakushield.backend.dto.ThreatResponse;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QuishGuardService {

    public ThreatResponse analyze(QuishGuardRequest request) {

        String url = request.getUrl();

        if (url == null || url.trim().isEmpty()) {
            return new ThreatResponse(
                    "QuishGuard",
                    "LOW",
                    0.0,
                    "No URL was provided for analysis."
            );
        }

        return analyzeUrl(url);
    }

    public ThreatResponse analyzeQr(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return new ThreatResponse(
                    "QuishGuard",
                    "LOW",
                    0.0,
                    "No QR image was provided for analysis."
            );
        }

        try {

            BufferedImage image =
                    ImageIO.read(file.getInputStream());

            if (image == null) {
                return new ThreatResponse(
                        "QuishGuard",
                        "LOW",
                        0.0,
                        "The uploaded file is not a valid image."
                );
            }

            String decodedUrl =
                    decodeQrRobustly(image);

            if (decodedUrl == null ||
                    decodedUrl.trim().isEmpty()) {

                return new ThreatResponse(
                        "QuishGuard",
                        "LOW",
                        0.0,
                        "QR image could not be decoded."
                );
            }

            return analyzeUrl(decodedUrl);

        } catch (IOException e) {

            return new ThreatResponse(
                    "QuishGuard",
                    "LOW",
                    0.0,
                    "The QR image could not be read."
            );

        } catch (Exception e) {

            return new ThreatResponse(
                    "QuishGuard",
                    "LOW",
                    0.0,
                    "QR image could not be decoded."
            );
        }
    }

    private String decodeQrRobustly(
            BufferedImage originalImage
    ) {

        Map<DecodeHintType, Object> hints =
                new EnumMap<>(DecodeHintType.class);

        hints.put(
                DecodeHintType.POSSIBLE_FORMATS,
                Collections.singletonList(
                        BarcodeFormat.QR_CODE
                )
        );

        hints.put(
                DecodeHintType.TRY_HARDER,
                Boolean.TRUE
        );

        /*
         * Attempt 1:
         * Original uploaded image.
         */
        String decoded =
                tryDecode(
                        originalImage,
                        hints,
                        false
                );

        if (decoded != null) {
            return decoded;
        }

        /*
         * Attempt 2:
         * Upscale the image. This helps when the QR
         * occupies only a small portion of the frame.
         */
        BufferedImage scaled =
                scaleImage(
                        originalImage,
                        2.0
                );

        decoded =
                tryDecode(
                        scaled,
                        hints,
                        false
                );

        if (decoded != null) {
            return decoded;
        }

        /*
         * Attempt 3:
         * Try the upscaled image using the global
         * histogram binarizer.
         */
        decoded =
                tryDecode(
                        scaled,
                        hints,
                        true
                );

        if (decoded != null) {
            return decoded;
        }

        /*
         * Attempts 4-6:
         * Try the original image at different rotations.
         */
        int[] rotations = {
                90,
                180,
                270
        };

        for (int rotation : rotations) {

            BufferedImage rotated =
                    rotateImage(
                            originalImage,
                            rotation
                    );

            decoded =
                    tryDecode(
                            rotated,
                            hints,
                            false
                    );

            if (decoded != null) {
                return decoded;
            }

            BufferedImage rotatedScaled =
                    scaleImage(
                            rotated,
                            2.0
                    );

            decoded =
                    tryDecode(
                            rotatedScaled,
                            hints,
                            false
                    );

            if (decoded != null) {
                return decoded;
            }

            decoded =
                    tryDecode(
                            rotatedScaled,
                            hints,
                            true
                    );

            if (decoded != null) {
                return decoded;
            }
        }

        /*
         * Final attempt:
         * Use a grayscale-friendly high-contrast
         * representation of the original image.
         */
        BufferedImage grayscale =
                createGrayscaleImage(
                        originalImage
                );

        decoded =
                tryDecode(
                        grayscale,
                        hints,
                        false
                );

        if (decoded != null) {
            return decoded;
        }

        return null;
    }

    private String tryDecode(
            BufferedImage image,
            Map<DecodeHintType, Object> hints,
            boolean useGlobalHistogram
    ) {

        try {

            BufferedImageLuminanceSource source =
                    new BufferedImageLuminanceSource(
                            image
                    );

            BinaryBitmap bitmap;

            if (useGlobalHistogram) {

                bitmap =
                        new BinaryBitmap(
                                new GlobalHistogramBinarizer(
                                        source
                                )
                        );

            } else {

                bitmap =
                        new BinaryBitmap(
                                new HybridBinarizer(
                                        source
                                )
                        );
            }

            Result result =
                    new MultiFormatReader().decode(
                            bitmap,
                            hints
                    );

            if (result != null &&
                    result.getText() != null &&
                    !result.getText().trim().isEmpty()) {

                return result.getText();
            }

        } catch (Exception ignored) {
            /*
             * This attempt failed.
             * The next decoding strategy will be tried.
             */
        }

        return null;
    }

    private BufferedImage scaleImage(
            BufferedImage source,
            double scale
    ) {

        int width =
                Math.max(
                        1,
                        (int) Math.round(
                                source.getWidth() * scale
                        )
                );

        int height =
                Math.max(
                        1,
                        (int) Math.round(
                                source.getHeight() * scale
                        )
                );

        BufferedImage scaled =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                scaled.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.drawImage(
                source,
                0,
                0,
                width,
                height,
                null
        );

        graphics.dispose();

        return scaled;
    }

    private BufferedImage rotateImage(
            BufferedImage source,
            int degrees
    ) {

        int width =
                source.getWidth();

        int height =
                source.getHeight();

        boolean swapDimensions =
                degrees == 90 ||
                        degrees == 270;

        int newWidth =
                swapDimensions
                        ? height
                        : width;

        int newHeight =
                swapDimensions
                        ? width
                        : height;

        BufferedImage rotated =
                new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                rotated.createGraphics();

        switch (degrees) {

            case 90:

                graphics.translate(
                        newWidth,
                        0
                );

                graphics.rotate(
                        Math.toRadians(90)
                );

                break;

            case 180:

                graphics.translate(
                        newWidth,
                        newHeight
                );

                graphics.rotate(
                        Math.toRadians(180)
                );

                break;

            case 270:

                graphics.translate(
                        0,
                        newHeight
                );

                graphics.rotate(
                        Math.toRadians(270)
                );

                break;

            default:
                break;
        }

        graphics.drawImage(
                source,
                0,
                0,
                null
        );

        graphics.dispose();

        return rotated;
    }

    private BufferedImage createGrayscaleImage(
            BufferedImage source
    ) {

        BufferedImage grayscale =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY
                );

        Graphics2D graphics =
                grayscale.createGraphics();

        graphics.drawImage(
                source,
                0,
                0,
                null
        );

        graphics.dispose();

        return grayscale;
    }

    private ThreatResponse analyzeUrl(String url) {

        String normalizedUrl =
                url.trim().toLowerCase();

        double score = 0.0;

        if (normalizedUrl.endsWith(".xyz")
                || normalizedUrl.contains(".xyz/")
                || normalizedUrl.endsWith(".top")
                || normalizedUrl.contains(".top/")) {

            score += 0.40;
        }

        if (normalizedUrl.contains("login")
                || normalizedUrl.contains("verify")
                || normalizedUrl.contains("signin")
                || normalizedUrl.contains("account")) {

            score += 0.25;
        }

        if (normalizedUrl.contains("bit.ly")
                || normalizedUrl.contains("tinyurl.com")
                || normalizedUrl.contains("t.co")) {

            score += 0.25;
        }

        try {

            URI uri =
                    URI.create(normalizedUrl);

            if (uri.getScheme() == null
                    || uri.getHost() == null
                    || !(uri.getScheme().equals("http")
                    || uri.getScheme().equals("https"))) {

                score += 0.30;
            }

        } catch (IllegalArgumentException e) {

            score += 0.40;
        }

        score =
                Math.min(
                        score,
                        1.0
                );

        score =
                Math.round(
                        score * 100.0
                ) / 100.0;

        String level;

        if (score >= 0.60) {

            level = "HIGH";

        } else if (score >= 0.25) {

            level = "MEDIUM";

        } else {

            level = "LOW";
        }

        return new ThreatResponse(
                "QuishGuard",
                level,
                score,
                "Decoded QR/URI and analyzed URI structure, suspicious domain patterns, credential-related paths, and URL-shortening indicators."
        );
    }
}