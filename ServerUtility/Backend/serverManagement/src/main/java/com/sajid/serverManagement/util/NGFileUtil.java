package com.sajid.serverManagement.util;

import com.sajid.serverManagement.dto.NGFile;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NGFileUtil {

    public static NGFileUtil getInstance() {
        return new NGFileUtil();
    }

    public List<NGFile> getFiles(String filePath) {
        List<NGFile> list = new ArrayList<>();
        File dataStore = new File(filePath);
        for (File file : dataStore.listFiles()) {
            String fileName = file.getName();
            String fileExtension = FilenameUtils.getExtension(fileName);
            fileName = FilenameUtils.getBaseName(fileName);
            int fileSize = (int)file.length() / 1024;
            fileSize = file.isDirectory() ? 0 : fileSize;


            list.add(new NGFile(fileName,
                    fileExtension,
                    new Timestamp(file.lastModified()),
                    fileSize,
                    file.isDirectory()));
        }
        return list;
    }
}
