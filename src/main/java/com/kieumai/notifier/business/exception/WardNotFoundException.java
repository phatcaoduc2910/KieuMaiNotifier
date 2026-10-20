package com.kieumai.notifier.business.exception;

public class WardNotFoundException extends RuntimeException {

    public WardNotFoundException(long wardId) {
        super("Ward " + wardId + " not found");
    }
}
