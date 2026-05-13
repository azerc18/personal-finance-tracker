package com.example.finance.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== COMMON RESPONSE BUILDER =====
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            boolean success,
            String message,
            List<ErrorItem> errors
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);

        if (errors != null && !errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.status(status).body(response);
    }

    // ===== VALIDATION ERROR =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        List<ErrorItem> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErrorItem(err.getField(), err.getDefaultMessage()))
                .toList();

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                false,
                "Validation failed",
                errors
        );
    }

    // ===== BUSINESS CONFLICT =====
    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessConflictException(BusinessConflictException ex) {
        return buildResponse(
                ex.getStatus(),
                false,
                ex.getMessage(),
                null
        );
    }

    // ===== NOT FOUND =====
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                false,
                ex.getMessage(),
                null
        );
    }

    // ===== BAD CREDENTIALS =====
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredential(BadCredentialsException ex) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                false,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {

        List<ErrorItem> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> new ErrorItem(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .toList();

        return ResponseEntity.badRequest().body(Map.of(
                "message", "Validation failed",
                "errors", errors
        ));
    }

    // ===== USER NOT FOUND =====
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                false,
                ex.getMessage(),
                null
        );
    }

    // ===== ILLEGAL ARGUMENT =====
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();

        String readable = (msg != null && !msg.isBlank()) ? msg : "Invalid request format";

        return buildResponse(HttpStatus.BAD_REQUEST, false, readable, null);
    }

    // ===== JSON PARSE ERROR =====
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseError(HttpMessageNotReadableException ex) {
        String message = "Malformed JSON request";
        String cause = ex.getMostSpecificCause().getMessage().toLowerCase();

        if (cause.contains("localdate") || cause.contains("parse")) {
            message = "Invalid date format. Expected yyyy-MM-dd";
        } else if (cause.contains("enum")) {
            message = "Invalid value for specific field (e.g. Type mismatch in Enum)";
        } else if (cause.contains("'true' or 'false'") || cause.contains("boolean")) {
            message = "Must be true or false";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, false, message, null);
    }

    // ===== TYPE MISMATCH =====
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName();
        String requiredType = (ex.getRequiredType() != null) ? ex.getRequiredType().getSimpleName() : "correct type";

        String customMessage = String.format("Parameter '%s' should be of type %s", fieldName, requiredType);

        if ("date".equals(fieldName)) {
            customMessage = "Date parameter must be in format yyyy-MM-dd";
        }

        ErrorItem error = new ErrorItem(fieldName, customMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, false, "Invalid parameter type", List.of(error));
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleEmailVerification(EmailVerificationException ex) {
            return buildResponse(
                    HttpStatus.FORBIDDEN,
                    false,
                    ex.getMessage(),
                    null
            );
    }
}

