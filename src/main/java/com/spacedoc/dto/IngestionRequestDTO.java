package com.spacedoc.dto;

public class IngestionRequestDTO {
    private String sourceUrl;
    private String title;
    private String organization;

    public IngestionRequestDTO() {}

    public IngestionRequestDTO(String sourceUrl, String title, String organization) {
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.organization = organization;
    }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
}
