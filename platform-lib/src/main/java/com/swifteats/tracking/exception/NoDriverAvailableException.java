package com.swifteats.tracking.exception;

public class NoDriverAvailableException extends IllegalStateException {

    public NoDriverAvailableException() {
        super("No driver available for delivery");
    }
}
