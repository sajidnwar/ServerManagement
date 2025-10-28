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

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${server.base-path}")
    private String basePath;

    private static final String[] ALLOWED_EXTENSIONS = {".zip"};
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB
    private static final String SERVER_ZIP_FOLDER = "ServerZip";

    public String uploadZipFile(MultipartFile file) throws IOException {
        validateFile(file);

        // Create ServerZip directory inside base path if it doesn't exist
        Path basePathDir = Paths.get(basePath);
        Path serverZipPath = basePathDir.resolve(SERVER_ZIP_FOLDER);

        if (!Files.exists(serverZipPath)) {
            Files.createDirectories(serverZipPath);
            logger.info("Created ServerZip directory: {}", serverZipPath);
        }

        // Use original filename without timestamp
        String originalFilename = file.getOriginalFilename();

        // Save the file inside ServerZip folder with original name
        Path filePath = serverZipPath.resolve(originalFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Successfully uploaded file: {} to path: {}", originalFilename, filePath);
        return filePath.toString();
    }

    public String uploadAndExtractZipFile(MultipartFile file) throws IOException {
        // First upload the file
        String uploadedFilePath = uploadZipFile(file);

        // Then extract it
        String extractionPath = extractZipFile(uploadedFilePath);

        return extractionPath;
    }

    public String extractZipFile(String zipFilePath) throws IOException {
        Path zipPath = Paths.get(zipFilePath);

        if (!Files.exists(zipPath)) {
            throw new IOException("ZIP file not found: " + zipFilePath);
        }

        // Create extraction directory (same name as zip file without extension)
        String fileName = zipPath.getFileName().toString();
        String folderName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path extractionDir = zipPath.getParent().resolve(folderName);

        if (!Files.exists(extractionDir)) {
            Files.createDirectories(extractionDir);
            logger.info("Created extraction directory: {}", extractionDir);
        }

        // Extract the ZIP file
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipPath.toFile()))) {
            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                Path newPath = extractionDir.resolve(zipEntry.getName());

                // Security check to prevent zip slip attacks
                if (!newPath.normalize().startsWith(extractionDir.normalize())) {
                    throw new IOException("Bad zip entry: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    // Create parent directories if they don't exist
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }

                    // Extract file
                    try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(newPath.toFile()))) {
                        byte[] buffer = new byte[8192]; // Optimized buffer size for large files
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }

                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        }

        logger.info("Successfully extracted ZIP file: {} to directory: {}", zipFilePath, extractionDir);
        return extractionDir.toString();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024 / 1024) + "GB");
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
