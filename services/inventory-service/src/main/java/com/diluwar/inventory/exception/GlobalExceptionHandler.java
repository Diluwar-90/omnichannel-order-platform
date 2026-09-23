package com.diluwar.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(
            InventoryNotFoundException ex) {

        return response(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(InventoryAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleAlreadyExists(
            InventoryAlreadyExistsException ex) {

        return response(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleInsufficientStock(
            InsufficientStockException ex) {

        return response(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, Object> response = response(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed"
        );

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        response.put("errors", errors);

        return response;
    }

    private Map<String, Object> response(
            int status,
            String message) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", status);
        response.put("message", message);

        return response;
    }
}
