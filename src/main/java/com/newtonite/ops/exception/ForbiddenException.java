package com.newtonite.ops.exception;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }
}
