package com.newtonite.ops.exception;

public class AppException extends RuntimeException {
    private final String code;
    private final int status;

    public AppException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public AppException(String message, String code, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
