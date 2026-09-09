package com.byakushield.backend.dto;

import java.util.List;

public class OrchestratorResponse {

    private String verdict;
    private double riskScore;
    private List<ThreatResponse> moduleResults;
    private String sanitizedData;

    public OrchestratorResponse(
            String verdict,
            double riskScore,
            List<ThreatResponse> moduleResults,
            String sanitizedData
    ) {
        this.verdict = verdict;
        this.riskScore = riskScore;
        this.moduleResults = moduleResults;
        this.sanitizedData = sanitizedData;
    }

    public String getVerdict() {
        return verdict;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public List<ThreatResponse> getModuleResults() {
        return moduleResults;
    }

    public String getSanitizedData() {
        return sanitizedData;
    }
}