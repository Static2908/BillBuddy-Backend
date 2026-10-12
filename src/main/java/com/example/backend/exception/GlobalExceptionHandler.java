package com.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, second) -> first
                ));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler(ApiExceptions.ConflictException.class)
    public ResponseEntity<?> conflict(ApiExceptions.ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(ApiExceptions.ResourceNotFoundException.class)
    public ResponseEntity<?> notFound(ApiExceptions.ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentials() {
        return response(HttpStatus.UNAUTHORIZED, "Invalid credentials", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<?> typeMismatch(MethodArgumentTypeMismatchException exception) {
    return response(
            HttpStatus.BAD_REQUEST,
            "Invalid value for parameter: " + exception.getName(),
            null
    );
}
    @ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<?> messageNotReadable(HttpMessageNotReadableException exception) {
    return response(
            HttpStatus.BAD_REQUEST,
            "Invalid request body",
            null
    );
}
@ExceptionHandler(NoResourceFoundException.class)
public ResponseEntity<?> resourceNotFound(NoResourceFoundException exception) {
    return response(HttpStatus.NOT_FOUND, "Resource not found", null);
}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected() {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message, Object details) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
