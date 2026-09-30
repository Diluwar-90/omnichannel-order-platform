package com.diluwar.notification.event;

import com.diluwar.notification.entity.Notification;
import com.diluwar.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment.failed",
            groupId = "notification-service",
            containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed for order {}", event.orderId());

        Notification notification = new Notification();
        notification.setCustomerId(0L);
        notification.setOrderId(event.orderId());
        notification.setType("PAYMENT_FAILED");
        notification.setMessage("Payment failed for order " + event.orderId());
        notification.setStatus("UNREAD");
        notification.setCreatedAt(Instant.now());

        notificationService.create(notification);
    }
}
