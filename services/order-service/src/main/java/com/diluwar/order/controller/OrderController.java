package com.diluwar.order.controller;

import com.diluwar.order.dto.CreateOrderRequest;
import com.diluwar.order.dto.OrderResponse;
import com.diluwar.order.dto.PaymentResponse;
import com.diluwar.order.entity.OrderStatus;
import com.diluwar.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.getOrder(id)
        );
    }

    @GetMapping("/{id}/payment")
public ResponseEntity<PaymentResponse> getPayment(
        @PathVariable Long id) {

    return ResponseEntity.ok(
            orderService.getPaymentByOrderId(id)
    );
}

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            Pageable pageable) {

        return ResponseEntity.ok(
                orderService.getOrders(pageable)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                orderService.updateStatus(id, status)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id) {

        orderService.deleteOrder(id);

        return ResponseEntity.noContent().build();
    }
}
