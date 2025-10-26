package com.sajid.serverManagement.dto;

public class FileUploadResponse {
    private boolean success;
    private String message;
    private String filePath;
    private String originalFilename;
    private long fileSize;

    public FileUploadResponse() {}

    public FileUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public FileUploadResponse(boolean success, String message, String filePath, String originalFilename, long fileSize) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
