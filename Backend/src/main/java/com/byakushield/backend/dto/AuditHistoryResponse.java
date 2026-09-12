package com.byakushield.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class AuditHistoryResponse {

    private final Long id;
    private final String module;
    private final String threatLevel;
    private final double riskScore;
    private final String details;
    private final LocalDateTime timestamp;
    private final Map<String, Object> resultData;

    public AuditHistoryResponse(
            Long id,
            String module,
            String threatLevel,
            double riskScore,
            String details,
            LocalDateTime timestamp,
            Map<String, Object> resultData
    ) {
        this.id = id;
        this.module = module;
        this.threatLevel = threatLevel;
        this.riskScore = riskScore;
        this.details = details;
        this.timestamp = timestamp;
        this.resultData = resultData;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }
}