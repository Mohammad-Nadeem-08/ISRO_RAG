package com.spacedoc.dto;

public class IngestionResponseDTO {
    private String jobKey;
    private String status;
    private String errorMessage;
    private Integer progressPercent;

    public IngestionResponseDTO() {}

    public IngestionResponseDTO(String jobKey, String status, String errorMessage, Integer progressPercent) {
        this.jobKey = jobKey;
        this.status = status;
        this.errorMessage = errorMessage;
        this.progressPercent = progressPercent;
    }

    public String getJobKey() { return jobKey; }
    public void setJobKey(String jobKey) { this.jobKey = jobKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
}
