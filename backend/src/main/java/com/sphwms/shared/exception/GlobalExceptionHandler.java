package com.sphwms.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised error handler.
 * <ul>
 *   <li>Validation errors (@Valid)  → 400 with field-level messages</li>
 *   <li>BusinessException           → 400 with a message</li>
 *   <li>NotFoundException           → 404 with a message</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Bean-validation (@Valid / @NotBlank / @Positive …) → 400
    // -------------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        // Use the first field message as the top-level message
        String firstMessage = fieldErrors.values().stream().findFirst()
                .orElse("Required fields are missing.");

        Map<String, Object> body = new HashMap<>();
        body.put("message", firstMessage);
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // -------------------------------------------------------------------------
    // Business-rule violation → 400
    // -------------------------------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    // -------------------------------------------------------------------------
    // Resource not found → 404
    // -------------------------------------------------------------------------
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
