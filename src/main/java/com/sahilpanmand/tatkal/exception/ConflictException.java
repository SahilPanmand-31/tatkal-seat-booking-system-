package com.sahilpanmand.tatkal.exception;

// -> HTTP 409 (seat already booked, duplicate email, already cancelled)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
