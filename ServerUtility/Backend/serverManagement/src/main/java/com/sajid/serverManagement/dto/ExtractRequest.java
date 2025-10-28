package com.sajid.serverManagement.dto;

public class ExtractRequest {
    private String zipFilePath;

    public ExtractRequest() {}

    public ExtractRequest(String zipFilePath) {
        this.zipFilePath = zipFilePath;
    }

    public String getZipFilePath() {
        return zipFilePath;
    }

    public void setZipFilePath(String zipFilePath) {
        this.zipFilePath = zipFilePath;
    }
}
