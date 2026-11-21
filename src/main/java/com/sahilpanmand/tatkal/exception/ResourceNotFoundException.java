package com.sahilpanmand.tatkal.exception;

// -> HTTP 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
