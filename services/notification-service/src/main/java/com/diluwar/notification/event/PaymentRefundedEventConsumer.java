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
public class PaymentRefundedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment.refunded",
            groupId = "notification-service",
            containerFactory = "paymentRefundedKafkaListenerContainerFactory"
    )
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        log.info("Received payment.refunded for order {}", event.orderId());

        Notification notification = new Notification();
        notification.setCustomerId(0L);
        notification.setOrderId(event.orderId());
        notification.setType("PAYMENT_REFUNDED");
        notification.setMessage("Payment refunded successfully for order " + event.orderId());
        notification.setStatus("UNREAD");
        notification.setCreatedAt(Instant.now());

        notificationService.create(notification);
    }
}
