package com.sajid.serverManagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${server.base-path}")
    private String basePath;

    private static final String[] ALLOWED_EXTENSIONS = {".zip"};
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public String uploadZipFile(MultipartFile file) throws IOException {
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(basePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename with timestamp
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = removeFileExtension(originalFilename) + "_" + timestamp + fileExtension;

        // Save the file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Successfully uploaded file: {} to path: {}", filename, filePath);
        return filePath.toString();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        String fileExtension = getFileExtension(filename);
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equalsIgnoreCase(fileExtension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new IllegalArgumentException("Only ZIP files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String removeFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return filename;
        }
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                logger.info("Successfully deleted file: {}", filePath);
            } else {
                logger.warn("File not found for deletion: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            logger.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }

    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
}
