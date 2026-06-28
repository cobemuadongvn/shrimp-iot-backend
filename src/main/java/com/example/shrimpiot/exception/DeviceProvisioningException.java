package com.example.shrimpiot.exception;

import org.springframework.http.HttpStatus;

public class DeviceProvisioningException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public DeviceProvisioningException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
