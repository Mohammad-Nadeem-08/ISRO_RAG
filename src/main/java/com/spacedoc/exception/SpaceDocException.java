package com.spacedoc.exception;

public class SpaceDocException extends RuntimeException {
    public SpaceDocException(String message) {
        super(message);
    }

    public SpaceDocException(String message, Throwable cause) {
        super(message, cause);
    }
}
