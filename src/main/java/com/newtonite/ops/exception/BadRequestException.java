package com.newtonite.ops.exception;

public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", 400);
    }

    public BadRequestException(String message, String code) {
        super(message, code, 400);
    }
}
