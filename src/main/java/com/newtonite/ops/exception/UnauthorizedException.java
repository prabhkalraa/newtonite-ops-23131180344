package com.newtonite.ops.exception;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }
}
