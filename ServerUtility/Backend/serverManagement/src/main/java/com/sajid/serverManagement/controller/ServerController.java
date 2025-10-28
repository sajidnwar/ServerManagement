package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.ServerInfo;
import com.sajid.serverManagement.exception.NoServerRunningException;
import com.sajid.serverManagement.service.ServerControlService;
import com.sajid.serverManagement.service.ServerScannerService;
import com.sajid.serverManagement.service.ServerStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/servers")
public class ServerController {

    private final ServerScannerService scanner;
    private final ServerStatusService status;
    private final ServerControlService control;

    public ServerController(ServerScannerService scanner, ServerStatusService status, ServerControlService control) {
        this.scanner = scanner;
        this.status = status;
        this.control = control;
    }

    @GetMapping
    public List<ServerInfo> listServers() {
        // Get the PID of the process using port 8080 (if any)
        Long activePid = status.getProcessIdUsingPort(8080);

        // Check if no server is running on port 8080
//        if (activePid == null) {
//            throw new NoServerRunningException("No server is running on port 8080");
//        }

        String activeProcessPath = null;
        String activeServerName = null;

        if (activePid != null) {
            // First try to get working directory
            activeProcessPath = status.getProcessWorkingDirectory(activePid);

            // If working directory doesn't work, try command line
            if (activeProcessPath == null) {
                String commandLine = status.getProcessCommandLine(activePid);
                if (commandLine != null) {
                    // Extract path from command line - look for jar file path or working directory
                    if (commandLine.contains("-jar")) {
                        String[] parts = commandLine.split("\\s+");
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (parts[i].equals("-jar")) {
                                String jarPath = parts[i + 1];
                                // Get directory containing the jar file
                                int lastSeparator = Math.max(jarPath.lastIndexOf("/"), jarPath.lastIndexOf("\\"));
                                if (lastSeparator > 0) {
                                    activeProcessPath = jarPath.substring(0, lastSeparator);
                                }
                                break;
                            }
                        }
                    }

                    // Alternative: look for server directory names in command line
                    if (activeProcessPath == null) {
                        List<ServerInfo> allServers = scanner.listAllServers();
                        for (ServerInfo server : allServers) {
                            if (commandLine.toLowerCase().contains(server.name().toLowerCase()) ||
                                    commandLine.contains(server.path())) {
                                activeServerName = server.name();
                                activeProcessPath = server.path();
                                break;
                            }
                        }
                    }
                }
            }

            // If we have a path, try to match it with server names
            if (activeProcessPath != null && activeServerName == null) {
                List<ServerInfo> allServers = scanner.listAllServers();
                for (ServerInfo server : allServers) {
                    // Normalize paths for comparison
                    String normalizedServerPath = server.path().replace("\\", "/").toLowerCase();
                    String normalizedProcessPath = activeProcessPath.replace("\\", "/").toLowerCase();

                    // Check if paths match
                    if (normalizedProcessPath.equals(normalizedServerPath) ||
                            normalizedProcessPath.contains(normalizedServerPath) ||
                            normalizedServerPath.contains(normalizedProcessPath)) {
                        activeServerName = server.name();
                        break;
                    }
                }
            }
        }

        final String finalActiveServerName = activeServerName;
        final Long finalActivePid = activePid;

        return scanner.listAllServers().stream()
                .map(info -> {
                    // A server is running if its name matches the active server name
                    boolean isRunning = finalActiveServerName != null &&
                            info.name().equals(finalActiveServerName);

                    return new ServerInfo(
                            info.name(),
                            info.path(),
                            isRunning,
                            info.port(),
                            isRunning ? finalActivePid : null
                    );
                })
                .toList();
    }

    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> getRunningServer() {
        // Get the PID of the process using port 8080 (if any)
        Long activePid = status.getProcessIdUsingPort(8080);

        if (activePid == null) {
            throw new NoServerRunningException("No server is running on port 8080");
        }

        String activeProcessPath = null;
        String activeServerName = null;

        // First try to get working directory
        activeProcessPath = status.getProcessWorkingDirectory(activePid);

        // If working directory doesn't work, try command line
        if (activeProcessPath == null) {
            String commandLine = status.getProcessCommandLine(activePid);
            if (commandLine != null) {
                // Extract path from command line - look for jar file path or working directory
                if (commandLine.contains("-jar")) {
                    String[] parts = commandLine.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (parts[i].equals("-jar")) {
                            String jarPath = parts[i + 1];
                            // Get directory containing the jar file
                            int lastSeparator = Math.max(jarPath.lastIndexOf("/"), jarPath.lastIndexOf("\\"));
                            if (lastSeparator > 0) {
                                activeProcessPath = jarPath.substring(0, lastSeparator);
                            }
                            break;
                        }
                    }
                }

                // Alternative: look for server directory names in command line
                if (activeProcessPath == null) {
                    List<ServerInfo> allServers = scanner.listAllServers();
                    for (ServerInfo server : allServers) {
                        if (commandLine.toLowerCase().contains(server.name().toLowerCase()) ||
                                commandLine.contains(server.path())) {
                            activeServerName = server.name();
                            activeProcessPath = server.path();
                            break;
                        }
                    }
                }
            }
        }

        // If we have a path, try to match it with server names
        if (activeProcessPath != null && activeServerName == null) {
            List<ServerInfo> allServers = scanner.listAllServers();
            for (ServerInfo server : allServers) {
                // Normalize paths for comparison
                String normalizedServerPath = server.path().replace("\\", "/").toLowerCase();
                String normalizedProcessPath = activeProcessPath.replace("\\", "/").toLowerCase();

                // Check if paths match
                if (normalizedProcessPath.equals(normalizedServerPath) ||
                        normalizedProcessPath.contains(normalizedServerPath) ||
                        normalizedServerPath.contains(normalizedProcessPath)) {
                    activeServerName = server.name();
                    activeProcessPath = server.path();
                    break;
                }
            }
        }

        // Build detailed response with deployments folder path
        Map<String, Object> response = new LinkedHashMap<>();

        // Basic server information
        response.put("name", activeServerName != null ? activeServerName : "Unknown Server");
        response.put("path", activeProcessPath != null ? activeProcessPath : "Unknown Path");
        response.put("running", true);
        response.put("port", 8080);
        response.put("pid", activePid);

        // Add deployments folder path
        String deploymentsPath = null;
        if (activeProcessPath != null) {
            File activeDir = new File(activeProcessPath);

            // Look for jboss* folder in the activeProcessPath
            File[] subDirs = activeDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    // Check if directory name starts with "jboss"
                    if (subDir.getName().toLowerCase().startsWith("jboss")) {
                        // Check for standalone/deployments inside jboss* folder
                        File standaloneDir = new File(subDir, "standalone");
                        if (standaloneDir.exists() && standaloneDir.isDirectory()) {
                            File deploymentsDir = new File(standaloneDir, "deployments");
                            if (deploymentsDir.exists() && deploymentsDir.isDirectory()) {
                                deploymentsPath = deploymentsDir.getAbsolutePath();
                                break;
                            }
                        }
                    }
                }
            }

            // If still not found, fallback to original logic for other server types
            if (deploymentsPath == null) {
                File serverDir = new File(activeProcessPath);

                // Common deployment folder locations for other application servers
                String[] deploymentFolders = {
                        "standalone/deployments",  // Direct standalone/deployments
                        "deployments",             // Direct deployments folder
                        "webapps",                 // Tomcat
                        "autodeploy",              // GlassFish
                        "deploy"                   // Generic deploy folder
                };

                for (String deployFolder : deploymentFolders) {
                    File deployDir = new File(serverDir, deployFolder);
                    if (deployDir.exists() && deployDir.isDirectory()) {
                        deploymentsPath = deployDir.getAbsolutePath();
                        break;
                    }
                }
            }
        }

        response.put("deployments_path", deploymentsPath);
        response.put("deployments_found", deploymentsPath != null);

        // Add additional process information
        response.put("command_line", status.getProcessCommandLine(activePid));
        response.put("working_directory", status.getProcessWorkingDirectory(activePid));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{port}/stop")
    public ResponseEntity<String> stopServer(@PathVariable int port,
                                           @RequestParam(defaultValue = "300") int timeout,
                                           @RequestParam(defaultValue = "false") boolean confirm) {
        try {
            // Validate port number
            if (port < 1 || port > 65535) {
                return ResponseEntity.badRequest().body("Invalid port number: " + port);
            }

            // Validate timeout
            if (timeout < 10 || timeout > 600) {
                return ResponseEntity.badRequest().body("Timeout must be between 10 and 600 seconds");
            }

            // Check if any server is running on this port
            Long pid = status.getProcessIdUsingPort(port);
            if (pid == null) {
                return ResponseEntity.ok("No server is running on port " + port);
            }

            // Get server details
            String serverName = "Unknown Server";
            String serverPath = "Unknown Path";

            // Try to identify the server
            String commandLine = status.getProcessCommandLine(pid);
            String workingDir = status.getProcessWorkingDirectory(pid);

            List<ServerInfo> allServers = scanner.listAllServers();
            for (ServerInfo server : allServers) {
                if ((commandLine != null && (commandLine.toLowerCase().contains(server.name().toLowerCase()) ||
                    commandLine.contains(server.path()))) ||
                    (workingDir != null && (workingDir.contains(server.name()) || workingDir.contains(server.path())))) {
                    serverName = server.name();
                    serverPath = server.path();
                    break;
                }
            }

            // Start shutdown process without end confirmation first
            boolean shutdownCompleted = control.stopServer(port, timeout, false);

            if (!shutdownCompleted) {
                return ResponseEntity.status(500).body("Failed to shutdown server '" + serverName + "' on port " + port + " (PID: " + pid + ") within " + timeout + " seconds timeout");
            }

            // Server has shutdown, now ask for final confirmation (like Ctrl+C prompt)
            if (!confirm) {
                return ResponseEntity.status(202) // 202 Accepted - waiting for final confirmation
                    .body("Server '" + serverName + "' (PID: " + pid + ") has completed shutdown process." +
                          "\nTerminate batch job (Y/N)? " +
                          "\nTo confirm termination, call: POST /servers/" + port + "/stop?confirm=true" +
                          "\nOr to cancel and potentially restart, call: GET /servers/" + port + "/stop/cancel");
            }

            // Final confirmation provided - complete the termination
            return ResponseEntity.ok("Server '" + serverName + "' on port " + port + " (PID: " + pid + ") terminated successfully");

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error stopping server on port " + port + ": " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/{port}/stop/cancel")
    public ResponseEntity<String> cancelStop(@PathVariable int port) {
        // This endpoint represents choosing "N" to the "Terminate batch job (Y/N)?" prompt
        return ResponseEntity.ok("Termination cancelled for port " + port + ". Note: Server may have already shutdown gracefully and would need to be manually restarted if needed.");
    }

    @PostMapping("/{port}/stop/force")
    public ResponseEntity<String> forceStopServer(@PathVariable int port) {
        try {
            // Validate port number
            if (port < 1 || port > 65535) {
                return ResponseEntity.badRequest().body("Invalid port number: " + port);
            }

            // Check if any server is running on this port
            Long pid = status.getProcessIdUsingPort(port);
            if (pid == null) {
                return ResponseEntity.ok("No server is running on port " + port);
            }

            // Force stop immediately (10 second timeout)
            boolean stopped = control.stopServer(port, 10);

            if (stopped) {
                return ResponseEntity.ok("Server on port " + port + " (PID: " + pid + ") force stopped successfully");
            } else {
                return ResponseEntity.status(500).body("Failed to force stop server on port " + port + " (PID: " + pid + ")");
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error force stopping server on port " + port + ": " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/{port}/stop/info")
    public ResponseEntity<Map<String, Object>> getStopInfo(@PathVariable int port) {
        Map<String, Object> info = new LinkedHashMap<>();

        try {
            // Validate port number
            if (port < 1 || port > 65535) {
                info.put("error", "Invalid port number: " + port);
                return ResponseEntity.badRequest().body(info);
            }

            Long pid = status.getProcessIdUsingPort(port);
            if (pid == null) {
                info.put("message", "No server is running on port " + port);
                info.put("action_required", false);
                return ResponseEntity.ok(info);
            }

            // Get detailed server information
            String serverName = "Unknown Server";
            String serverPath = "Unknown Path";
            String commandLine = status.getProcessCommandLine(pid);
            String workingDir = status.getProcessWorkingDirectory(pid);

            List<ServerInfo> allServers = scanner.listAllServers();
            for (ServerInfo server : allServers) {
                if ((commandLine != null && (commandLine.toLowerCase().contains(server.name().toLowerCase()) ||
                    commandLine.contains(server.path()))) ||
                    (workingDir != null && (workingDir.contains(server.name()) || workingDir.contains(server.path())))) {
                    serverName = server.name();
                    serverPath = server.path();
                    break;
                }
            }

            info.put("server_name", serverName);
            info.put("server_path", serverPath);
            info.put("process_id", pid);
            info.put("port", port);
            info.put("command_line", commandLine);
            info.put("working_directory", workingDir);
            info.put("action_required", true);
            info.put("warning", "Stopping this server will terminate the running process and may cause data loss.");
            info.put("confirmation_endpoint", "/servers/" + port + "/stop?confirm=true");

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            info.put("error", "Error getting server info: " + e.getMessage());
            return ResponseEntity.status(500).body(info);
        }
    }

    @PostMapping("/{name}/start")
    public ResponseEntity<String> startServer(@PathVariable String name) {
        try {
            // Validate server name exists in our scanned servers
            List<ServerInfo> allServers = scanner.listAllServers();
            ServerInfo targetServer = allServers.stream()
                    .filter(server -> server.name().equals(name))
                    .findFirst()
                    .orElse(null);

            if (targetServer == null) {
                return ResponseEntity.badRequest().body("Server '" + name + "' not found. Available servers: " +
                    allServers.stream().map(ServerInfo::name).toList());
            }

            // Check if server is already running
            Long existingPid = status.getProcessIdUsingPort(8080); // Assuming all servers use 8080
            if (existingPid != null) {
                // Check if the running server is this one
                String commandLine = status.getProcessCommandLine(existingPid);
                if (commandLine != null && (commandLine.contains(name) || commandLine.contains(targetServer.path()))) {
                    return ResponseEntity.ok("Server '" + name + "' is already running (PID: " + existingPid + ")");
                } else {
                    return ResponseEntity.badRequest().body("Another server is already running on port 8080 (PID: " + existingPid + "). Stop it first.");
                }
            }

            // Start the server using its actual path
            boolean started = control.startServer(targetServer.path());

            if (started) {
                return ResponseEntity.ok("Server '" + name + "' started successfully from path: " + targetServer.path());
            } else {
                return ResponseEntity.status(500).body("Failed to start server '" + name + "'");
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error starting server '" + name + "': " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error starting server '" + name + "': " + e.getMessage());
        }
    }

    @GetMapping("/{name}/status")
    public ResponseEntity<Map<String, Object>> getServerStartupStatus(@PathVariable String name) {
        Map<String, Object> status = new LinkedHashMap<>();

        try {
            // Validate server name exists in our scanned servers
            List<ServerInfo> allServers = scanner.listAllServers();
            ServerInfo targetServer = allServers.stream()
                    .filter(server -> server.name().equals(name))
                    .findFirst()
                    .orElse(null);

            if (targetServer == null) {
                status.put("server_name", name);
                status.put("exists", false);
                status.put("message", "Server '" + name + "' not found");
                status.put("available_servers", allServers.stream().map(ServerInfo::name).toList());
                return ResponseEntity.badRequest().body(status);
            }

            status.put("server_name", name);
            status.put("server_path", targetServer.path());
            status.put("exists", true);

            // Get accurate startup status from deployment folder
            String startupStatus = control.getServerStartupStatus(targetServer.path());
            Long pid = this.status.getProcessIdUsingPort(8080);

            status.put("startup_status", startupStatus);
            status.put("process_id", pid);
            status.put("port", 8080);

            // Get detailed deployment information
            Map<String, Object> deploymentStatus = control.getDeploymentStatus(targetServer.path());
            status.put("deployment_details", deploymentStatus);

            // Set appropriate message based on status
            switch (startupStatus) {
                case "STARTING":
                    status.put("is_running", true);
                    status.put("is_started", false);
                    status.put("message", "Server '" + name + "' is starting up - deployments are still being processed");
                    break;
                case "RUNNING":
                    status.put("message", "Server '" + name + "' is fully started and running successfully");
                    status.put("is_running", true);
                    status.put("is_started", true);
                    break;
                case "STOPPED":
                    status.put("is_running", false);
                    status.put("message", "Server '" + name + "' is not running");
                    break;
                case "UNKNOWN":
                    status.put("is_running", pid != null);
                    status.put("message", "Server '" + name + "' process status unclear - cannot access deployment information");
                    break;
                default:
                    status.put("is_running", pid != null);
                    status.put("message", "Server '" + name + "' status: " + startupStatus);
            }

            // Add process details if running
            if (pid != null) {
                status.put("command_line", this.status.getProcessCommandLine(pid));
                status.put("working_directory", this.status.getProcessWorkingDirectory(pid));
            }

            // Check if startup script exists
            File startupScript = control.findStartupScriptWithPatterns(targetServer.path(), "standalone.bat");
            status.put("startup_script_found", startupScript != null);
            if (startupScript != null) {
                status.put("startup_script_path", startupScript.getAbsolutePath());
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            status.put("server_name", name);
            status.put("error", "Error checking server status: " + e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
}
