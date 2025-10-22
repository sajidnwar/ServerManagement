package com.sajid.serverManagement.dto;

public record ServerInfo(String name,
                         String path,
                         boolean running,
                         int port,
                         Long pid) {
}
