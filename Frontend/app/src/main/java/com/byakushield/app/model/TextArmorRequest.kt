package com.byakushield.app.model

data class TextArmorRequest(
    val message: String,
    val sender: String? = null
)