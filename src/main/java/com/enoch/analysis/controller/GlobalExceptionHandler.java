package com.enoch.analysis.controller;

import com.enoch.analysis.dto.ApiErrorResponse;
import com.enoch.analysis.service.FeatureDisabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleFeatureDisabled(FeatureDisabledException exception, HttpServletRequest request) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled error in analysis", exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse payload = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(payload);
    }
}
