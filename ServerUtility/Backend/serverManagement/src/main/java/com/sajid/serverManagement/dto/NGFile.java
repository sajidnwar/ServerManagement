package com.sajid.serverManagement.dto;

import java.sql.Timestamp;

public record NGFile(String name,
                     String extension,
                     Timestamp lastModified,
                     int size,
                     boolean isDirectory) {
}
