package com.byakushield.backend.dto;

public class ThreatResponse {

    private String module;
    private String threatLevel;
    private double riskScore;
    private String details;

    private String sanitizedData;
    private String sanitizedFilename;
    private String privacyReminder;

    public ThreatResponse(
            String module,
            String threatLevel,
            double riskScore,
            String details
    ) {
        this.module = module;
        this.threatLevel = threatLevel;
        this.riskScore = riskScore;
        this.details = details;
    }

    public ThreatResponse(
            String module,
            String threatLevel,
            double riskScore,
            String details,
            String sanitizedData,
            String sanitizedFilename,
            String privacyReminder
    ) {
        this.module = module;
        this.threatLevel = threatLevel;
        this.riskScore = riskScore;
        this.details = details;
        this.sanitizedData = sanitizedData;
        this.sanitizedFilename = sanitizedFilename;
        this.privacyReminder = privacyReminder;
    }

    public String getModule() {
        return module;
    }

    public String getThreatLevel() {
        return threatLevel;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getDetails() {
        return details;
    }

    public String getSanitizedData() {
        return sanitizedData;
    }

    public String getSanitizedFilename() {
        return sanitizedFilename;
    }

    public String getPrivacyReminder() {
        return privacyReminder;
    }
}