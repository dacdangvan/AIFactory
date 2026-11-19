package com.vib.cms.common.exception;

import com.vib.cms.common.api.ApiResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(404)
                .body(ApiResponse.failure("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<?> conflict(DuplicateKeyException exception) {
        return ResponseEntity.status(409)
                .body(ApiResponse.failure("CONFLICT",
                        "A record with the same unique key already exists"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_REQUEST",
                        "Request body is malformed or contains an unsupported value"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("BAD_REQUEST", exception.getMessage()));
    }
}
