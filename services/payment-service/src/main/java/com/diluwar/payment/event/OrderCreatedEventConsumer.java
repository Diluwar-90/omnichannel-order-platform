package com.diluwar.payment.event;

import com.diluwar.payment.dto.CreatePaymentRequest;
import com.diluwar.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "order.created",
            groupId = "payment-service"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(event.orderId());
        request.setAmount(event.totalAmount());
        request.setCurrency("INR");

        paymentService.create(request);
    }
}