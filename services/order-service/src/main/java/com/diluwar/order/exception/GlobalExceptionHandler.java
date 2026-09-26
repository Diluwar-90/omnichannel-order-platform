package com.diluwar.order.exception;

import com.diluwar.order.service.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.diluwar.order.exception.InventoryReservationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, Object> response =
                error(HttpStatus.BAD_REQUEST.value(), "Validation failed");

        Map<String, String> fields = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fields.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        response.put("fields", fields);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    private Map<String, Object> error(
            int status,
            String message) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", status);
        response.put("message", message);

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

@ExceptionHandler(InventoryReservationException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public Map<String, Object> handleInventoryReservation(
        InventoryReservationException ex) {

    return response(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
    );
}
}
