package com.footArena.booking.domain.exceptions;

public class UnauthorizedAccessException extends BaseException {

    public UnauthorizedAccessException(String message) {
        super(message, "UNAUTHORIZED_ACCESS");
    }

    public UnauthorizedAccessException() {
        super("Access denied", "UNAUTHORIZED_ACCESS");
    }
}