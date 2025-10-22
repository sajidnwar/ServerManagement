package com.sajid.serverManagement.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

@Service
public class ServerStatusService {

    public boolean isPortInUse(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Long getProcessIdUsingPort(int port) {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getProcessIdUsingPortWindows(port);
        } else {
            return getProcessIdUsingPortUnix(port);
        }
    }

    private Long getProcessIdUsingPortWindows(int port) {
        try {
            // Try multiple approaches for Windows

            // Approach 1: netstat -ano with better parsing
            Process process = Runtime.getRuntime().exec("cmd /c netstat -ano | findstr :" + port);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(":" + port) && line.contains("LISTENING")) {
                        // Split by whitespace and get the last part (PID)
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 5) {
                            try {
                                return Long.parseLong(parts[parts.length - 1]);
                            } catch (NumberFormatException e) {
                                // Continue to next line
                            }
                        }
                    }
                }
            }

            // Approach 2: Use PowerShell as fallback
            Process psProcess = Runtime.getRuntime().exec(new String[]{
                "powershell", "-Command",
                "Get-NetTCPConnection -LocalPort " + port + " | Select-Object -ExpandProperty OwningProcess"
            });
            try (BufferedReader psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                String psLine = psReader.readLine();
                if (psLine != null && !psLine.trim().isEmpty()) {
                    try {
                        return Long.parseLong(psLine.trim());
                    } catch (NumberFormatException e) {
                        // Fall through to approach 3
                    }
                }
            }

            // Approach 3: Alternative netstat command
            Process altProcess = Runtime.getRuntime().exec("cmd /c netstat -aon | findstr \":" + port + " \"");
            try (BufferedReader altReader = new BufferedReader(new InputStreamReader(altProcess.getInputStream()))) {
                String altLine;
                while ((altLine = altReader.readLine()) != null) {
                    if (altLine.contains("LISTENING")) {
                        String[] altParts = altLine.trim().split("\\s+");
                        if (altParts.length >= 5) {
                            try {
                                return Long.parseLong(altParts[altParts.length - 1]);
                            } catch (NumberFormatException e) {
                                // Continue
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            // Log error but don't throw
            System.err.println("Error getting process ID for port " + port + ": " + e.getMessage());
        }
        return null;
    }

    private Long getProcessIdUsingPortUnix(int port) {
        try {
            Process process = Runtime.getRuntime().exec("lsof -t -i:" + port);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                return line != null ? Long.parseLong(line.trim()) : null;
            }
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get the working directory of a process by PID
     */
    public String getProcessWorkingDirectory(Long pid) {
        if (pid == null) return null;

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getProcessWorkingDirectoryWindows(pid);
        } else {
            return getProcessWorkingDirectoryUnix(pid);
        }
    }

    private String getProcessWorkingDirectoryWindows(Long pid) {
        try {
            Process process = Runtime.getRuntime().exec("wmic process where processid=" + pid + " get commandline /value");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("CommandLine=")) {
                        String commandLine = line.substring("CommandLine=".length());
                        // Extract working directory from Java command line
                        if (commandLine.contains("-jar")) {
                            String[] parts = commandLine.split("\\s+");
                            for (int i = 0; i < parts.length - 1; i++) {
                                if (parts[i].equals("-jar")) {
                                    String jarPath = parts[i + 1];
                                    return jarPath.substring(0, jarPath.lastIndexOf("\\"));
                                }
                            }
                        }
                        return commandLine;
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private String getProcessWorkingDirectoryUnix(Long pid) {
        try {
            Process process = Runtime.getRuntime().exec("pwdx " + pid);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains(": ")) {
                    return line.substring(line.indexOf(": ") + 2);
                }
            }
        } catch (IOException e) {
            // Fallback to /proc/pid/cwd
            try {
                Process process = Runtime.getRuntime().exec("readlink /proc/" + pid + "/cwd");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    return reader.readLine();
                }
            } catch (IOException e2) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get command line arguments of a process by PID
     */
    public String getProcessCommandLine(Long pid) {
        if (pid == null) return null;

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getProcessCommandLineWindows(pid);
        } else {
            return getProcessCommandLineUnix(pid);
        }
    }

    private String getProcessCommandLineWindows(Long pid) {
        try {
            Process process = Runtime.getRuntime().exec("wmic process where processid=" + pid + " get commandline /value");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("CommandLine=")) {
                        return line.substring("CommandLine=".length());
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private String getProcessCommandLineUnix(Long pid) {
        try {
            Process process = Runtime.getRuntime().exec("ps -p " + pid + " -o args --no-headers");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            return null;
        }
    }
}
