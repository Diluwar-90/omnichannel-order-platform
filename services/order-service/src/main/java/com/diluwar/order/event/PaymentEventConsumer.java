package com.diluwar.order.event;

import com.diluwar.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment.captured",
            groupId = "order-service"
    )
    public void handlePaymentCaptured(PaymentCapturedEvent event) {

        orderService.confirmOrder(event.orderId());
    }
}
