package com.neurogine.wallet.exception;

/**
 * Exception thrown for bad/invalid requests
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
