package com.sahilpanmand.tatkal.exception;

// -> HTTP 401 (wrong email or password)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
