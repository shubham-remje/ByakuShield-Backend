package com.byakushield.backend.service;

import com.byakushield.backend.dto.QuishGuardRequest;
import com.byakushield.backend.dto.ThreatResponse;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

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
            BufferedImage image = ImageIO.read(file.getInputStream());

            if (image == null) {
                return new ThreatResponse(
                        "QuishGuard",
                        "LOW",
                        0.0,
                        "The uploaded file is not a valid image."
                );
            }

            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(
                            new BufferedImageLuminanceSource(image)
                    )
            );

            Result result = new MultiFormatReader().decode(bitmap);

            String decodedUrl = result.getText();

            return analyzeUrl(decodedUrl);

        } catch (Exception e) {

            return new ThreatResponse(
                    "QuishGuard",
                    "LOW",
                    0.0,
                    "QR image could not be decoded."
            );
        }
    }

    private ThreatResponse analyzeUrl(String url) {

        String normalizedUrl = url.trim().toLowerCase();
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
            URI uri = URI.create(normalizedUrl);

            if (uri.getScheme() == null
                    || uri.getHost() == null
                    || !(uri.getScheme().equals("http")
                    || uri.getScheme().equals("https"))) {
                score += 0.30;
            }

        } catch (IllegalArgumentException e) {
            score += 0.40;
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

        return new ThreatResponse(
                "QuishGuard",
                level,
                score,
                "Decoded QR/URI and analyzed URI structure, suspicious domain patterns, credential-related paths, and URL-shortening indicators."
        );
    }
}