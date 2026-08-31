package com.swifteats.analytics.exception;

import com.swifteats.common.exception.ErrorResponse;
import com.swifteats.common.exception.ImportInProgressException;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsExceptionHandler {

    @ExceptionHandler(AnalyticsDataNotLoadedException.class)
    public ResponseEntity<ErrorResponse> handleAnalyticsDataNotLoaded(
            AnalyticsDataNotLoadedException ex, HttpServletRequest request) {
        return jsonError(HttpStatus.PRECONDITION_FAILED, "ANALYTICS_DATA_NOT_LOADED", ex.getMessage(), request);
    }

    @ExceptionHandler(ImportInProgressException.class)
    public ResponseEntity<ErrorResponse> handleImportInProgress(
            ImportInProgressException ex, HttpServletRequest request) {
        return jsonError(HttpStatus.CONFLICT, "IMPORT_IN_PROGRESS", ex.getMessage(), request);
    }

    private static ResponseEntity<ErrorResponse> jsonError(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(code, message, request.getRequestURI()));
    }
}
