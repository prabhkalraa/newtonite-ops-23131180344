package com.newtonite.ops.exception;

public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(message, "CONFLICT", 409);
    }

    public ConflictException(String message, String code) {
        super(message, code, 409);
    }
}
