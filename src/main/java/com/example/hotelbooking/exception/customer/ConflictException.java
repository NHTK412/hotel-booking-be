package com.example.hotelbooking.exception.customer;

// 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
