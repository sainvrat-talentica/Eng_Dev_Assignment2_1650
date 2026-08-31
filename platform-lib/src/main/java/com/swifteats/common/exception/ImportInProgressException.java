package com.swifteats.common.exception;

public class ImportInProgressException extends RuntimeException {

    public ImportInProgressException() {
        super("Sample data import is already in progress");
    }
}
