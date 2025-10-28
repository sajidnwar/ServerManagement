package com.sajid.serverManagement.service;

import com.sajid.serverManagement.dto.ExtractionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.UUID;

@Service
public class AsyncExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExtractionService.class);

    // Store extraction status for each task
    private final ConcurrentHashMap<String, ExtractionStatus> extractionTasks = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("extractionTaskExecutor")
    private Executor taskExecutor;

    public CompletableFuture<String> extractZipFileAsync(String zipFilePath, String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            ExtractionStatus status = extractionTasks.get(taskId);
            if (status == null) {
                throw new RuntimeException("Task not found: " + taskId);
            }

            try {
                logger.info("Starting async extraction for task: {} - file: {}", taskId, zipFilePath);

                // Update status to in progress
                status.setStatus(ExtractionStatus.Status.IN_PROGRESS);
                status.setMessage("Extraction in progress");
                status.setProgressPercentage(0);

                // Perform the extraction with progress tracking
                String extractionPath = performExtractionWithProgress(zipFilePath, status);

                // Update status to completed
                status.setStatus(ExtractionStatus.Status.COMPLETED);
                status.setMessage("Extraction completed successfully");
                status.setExtractionPath(extractionPath);
                status.setProgressPercentage(100);
                status.setEndTime(System.currentTimeMillis());

                logger.info("Completed async extraction for task: {} - extracted to: {}", taskId, extractionPath);
                return extractionPath;

            } catch (Exception e) {
                logger.error("Failed async extraction for task: {} - error: {}", taskId, e.getMessage(), e);

                // Update status to failed
                status.setStatus(ExtractionStatus.Status.FAILED);
                status.setMessage("Extraction failed");
                status.setErrorMessage(e.getMessage());
                status.setEndTime(System.currentTimeMillis());

                throw new RuntimeException("Extraction failed: " + e.getMessage(), e);
            }
        }, taskExecutor);
    }

    private String performExtractionWithProgress(String zipFilePath, ExtractionStatus status) throws IOException {
        Path zipPath = Paths.get(zipFilePath);

        if (!Files.exists(zipPath)) {
            throw new IOException("ZIP file not found: " + zipFilePath);
        }

        // Create extraction directory
        String fileName = zipPath.getFileName().toString();
        String folderName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path extractionDir = zipPath.getParent().resolve(folderName);

        if (!Files.exists(extractionDir)) {
            Files.createDirectories(extractionDir);
            logger.info("Created extraction directory: {}", extractionDir);
        }

        // First pass: count total entries for progress calculation
        int totalEntries = countZipEntries(zipPath);
        int processedEntries = 0;

        // Second pass: extract files with progress updates
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();

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
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newPath.toFile()))) {
                        byte[] buffer = new byte[8192]; // Increased buffer size for better performance
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }

                // Update progress
                processedEntries++;
                int progressPercentage = (int) ((processedEntries * 100.0) / totalEntries);
                status.setProgressPercentage(progressPercentage);
                status.setMessage("Extracting: " + zipEntry.getName());

                // Log progress every 10%
                if (processedEntries % Math.max(1, totalEntries / 10) == 0) {
                    logger.info("Extraction progress for task {}: {}% ({}/{})",
                        status.getTaskId(), progressPercentage, processedEntries, totalEntries);
                }

                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        }

        return extractionDir.toString();
    }

    private int countZipEntries(Path zipPath) throws IOException {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            while (zis.getNextEntry() != null) {
                count++;
                zis.closeEntry();
            }
        }
        return count;
    }

    public ExtractionStatus getExtractionStatus(String taskId) {
        return extractionTasks.get(taskId);
    }

    public void cleanupCompletedTask(String taskId) {
        ExtractionStatus status = extractionTasks.get(taskId);
        if (status != null &&
            (status.getStatus() == ExtractionStatus.Status.COMPLETED ||
             status.getStatus() == ExtractionStatus.Status.FAILED)) {
            extractionTasks.remove(taskId);
            logger.info("Cleaned up extraction task: {}", taskId);
        }
    }

    public String startExtractionTask(String zipFilePath) {
        String taskId = UUID.randomUUID().toString();

        ExtractionStatus status = new ExtractionStatus(taskId, ExtractionStatus.Status.PENDING, "Extraction task queued");
        status.setZipFilePath(zipFilePath);
        extractionTasks.put(taskId, status);

        // Start async extraction using CompletableFuture.supplyAsync with custom executor
        extractZipFileAsync(zipFilePath, taskId)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    logger.error("Async extraction failed for task {}: {}", taskId, exception.getMessage());
                    ExtractionStatus failedStatus = extractionTasks.get(taskId);
                    if (failedStatus != null) {
                        failedStatus.setStatus(ExtractionStatus.Status.FAILED);
                        failedStatus.setMessage("Extraction failed");
                        failedStatus.setErrorMessage(exception.getMessage());
                        failedStatus.setEndTime(System.currentTimeMillis());
                    }
                }
            });

        logger.info("Extraction task {} queued and started asynchronously", taskId);
        return taskId;  // Return immediately without waiting for extraction
    }
}
