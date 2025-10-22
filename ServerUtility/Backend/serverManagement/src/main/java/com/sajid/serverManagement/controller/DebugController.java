package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.ServerInfo;
import com.sajid.serverManagement.service.ServerScannerService;
import com.sajid.serverManagement.service.ServerStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final ServerScannerService scanner;
    private final ServerStatusService status;

    public DebugController(ServerScannerService scanner, ServerStatusService status) {
        this.scanner = scanner;
        this.status = status;
    }

    @GetMapping("/process-info")
    public Map<String, Object> getProcessInfo() {
        Map<String, Object> debug = new LinkedHashMap<>();

        // Check if port 8080 is in use
        boolean portInUse = status.isPortInUse(8080);
        debug.put("port8080InUse", portInUse);

        // Get PID using the port
        Long activePid = status.getProcessIdUsingPort(8080);
        debug.put("activePid", activePid);

        // Get process working directory
        String workingDir = status.getProcessWorkingDirectory(activePid);
        debug.put("processWorkingDirectory", workingDir);

        // Get command line
        String commandLine = status.getProcessCommandLine(activePid);
        debug.put("processCommandLine", commandLine);

        // Get all server directories found by scanner
        List<ServerInfo> allServers = scanner.listAllServers();
        debug.put("foundServerDirectories", allServers.stream()
                .map(info -> Map.of("name", info.name(), "path", info.path()))
                .toList());

        // Show OS info
        debug.put("operatingSystem", System.getProperty("os.name"));

        return debug;
    }
}
