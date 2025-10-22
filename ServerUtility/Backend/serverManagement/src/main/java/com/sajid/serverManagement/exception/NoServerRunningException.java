package com.sajid.serverManagement.exception;

public class NoServerRunningException extends RuntimeException {

    public NoServerRunningException(String message) {
        super(message);
    }

    public NoServerRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}
