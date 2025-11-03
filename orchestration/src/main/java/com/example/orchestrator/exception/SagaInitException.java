package com.example.orchestrator.exception;

public class SagaInitException extends RuntimeException {

    public SagaInitException(String message) {
        super(message);
    }

    public SagaInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public SagaInitException(Throwable cause) {
        super(cause);
    }
}
