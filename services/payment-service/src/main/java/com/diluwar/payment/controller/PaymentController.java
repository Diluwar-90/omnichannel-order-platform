package com.diluwar.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diluwar.payment.dto.CreatePaymentRequest;
import com.diluwar.payment.dto.PaymentResponse;
import com.diluwar.payment.dto.UpdatePaymentStatusRequest;
import com.diluwar.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.create(request, idempotencyKey));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(
        @PathVariable Long orderId) {

        return ResponseEntity.ok(
            paymentService.getByOrderId(orderId)
        );
    }

    @PatchMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponse> updateStatus(
        @PathVariable Long paymentId,
        @Valid @RequestBody UpdatePaymentStatusRequest request) {

            return ResponseEntity.ok(
                paymentService.updateStatus(paymentId, request)
        );
    }
}