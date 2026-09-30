package com.diluwar.payment.event;

import com.diluwar.payment.dto.CreatePaymentRequest;
import com.diluwar.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "order.created",
            groupId = "payment-service"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setOrderId(event.orderId());
            request.setAmount(event.totalAmount());
            request.setCurrency("INR");

            paymentService.create(request);
            log.info("Successfully created payment for order {}", event.orderId());
        } catch (Exception ex) {
            log.warn("Payment could not be created for order {}: {}", event.orderId(), ex.getMessage());
        }
    }
}