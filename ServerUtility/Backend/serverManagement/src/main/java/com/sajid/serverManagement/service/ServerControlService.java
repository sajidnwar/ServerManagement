package com.sajid.serverManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServerControlService {

    @Autowired
    private ServerStatusService statusService;

    public boolean startServer(String serverPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return startServerWindows(serverPath);
        } else {
            return startServerUnix(serverPath);
        }
    }

    private boolean startServerWindows(String serverPath) throws IOException {
        // Search for Windows batch file in both patterns
        File standaloneFile = findStartupScriptWithPatterns(serverPath, "standalone.bat");
        if (standaloneFile == null) {
            throw new IllegalStateException("Windows start script 'standalone.bat' not found in any jboss-eap subfolder of: " + serverPath);
        }

        // Get the bin directory containing the standalone.bat file
        File binDirectory = standaloneFile.getParentFile();

        // Use 'start' command to open a new visible CMD window
        String command = "start \"Server Console\" /D \"" + binDirectory.getAbsolutePath() + "\" \"" +
                        standaloneFile.getAbsolutePath() + "\" -b 0.0.0.0";

        Runtime.getRuntime().exec("cmd /c " + command);

        return true;
    }

    private boolean startServerUnix(String serverPath) throws IOException {
        // Search for Unix shell script in both patterns
        File standaloneFile = findStartupScriptWithPatterns(serverPath, "standalone.sh");
        if (standaloneFile == null) {
            throw new IllegalStateException("Unix start script 'standalone.sh' not found in any jboss-eap subfolder of: " + serverPath);
        }

        // Get the bin directory containing the standalone.sh file
        File binDirectory = standaloneFile.getParentFile();

        ProcessBuilder pb = new ProcessBuilder("bash", standaloneFile.getAbsolutePath(), "-b", "0.0.0.0");
        pb.directory(binDirectory);
        pb.redirectErrorStream(true);
        pb.start();

        return true;
    }

    /**
     * Search for startup script following JBoss EAP patterns
     * Pattern 1: {serverPath}/jboss-eap/bin/standalone.bat
     * Pattern 2: {serverPath}/{serverName}/jboss-eap/bin/standalone.bat
     */
    public File findStartupScriptWithPatterns(String serverPath, String scriptName) {
        File serverDir = new File(serverPath);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            return null;
        }

        // Pattern 1: Look directly in serverPath for jboss-eap folders
        File found = searchForJBossEapScript(serverDir, scriptName);
        if (found != null) {
            return found;
        }

        // Pattern 2: Look in subdirectories (server name folders) for jboss-eap folders
        File[] subDirs = serverDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                found = searchForJBossEapScript(subDir, scriptName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Search for jboss-eap-* folders and check for bin/standalone script
     */
    private File searchForJBossEapScript(File directory, String scriptName) {
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        // Look for directories starting with "jboss-eap"
        for (File file : files) {
            if (file.isDirectory() && file.getName().startsWith("jboss-eap")) {
                File binDir = new File(file, "bin");
                if (binDir.exists() && binDir.isDirectory()) {
                    File scriptFile = new File(binDir, scriptName);
                    if (scriptFile.exists() && scriptFile.isFile()) {
                        return scriptFile;
                    }
                }
            }
        }

        return null;
    }

    public boolean stopServer(int port) throws IOException {
        return stopServer(port, 300, true); // Default 5 minutes timeout with end confirmation
    }

    public boolean stopServer(int port, int timeoutSeconds) throws IOException {
        return stopServer(port, timeoutSeconds, true); // Default with end confirmation
    }

    public boolean stopServer(int port, int timeoutSeconds, boolean requireEndConfirmation) throws IOException {
        Long pid = statusService.getProcessIdUsingPort(port);
        if (pid == null) {
            return false; // No process found on this port
        }

        try {
            // Cross-platform process termination
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                return stopServerWindows(pid, port, timeoutSeconds, requireEndConfirmation);
            } else {
                return stopServerUnix(pid, port, timeoutSeconds, requireEndConfirmation);
            }
        } catch (Exception e) {
            throw new IOException("Failed to stop server on port " + port + ": " + e.getMessage(), e);
        }
    }

    private boolean stopServerWindows(Long pid, int port, int timeoutSeconds, boolean requireEndConfirmation) throws IOException, InterruptedException {
        // Try graceful shutdown using Ctrl+C equivalent (SIGINT) first
        boolean gracefulShutdown = attemptGracefulShutdownWindows(pid, port, timeoutSeconds);

        if (gracefulShutdown) {
            if (requireEndConfirmation) {
                return true; // Gracefully stopped and confirmed
            }
            return true;
        }

        // If graceful shutdown failed, fall back to taskkill
        System.out.println("Graceful shutdown timed out, attempting force termination...");
        Process forceStop = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
        int exitCode = forceStop.waitFor();

        // Verify the process is actually stopped
        Thread.sleep(2000);
        boolean forceStopped = statusService.getProcessIdUsingPort(port) == null;

        if (forceStopped && requireEndConfirmation) {
            // This represents the end confirmation state after force kill
            return true; // Force stopped and confirmed
        }

        return forceStopped;
    }

    /**
     * Attempt graceful shutdown on Windows using multiple methods
     */
    private boolean attemptGracefulShutdownWindows(Long pid, int port, int timeoutSeconds) throws IOException, InterruptedException {
        System.out.println("Attempting graceful shutdown for process " + pid + " on port " + port);

        // Method 1: Try JBoss management interface shutdown first (most graceful)
        boolean managementShutdown = attemptJBossManagementShutdown(port);
        if (managementShutdown) {
            System.out.println("JBoss management interface shutdown initiated");
            return waitForProcessShutdown(port, timeoutSeconds);
        }

        // Method 2: Try sending actual Ctrl+C keystrokes to console window
        System.out.println("Attempting to send Ctrl+C keystrokes to console window...");
        boolean keystrokeSent = sendCtrlCToConsoleWindow(pid);
        if (keystrokeSent) {
            boolean result = waitForProcessShutdown(port, Math.min(timeoutSeconds, 60));
            if (result) {
                System.out.println("Server shutdown successfully via console keystrokes");
                return true;
            }
        }

        try {
            // Method 3: Try using Windows SendSignal via PowerShell (sends CTRL_C_EVENT)
            System.out.println("Attempting Ctrl+C signal via PowerShell...");
            String powerShellCommand = "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; " +
                "public class Win32 { " +
                "[DllImport(\"kernel32.dll\", SetLastError=true)] " +
                "public static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId); }'; " +
                "[Win32]::GenerateConsoleCtrlEvent(0, " + pid + ")";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", powerShellCommand);
            Process ctrlCProcess = pb.start();
            int psResult = ctrlCProcess.waitFor();
            System.out.println("PowerShell Ctrl+C signal sent, result: " + psResult);

            // Wait for graceful shutdown with timeout
            boolean signalResult = waitForProcessShutdown(port, Math.min(timeoutSeconds / 2, 30));
            if (signalResult) return true;

        } catch (Exception e) {
            System.out.println("PowerShell Ctrl+C method failed: " + e.getMessage());
        }

        // Method 4: Try alternative Windows signal method
        try {
            System.out.println("Trying alternative Windows signal method...");

            // Send Ctrl+Break signal using Windows API
            ProcessBuilder breakSignal = new ProcessBuilder("powershell", "-Command",
                "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; " +
                "public class Win32 { " +
                "[DllImport(\"kernel32.dll\", SetLastError=true)] " +
                "public static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId); }'; " +
                "[Win32]::GenerateConsoleCtrlEvent(1, " + pid + ")"); // 1 = CTRL_BREAK_EVENT
            breakSignal.start().waitFor();

            boolean result = waitForProcessShutdown(port, Math.min(timeoutSeconds / 3, 20));
            if (result) return true;

        } catch (Exception e2) {
            System.out.println("Alternative signal method failed: " + e2.getMessage());
        }

        // Method 5: Fallback to taskkill without /F flag (graceful termination)
        System.out.println("Falling back to taskkill graceful termination...");
        Process gracefulStop = Runtime.getRuntime().exec("taskkill /PID " + pid);
        gracefulStop.waitFor();

        return waitForProcessShutdown(port, Math.min(timeoutSeconds / 4, 15));
    }

    /**
     * Attempt to shutdown JBoss using its management interface
     */
    private boolean attemptJBossManagementShutdown(int port) {
        try {
            // Try to connect to JBoss management interface and send shutdown command
            // JBoss EAP typically has management interface on port 9990
            int managementPort = 9990;

            // Method 1: Try HTTP management interface
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "curl", "-X", "POST",
                    "http://localhost:" + managementPort + "/management",
                    "-H", "Content-Type: application/json",
                    "-d", "{\"operation\":\"shutdown\"}");
                Process curlProcess = pb.start();
                int result = curlProcess.waitFor();
                if (result == 0) {
                    System.out.println("JBoss shutdown command sent via HTTP management interface");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("HTTP management interface method failed: " + e.getMessage());
            }

            // Method 2: Try CLI interface if available
            try {
                // Look for jboss-cli script
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "jboss-cli.bat",
                    "--connect", "--command=:shutdown");
                Process cliProcess = pb.start();
                int result = cliProcess.waitFor();
                if (result == 0) {
                    System.out.println("JBoss shutdown command sent via CLI");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("CLI management interface method failed: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("JBoss management shutdown failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Wait for process to shutdown gracefully within timeout
     */
    private boolean waitForProcessShutdown(int port, int timeoutSeconds) throws InterruptedException {
        int checkInterval = 2; // Check every 2 seconds
        int maxChecks = timeoutSeconds / checkInterval;

        for (int i = 0; i < maxChecks; i++) {
            Thread.sleep(checkInterval * 1000);
            if (statusService.getProcessIdUsingPort(port) == null) {
                System.out.println("Server shutdown gracefully after " + (i * checkInterval) + " seconds");
                return true; // Server has stopped gracefully
            }
        }

        System.out.println("Graceful shutdown timed out after " + timeoutSeconds + " seconds");
        return false; // Timeout reached
    }

    private boolean stopServerUnix(Long pid, int port, int timeoutSeconds, boolean requireEndConfirmation) throws IOException, InterruptedException {
        // Try graceful shutdown using SIGINT (equivalent to Ctrl+C) first
        boolean gracefulShutdown = attemptGracefulShutdownUnix(pid, port, timeoutSeconds);

        if (gracefulShutdown) {
            if (requireEndConfirmation) {
                return true; // Gracefully stopped and confirmed
            }
            return true;
        }

        // If graceful shutdown failed, fall back to SIGKILL
        System.out.println("Graceful shutdown timed out, attempting force termination...");
        Process forceStop = Runtime.getRuntime().exec("kill -9 " + pid);
        int exitCode = forceStop.waitFor();

        // Verify the process is actually stopped
        Thread.sleep(1000);
        boolean forceStopped = statusService.getProcessIdUsingPort(port) == null;

        if (forceStopped && requireEndConfirmation) {
            // This represents the end confirmation state after force kill
            return true; // Force stopped and confirmed
        }

        return forceStopped;
    }

    /**
     * Attempt graceful shutdown on Unix using SIGINT (Ctrl+C equivalent)
     */
    private boolean attemptGracefulShutdownUnix(Long pid, int port, int timeoutSeconds) throws IOException, InterruptedException {
        try {
            // Method 1: Send SIGINT (interrupt signal - equivalent to Ctrl+C)
            Process gracefulStop = Runtime.getRuntime().exec("kill -INT " + pid);
            gracefulStop.waitFor();

            // Wait for graceful shutdown with timeout
            return waitForProcessShutdown(port, timeoutSeconds);

        } catch (Exception e) {
            System.out.println("SIGINT method failed: " + e.getMessage());

            // Method 2: Fallback to SIGTERM (graceful termination)
            Process gracefulStop = Runtime.getRuntime().exec("kill " + pid);
            gracefulStop.waitFor();

            return waitForProcessShutdown(port, timeoutSeconds);
        }
    }

    /**
     * Check if server has fully started by examining deployment status
     * Returns: "STARTING" if deployments are still being processed, "RUNNING" if all deployed, "STOPPED" if no process
     */
    public String getServerStartupStatus(String serverPath) {
        // First check if server process is running
//        Long pid = statusService.getProcessIdUsingPort(8080);
//        if (pid == null) {
//            return "STOPPED";
//        }

        // Find the standalone directory (parallel to bin folder)
        File standaloneDir = findStandaloneDirectory(serverPath);
        if (standaloneDir == null) {
            return "UNKNOWN"; // Can't determine deployment status
        }

        File deploymentsDir = new File(standaloneDir, "deployments");


        if (!deploymentsDir.exists() || !deploymentsDir.isDirectory()) {
            System.out.println("DEBUG: Deployments directory does not exist - server is still starting");
            return "STARTING"; // Server process exists but deployments folder not ready yet
        }

        // Check for files with .isdeploying, .pending, .failed extensions
        File[] deploymentFiles = deploymentsDir.listFiles();
        System.out.println("DEBUG: Number of files in deployments directory: " + (deploymentFiles != null ? deploymentFiles.length : 0));

        if (deploymentFiles != null) {
            for (File file : deploymentFiles) {
                String fileName = file.getName().toLowerCase();
                System.out.println("DEBUG: Found deployment file: " + fileName);
                if (fileName.endsWith(".isdeploying") ||
                    fileName.endsWith(".pending") ||
                    fileName.endsWith(".deploying")) {
                    System.out.println("DEBUG: Found deploying file - server is still starting");
                    return "STARTING"; // Still deploying
                }
            }
        }

        System.out.println("DEBUG: All deployments complete - server is running");
        return "RUNNING"; // All deployments complete
    }

    /**
     * Find the standalone directory that contains deployments folder
     */
    private File findStandaloneDirectory(String serverPath) {

        File serverDir = new File(serverPath);
        if (!serverDir.exists()) {
            return null;
        }

        // Pattern 1: Look for standalone directory directly in server path
        File standaloneDir = findStandaloneInDirectory(serverDir);
        if (standaloneDir != null) {
            return standaloneDir;
        }

        // Pattern 2: Look in subdirectories for jboss-eap/standalone
        File[] subDirs = serverDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (subDir.getName().startsWith("jboss-eap")) {
                    standaloneDir = findStandaloneInDirectory(subDir);
                    if (standaloneDir != null) {
                        return standaloneDir;
                    }
                }
            }
        }

        // Pattern 3: Look in server name subdirectories for jboss-eap/standalone
        if (subDirs != null) {
            for (File subDir : subDirs) {
                File[] jbossSubDirs = subDir.listFiles(File::isDirectory);
                if (jbossSubDirs != null) {
                    for (File jbossDir : jbossSubDirs) {
                        if (jbossDir.getName().startsWith("jboss-eap")) {
                            standaloneDir = findStandaloneInDirectory(jbossDir);
                            if (standaloneDir != null) {
                                return standaloneDir;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find standalone directory within a given directory
     */
    private File findStandaloneInDirectory(File directory) {
        File standaloneDir = new File(directory, "standalone");
        if (standaloneDir.exists() && standaloneDir.isDirectory()) {
            return standaloneDir;
        }
        return null;
    }

    /**
     * Get detailed deployment status information
     */
    public Map<String, Object> getDeploymentStatus(String serverPath) {
        Map<String, Object> deploymentInfo = new LinkedHashMap<>();

        File standaloneDir = findStandaloneDirectory(serverPath);
        if (standaloneDir == null) {
            deploymentInfo.put("deployments_folder_found", false);
            deploymentInfo.put("status", "UNKNOWN");
            return deploymentInfo;
        }

        File deploymentsDir = new File(standaloneDir, "deployments");
        deploymentInfo.put("deployments_folder_found", deploymentsDir.exists());
        deploymentInfo.put("deployments_path", deploymentsDir.getAbsolutePath());

        if (!deploymentsDir.exists()) {
            deploymentInfo.put("status", "NO_DEPLOYMENTS");
            return deploymentInfo;
        }

        File[] deploymentFiles = deploymentsDir.listFiles();
        List<String> deployingFiles = new ArrayList<>();
        List<String> deployedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        if (deploymentFiles != null) {
            for (File file : deploymentFiles) {
                String fileName = file.getName();
                if (fileName.endsWith(".isdeploying") || fileName.endsWith(".pending") || fileName.endsWith(".deploying")) {
                    deployingFiles.add(fileName);
                } else if (fileName.endsWith(".deployed")) {
                    deployedFiles.add(fileName);
                } else if (fileName.endsWith(".failed")) {
                    failedFiles.add(fileName);
                }
            }
        }

        deploymentInfo.put("deploying_files", deployingFiles);
        deploymentInfo.put("deployed_files", deployedFiles);
        deploymentInfo.put("failed_files", failedFiles);
        deploymentInfo.put("is_still_deploying", !deployingFiles.isEmpty());

        if (!deployingFiles.isEmpty()) {
            deploymentInfo.put("status", "STARTING");
        } else if (!failedFiles.isEmpty()) {
            deploymentInfo.put("status", "FAILED");
        } else {
            deploymentInfo.put("status", "RUNNING");
        }

        return deploymentInfo;
    }

    /**
     * Attempt to send actual Ctrl+C keystrokes to the console window
     */
    private boolean sendCtrlCToConsoleWindow(Long pid) {
        try {
            // Method 1: Use Windows API to find and send Ctrl+C to the console window
            String findWindowScript =
                "Add-Type -TypeDefinition '" +
                "using System;" +
                "using System.Runtime.InteropServices;" +
                "using System.Diagnostics;" +
                "public class WindowsAPI {" +
                "    [DllImport(\"user32.dll\")] public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);" +
                "    [DllImport(\"user32.dll\")] public static extern bool SetForegroundWindow(IntPtr hWnd);" +
                "    [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, uint dwExtraInfo);" +
                "    [DllImport(\"kernel32.dll\")] public static extern uint GetConsoleProcessList(uint[] ProcessList, uint ProcessCount);" +
                "    [DllImport(\"kernel32.dll\")] public static extern IntPtr GetConsoleWindow();" +
                "    [DllImport(\"user32.dll\")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);" +
                "    public static bool SendCtrlC(uint targetPid) {" +
                "        IntPtr consoleWindow = GetConsoleWindow();" +
                "        if (consoleWindow != IntPtr.Zero) {" +
                "            uint windowPid;" +
                "            GetWindowThreadProcessId(consoleWindow, out windowPid);" +
                "            if (windowPid == targetPid) {" +
                "                SetForegroundWindow(consoleWindow);" +
                "                keybd_event(0x11, 0, 0, 0); // Ctrl down" +
                "                keybd_event(0x43, 0, 0, 0); // C down" +
                "                keybd_event(0x43, 0, 2, 0); // C up" +
                "                keybd_event(0x11, 0, 2, 0); // Ctrl up" +
                "                return true;" +
                "            }" +
                "        }" +
                "        return false;" +
                "    }" +
                "}';" +
                "[WindowsAPI]::SendCtrlC(" + pid + ")";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", findWindowScript);
            Process process = pb.start();
            int result = process.waitFor();

            if (result == 0) {
                System.out.println("Ctrl+C keystrokes sent to console window for PID " + pid);
                return true;
            }

        } catch (Exception e) {
            System.out.println("Failed to send Ctrl+C keystrokes: " + e.getMessage());
        }
        return false;
    }
}
