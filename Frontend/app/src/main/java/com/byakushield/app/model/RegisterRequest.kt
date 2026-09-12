package com.byakushield.app.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)