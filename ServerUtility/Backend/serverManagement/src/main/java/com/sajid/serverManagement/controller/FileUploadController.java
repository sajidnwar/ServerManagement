package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.FileOperationResponse;
import com.sajid.serverManagement.dto.FileUploadResponse;
import com.sajid.serverManagement.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadZipFile(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Received file upload request: {}", file.getOriginalFilename());

            String filePath = fileUploadService.uploadZipFile(file);

            FileUploadResponse response = new FileUploadResponse(
                true,
                "File uploaded successfully",
                filePath,
                file.getOriginalFilename(),
                file.getSize()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload request: {}", e.getMessage());
            FileUploadResponse response = new FileUploadResponse(false, e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            FileUploadResponse response = new FileUploadResponse(false, "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", e.getMessage(), e);
            FileUploadResponse response = new FileUploadResponse(false, "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<FileOperationResponse> deleteFile(@RequestParam("filePath") String filePath) {
        try {
            logger.info("Received file deletion request for: {}", filePath);

            boolean deleted = fileUploadService.deleteFile(filePath);

            FileOperationResponse response;
            if (deleted) {
                response = new FileOperationResponse(true, "File deleted successfully");
            } else {
                response = new FileOperationResponse(false, "File not found or could not be deleted");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting file: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<FileOperationResponse> getFileInfo(@RequestParam("filePath") String filePath) {
        try {
            boolean exists = fileUploadService.fileExists(filePath);

            if (!exists) {
                FileOperationResponse response = new FileOperationResponse(false, "File not found");
                return ResponseEntity.notFound().build();
            }

            long fileSize = fileUploadService.getFileSize(filePath);

            FileOperationResponse response = new FileOperationResponse(
                true,
                "File information retrieved successfully",
                filePath,
                true,
                fileSize
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error getting file info: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "Failed to get file information: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
