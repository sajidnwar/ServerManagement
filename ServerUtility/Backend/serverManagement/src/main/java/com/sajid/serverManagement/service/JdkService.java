package com.sajid.serverManagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JdkService {
    @Value("${jdk.base.path}")
    private String jdkBasePath;

    public List<String> getAllJdkVersions() {
        File baseDir = new File(jdkBasePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return List.of();
        }
        return Arrays.stream(baseDir.listFiles(File::isDirectory))
                .map(File::getName)
                .filter(name -> name.toLowerCase().startsWith("jdk"))
                .collect(Collectors.toList());
    }
}