package com.byakushield.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TextArmorRequest {

    @NotBlank(message = "Message is required")
    @Size(
            max = 5000,
            message = "Message must not exceed 5000 characters"
    )
    private String message;

    @Size(
            max = 255,
            message = "Sender must not exceed 255 characters"
    )
    private String sender;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}