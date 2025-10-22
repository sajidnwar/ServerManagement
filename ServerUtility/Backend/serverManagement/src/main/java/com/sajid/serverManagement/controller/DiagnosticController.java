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
@RequestMapping("/api")
public class DiagnosticController {

    private final ServerScannerService scanner;
    private final ServerStatusService status;

    public DiagnosticController(ServerScannerService scanner, ServerStatusService status) {
        this.scanner = scanner;
        this.status = status;
    }

    @GetMapping("/diagnose")
    public Map<String, Object> diagnose() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Check if port 8080 is in use
        boolean portInUse = status.isPortInUse(8080);
        result.put("step1_portInUse", portInUse);

        if (!portInUse) {
            result.put("conclusion", "No server is running on port 8080");
            return result;
        }

        // 2. Get PID of process using port 8080
        Long pid = status.getProcessIdUsingPort(8080);
        result.put("step2_processId", pid);

        if (pid == null) {
            result.put("conclusion", "Port is in use but couldn't find process ID");
            return result;
        }

        // 3. Get process working directory
        String workingDir = status.getProcessWorkingDirectory(pid);
        result.put("step3_workingDirectory", workingDir);

        // 4. Get process command line
        String commandLine = status.getProcessCommandLine(pid);
        result.put("step4_commandLine", commandLine);

        // 5. Get all server directories
        List<ServerInfo> servers = scanner.listAllServers();
        result.put("step5_foundServers", servers.stream()
                .map(s -> s.name() + " -> " + s.path())
                .toList());

        // 6. Try to match running process with servers
        String matchedServer = null;
        String matchReason = null;

        if (workingDir != null) {
            for (ServerInfo server : servers) {
                String normalizedWorkingDir = workingDir.replace("\\", "/").toLowerCase();
                String normalizedServerPath = server.path().replace("\\", "/").toLowerCase();

                if (normalizedWorkingDir.equals(normalizedServerPath) ||
                    normalizedWorkingDir.contains(normalizedServerPath) ||
                    normalizedServerPath.contains(normalizedWorkingDir)) {
                    matchedServer = server.name();
                    matchReason = "Working directory match: " + workingDir + " <-> " + server.path();
                    break;
                }
            }
        }

        if (matchedServer == null && commandLine != null) {
            for (ServerInfo server : servers) {
                if (commandLine.toLowerCase().contains(server.name().toLowerCase())) {
                    matchedServer = server.name();
                    matchReason = "Server name found in command line: " + server.name();
                    break;
                }
                if (commandLine.contains(server.path())) {
                    matchedServer = server.name();
                    matchReason = "Server path found in command line: " + server.path();
                    break;
                }
            }
        }

        result.put("step6_matchedServer", matchedServer);
        result.put("step6_matchReason", matchReason);

        return result;
    }
}
