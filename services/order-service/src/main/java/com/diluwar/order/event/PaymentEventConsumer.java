package com.diluwar.order.event;

import com.diluwar.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment.captured",
            groupId = "order-service",
            containerFactory = "paymentCapturedKafkaListenerContainerFactory"
    )
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        log.info("Received payment.captured for order {}", event.orderId());
        orderService.confirmOrder(event.orderId());
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "order-service",
            containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed for order {}", event.orderId());
        orderService.handlePaymentFailure(event.orderId());
    }
}
