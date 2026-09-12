package com.byakushield.app.model

data class AuditHistoryResponse(
    val id: Long,
    val module: String,
    val threatLevel: String,
    val riskScore: Double,
    val details: String,
    val timestamp: String?,
    val resultData: Map<String, Any>?
)