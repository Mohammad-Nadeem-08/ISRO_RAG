package com.spacedoc.exception;

public class GroqException extends SpaceDocException {
    public GroqException(String message) {
        super(message);
    }

    public GroqException(String message, Throwable cause) {
        super(message, cause);
    }
}
