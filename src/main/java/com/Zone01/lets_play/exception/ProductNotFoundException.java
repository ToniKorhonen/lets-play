package com.Zone01.lets_play.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String id, String operation) {
        super("Product with ID '" + id + "' not found for " + operation);
    }
}
