package com.Zone01.lets_play.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String id, String operation) {
        super("User with ID '" + id + "' not found for " + operation);
    }
}
