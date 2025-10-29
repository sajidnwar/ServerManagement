package com.sajid.serverManagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestController
public class TestController {
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "UP");
        resp.put("timestamp", Instant.now().toString());

        return resp;
    }

    public static void main(String[] args) {

        String zipPath = "D:\\NewgenONE2024.2_Patch3_June11.zip";

        System.out.println("Processing file: " + zipPath);
        System.out.println("Base Name: " + getBaseName(zipPath));

        List<String> folders = processZipFile(zipPath);

        if (folders.isEmpty()) {
            System.out.println("No first-level folders found in the ZIP archive.");
        } else {
            System.out.println("Found " + folders.size() + " first-level folders:");
            folders.forEach(System.out::println);
        }

    }
    public static List<String> processZipFile(String zipFilePath) {
        List<String> resultFolders = new ArrayList<>();
        String baseName = getBaseName(zipFilePath);
        String targetFolderName = ""; // Folder name we successfully located

        try (ZipFile zipFile = new ZipFile(new File(zipFilePath))) {

            // --- CHECK 1: Base Name Folder ---
            // The entry name for a folder must include a trailing slash
            String baseNameEntry = baseName + "/";
            ZipEntry baseFolderEntry = zipFile.getEntry(baseNameEntry);

            if (baseFolderEntry != null && baseFolderEntry.isDirectory()) {
                System.out.println("✅ Found folder matching ZIP file name: " + baseName);
                targetFolderName = baseName;
                resultFolders = getFoldersInside(zipFile, targetFolderName);
            } else {
                System.out.println("❌ Folder matching ZIP file name not found. Checking for jboss-eap*...");

                // --- CHECK 2: jboss-eap* Folder ---
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if it's a root-level directory starting with "jboss-eap"
                    if (entry.isDirectory() &&
                            entryName.startsWith("jboss-eap") &&
                            !entryName.substring(0, entryName.length() - 1).contains("/")) {

                        // Extract the folder name without the trailing slash
                        targetFolderName = entryName.substring(0, entryName.length() - 1);
                        System.out.println("✅ Found jboss-eap folder: " + targetFolderName);
                        resultFolders = getFoldersInside(zipFile, targetFolderName);
                        break; // Stop after finding the first match
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error processing ZIP file: " + e.getMessage());
        }

        return resultFolders;
    }
    public static String getBaseName(String fullPath) {
        // Using Path for robustness as shown previously
        String fileNameWithExt = new File(fullPath).getName();
        int lastDotIndex = fileNameWithExt.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileNameWithExt.substring(0, lastDotIndex);
        }
        return fileNameWithExt;
    }

    /**
     * Gets a list of folders inside a specified parent folder within the ZIP.
     * The parent folder must be at the root level.
     * * @param zipFile The open ZipFile object.
     * @param parentFolderName The name of the root folder to search inside (e.g., "NewgenONE...").
     * @return List of folder names immediately inside the parent.
     */
    public static List<String> getFoldersInside(ZipFile zipFile, String parentFolderName) {
        List<String> innerFolders = new ArrayList<>();

        // Ensure parentFolderName ends with a forward slash for path matching
        String prefix = parentFolderName.endsWith("/") ? parentFolderName : parentFolderName + "/";
        int prefixLength = prefix.length();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // 1. Check if the entry is a directory and starts with the required prefix
            if (entry.isDirectory() && entryName.startsWith(prefix)) {

                // 2. Get the substring *after* the parent folder name
                String relativePath = entryName.substring(prefixLength);

                // 3. Check for subfolders (it must contain a path separator) and ensure it's first-level inside the parent
                // For example, if entry is "parent/A/B/", relativePath is "A/B/".
                // We only want "A".
                int nextSlash = relativePath.indexOf('/');

                if (nextSlash > 0) {
                    String firstLevelFolder = relativePath.substring(0, nextSlash);
                    if (!innerFolders.contains(firstLevelFolder)) {
                        innerFolders.add(firstLevelFolder);
                    }
                }
            }
        }
        return innerFolders;
    }

}
