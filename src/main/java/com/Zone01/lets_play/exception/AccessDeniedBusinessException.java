package com.Zone01.lets_play.exception;

public class AccessDeniedBusinessException extends RuntimeException {
    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}
