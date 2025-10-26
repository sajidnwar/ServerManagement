package com.sajid.serverManagement.dto;

public class FileOperationResponse {
    private boolean success;
    private String message;
    private String filePath;
    private boolean exists;
    private long fileSize;

    public FileOperationResponse() {}

    public FileOperationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public FileOperationResponse(boolean success, String message, String filePath, boolean exists, long fileSize) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
        this.exists = exists;
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

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
