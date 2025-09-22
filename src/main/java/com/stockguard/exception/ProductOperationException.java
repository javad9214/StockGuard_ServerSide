package com.stockguard.exception;

public class ProductOperationException extends RuntimeException {
    public ProductOperationException(String message) {
        super(message);
    }

    public ProductOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
