package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.service.JdkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jdks")
public class JdkController {
    private final JdkService jdkService;

    @Autowired
    public JdkController(JdkService jdkService) {
        this.jdkService = jdkService;
    }

    @GetMapping
    public List<String> getAllJdkVersions() {
        return jdkService.getAllJdkVersions();
    }
}


