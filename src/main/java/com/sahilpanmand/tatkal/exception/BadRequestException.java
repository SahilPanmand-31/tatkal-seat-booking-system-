package com.sahilpanmand.tatkal.exception;

// -> HTTP 400
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
