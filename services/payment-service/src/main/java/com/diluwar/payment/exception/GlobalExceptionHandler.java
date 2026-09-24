package com.diluwar.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handlePaymentNotFound(
            PaymentNotFoundException ex) {

        return response(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaymentStatusException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleInvalidPaymentStatus(
        InvalidPaymentStatusException ex) {

        return response(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
        );
    }
    
    @ExceptionHandler(PaymentAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handlePaymentAlreadyExists(
        PaymentAlreadyExistsException ex) {

        return response(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
        );
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
                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

        response.put("errors", errors);

        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMessageNotReadable(
        HttpMessageNotReadableException ex) {

        return response(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request body"
        );
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