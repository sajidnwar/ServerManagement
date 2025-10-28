package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.ExtractRequest;
import com.sajid.serverManagement.dto.ExtractionStatus;
import com.sajid.serverManagement.dto.FileOperationResponse;
import com.sajid.serverManagement.dto.FileUploadResponse;
import com.sajid.serverManagement.service.AsyncExtractionService;
import com.sajid.serverManagement.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;
    private final AsyncExtractionService asyncExtractionService;

    public FileUploadController(FileUploadService fileUploadService, AsyncExtractionService asyncExtractionService) {
        this.fileUploadService = fileUploadService;
        this.asyncExtractionService = asyncExtractionService;
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

    @PostMapping("/upload-and-extract")
    public ResponseEntity<FileOperationResponse> uploadAndExtractZipFile(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Received file upload and extract request: {}", file.getOriginalFilename());

            String extractionPath = fileUploadService.uploadAndExtractZipFile(file);

            FileOperationResponse response = new FileOperationResponse(
                true,
                "File uploaded and extracted successfully",
                extractionPath,
                true,
                file.getSize()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload and extract request: {}", e.getMessage());
            FileOperationResponse response = new FileOperationResponse(false, e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            logger.error("Error uploading and extracting file: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "Failed to upload and extract file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            logger.error("Unexpected error during file upload and extract: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<FileOperationResponse> extractZipFile(@RequestBody ExtractRequest request) {
        try {
            String zipFilePath = request.getZipFilePath();
            if (zipFilePath == null || zipFilePath.trim().isEmpty()) {
                FileOperationResponse response = new FileOperationResponse(false, "zipFilePath is required");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Received file extraction request for: {}", zipFilePath);

            String extractionPath = fileUploadService.extractZipFile(zipFilePath);

            FileOperationResponse response = new FileOperationResponse(
                true,
                "File extracted successfully",
                extractionPath,
                true,
                0
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error extracting file: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "Failed to extract file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            logger.error("Unexpected error during file extraction: {}", e.getMessage(), e);
            FileOperationResponse response = new FileOperationResponse(false, "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/extract-async")
    public ResponseEntity<Map<String, Object>> extractZipFileAsync(@RequestBody ExtractRequest request) {
        try {
            String zipFilePath = request.getZipFilePath();
            if (zipFilePath == null || zipFilePath.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "zipFilePath is required");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Received async file extraction request for: {}", zipFilePath);

            String taskId = asyncExtractionService.startExtractionTask(zipFilePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Extraction task started");
            response.put("taskId", taskId);
            response.put("statusUrl", "/api/files/extraction-status/" + taskId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error starting async extraction: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to start extraction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/extraction-status/{taskId}")
    public ResponseEntity<ExtractionStatus> getExtractionStatus(@PathVariable String taskId) {
        try {
            ExtractionStatus status = asyncExtractionService.getExtractionStatus(taskId);

            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting extraction status for task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/extraction-status/{taskId}")
    public ResponseEntity<Map<String, Object>> cleanupExtractionTask(@PathVariable String taskId) {
        try {
            asyncExtractionService.cleanupCompletedTask(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task cleaned up successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error cleaning up extraction task {}: {}", taskId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cleanup task: " + e.getMessage());
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
