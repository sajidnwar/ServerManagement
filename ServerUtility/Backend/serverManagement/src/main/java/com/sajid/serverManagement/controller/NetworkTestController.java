package com.sajid.serverManagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NetworkTestController {

    @GetMapping("/test-commands")
    public Map<String, Object> testCommands() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Test raw netstat output for port 8080
            Process process = Runtime.getRuntime().exec("cmd /c netstat -ano | findstr :8080");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line.trim());
                }
                result.put("netstat_output", lines);
            }

            // Test PowerShell approach
            try {
                Process psProcess = Runtime.getRuntime().exec(new String[]{
                    "powershell", "-Command",
                    "Get-NetTCPConnection -LocalPort 8080 | Select-Object -ExpandProperty OwningProcess"
                });
                try (BufferedReader psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                    List<String> psLines = new ArrayList<>();
                    String psLine;
                    while ((psLine = psReader.readLine()) != null) {
                        psLines.add(psLine.trim());
                    }
                    result.put("powershell_output", psLines);
                }
            } catch (Exception psError) {
                result.put("powershell_error", psError.getMessage());
            }

            // Test alternative netstat command
            Process altProcess = Runtime.getRuntime().exec("cmd /c netstat -aon | findstr \"8080\"");
            try (BufferedReader altReader = new BufferedReader(new InputStreamReader(altProcess.getInputStream()))) {
                List<String> altLines = new ArrayList<>();
                String altLine;
                while ((altLine = altReader.readLine()) != null) {
                    altLines.add(altLine.trim());
                }
                result.put("netstat_alternative_output", altLines);
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }

        return result;
    }
}
