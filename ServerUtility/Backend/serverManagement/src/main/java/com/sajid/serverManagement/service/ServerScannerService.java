package com.sajid.serverManagement.service;

import com.sajid.serverManagement.dto.ServerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ServerScannerService {

    @Value("${server.base-path}")
    private String basePath;

    @Value("${server.prefix}")
    private String prefix;

    public List<ServerInfo> listAllServers() {
        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalStateException("Invalid base directory: " + basePath);
        }

        return Arrays.stream(Objects.requireNonNull(baseDir.listFiles()))
                .filter(File::isDirectory)
                .filter(dir -> dir.getName().startsWith(prefix))
                .map(dir -> new ServerInfo(dir.getName(), dir.getAbsolutePath(), false, 8080, null))
                .toList();
    }
}
