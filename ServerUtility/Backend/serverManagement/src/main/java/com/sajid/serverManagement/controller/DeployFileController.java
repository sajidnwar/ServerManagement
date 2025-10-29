package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.DeployFileDTO;
import com.sajid.serverManagement.dto.NGFile;
import com.sajid.serverManagement.util.NGFileUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DeployFileController {


    @PostMapping({"/tmpFileUpload"})
    public ResponseEntity<?> upload(@RequestPart("tmpFile") MultipartFile multipartFile, @RequestPart("deployInfo")  DeployFileDTO deployFile) throws IllegalStateException, IOException, IOException {
        Path filepath = Paths.get(deployFile.deploymentPath(), new String[] { multipartFile.getOriginalFilename() });
        multipartFile.transferTo(filepath);
        return ResponseEntity.ok("File Uploaded Successfully");
    }

    @PostMapping({"/listFiles"})
    public ResponseEntity<?> listFile(@RequestParam Map<String, String> queryParams, @RequestBody DeployFileDTO deployFile) {
        String fileExtensionToSearch = queryParams.get("ext");
        String fileNameToSearch = queryParams.get("name");



        List<NGFile> fileList = NGFileUtil.getInstance().getFiles(deployFile.deploymentPath());
        if (null != fileExtensionToSearch && !fileExtensionToSearch.trim().isEmpty())
            fileList = (List<NGFile>)fileList.stream().filter(file -> file.extension().equalsIgnoreCase(fileExtensionToSearch)).collect(Collectors.toList());
        if (null != fileNameToSearch && !fileNameToSearch.trim().isEmpty())
            fileList = (List<NGFile>)fileList.stream().filter(file -> file.name().contains(fileNameToSearch)).collect(Collectors.toList());
        return ResponseEntity.ok(fileList);
    }
}
