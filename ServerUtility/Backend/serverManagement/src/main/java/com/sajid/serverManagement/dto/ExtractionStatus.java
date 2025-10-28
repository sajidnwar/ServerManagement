package com.sajid.serverManagement.dto;

public class ExtractionStatus {
    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    private String taskId;
    private Status status;
    private String message;
    private String zipFilePath;
    private String extractionPath;
    private int progressPercentage;
    private long startTime;
    private long endTime;
    private String errorMessage;

    public ExtractionStatus() {}

    public ExtractionStatus(String taskId, Status status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
        this.startTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getZipFilePath() {
        return zipFilePath;
    }

    public void setZipFilePath(String zipFilePath) {
        this.zipFilePath = zipFilePath;
    }

    public String getExtractionPath() {
        return extractionPath;
    }

    public void setExtractionPath(String extractionPath) {
        this.extractionPath = extractionPath;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getDurationMs() {
        if (endTime > 0 && startTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }
}
