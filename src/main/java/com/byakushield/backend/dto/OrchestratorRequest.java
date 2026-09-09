package com.byakushield.backend.dto;

public class OrchestratorRequest {

    private String text;
    private String url;

    public OrchestratorRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}