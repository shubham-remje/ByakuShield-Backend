package com.byakushield.backend.service;

import com.byakushield.backend.dto.TextArmorRequest;
import com.byakushield.backend.dto.ThreatResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TextArmorService {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE);

    private static final String[] URGENCY_KEYWORDS = {
            "urgent",
            "immediately",
            "act now",
            "verify now",
            "account suspended",
            "account blocked",
            "limited time"
    };

    private static final String[] CREDENTIAL_KEYWORDS = {
            "password",
            "otp",
            "one time password",
            "pin",
            "cvv",
            "login",
            "credentials",
            "verify your account"
    };

    private static final String[] FINANCIAL_KEYWORDS = {
            "bank",
            "payment",
            "refund",
            "prize",
            "lottery",
            "transaction",
            "credit card"
    };

    public ThreatResponse analyze(TextArmorRequest request) {

        // Basic sanitization
        String msg = request.getMessage();

        if (msg == null || msg.trim().isEmpty()) {
            return new ThreatResponse(
                    "TextArmor",
                    "LOW",
                    0.0,
                    "No text content was provided for analysis."
            );
        }

        String sanitizedMsg = msg
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        double score = 0.0;

        int urgencyMatches = countMatches(sanitizedMsg, URGENCY_KEYWORDS);
        int credentialMatches = countMatches(sanitizedMsg, CREDENTIAL_KEYWORDS);
        int financialMatches = countMatches(sanitizedMsg, FINANCIAL_KEYWORDS);

        // Social-engineering indicators
        if (urgencyMatches > 0) {
            score += 0.25;
        }

        if (credentialMatches > 0) {
            score += 0.25;
        }

        if (financialMatches > 0) {
            score += 0.15;
        }

        // Suspicious URL indicator
        if (URL_PATTERN.matcher(sanitizedMsg).find()) {
            score += 0.25;
        }

        // Strong phishing combination: financial lure + URL
        if (financialMatches > 0 && URL_PATTERN.matcher(sanitizedMsg).find()) {
            score += 0.20;
        }

        // Cap score at 1.0
        score = Math.min(score, 1.0);
        score = Math.round(score * 100.0) / 100.0;

        String level;

        if (score >= 0.50) {
            level = "HIGH";
        } else if (score >= 0.25) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        String details =
                "Text sanitized and analyzed for social-engineering keywords, " +
                        "credential requests, financial manipulation indicators, and suspicious URLs.";

        return new ThreatResponse(
                "TextArmor",
                level,
                score,
                details
        );
    }

    private int countMatches(String message, String[] keywords) {

        int count = 0;

        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                count++;
            }
        }

        return count;
    }
}