package com.byakushield.app.model

data class OrchestratorResponse(
    val verdict: String,
    val riskScore: Double,
    val moduleResults: List<ThreatResponse>,
    val sanitizedData: String?
)