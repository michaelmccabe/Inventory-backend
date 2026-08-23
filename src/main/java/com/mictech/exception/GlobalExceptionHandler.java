package com.mictech.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStockException(InsufficientStockException ex) {
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateItemNameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateItemNameException(DuplicateItemNameException ex) {
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGenericException(RuntimeException ex) {
        return new ResponseEntity<>(Map.of("message", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
