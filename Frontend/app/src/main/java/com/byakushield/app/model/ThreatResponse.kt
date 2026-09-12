package com.byakushield.app.model

data class ThreatResponse(
    val module: String,
    val threatLevel: String,
    val riskScore: Double,
    val details: String,
    val sanitizedData: String? = null,
    val sanitizedFilename: String? = null,
    val privacyReminder: String? = null
)