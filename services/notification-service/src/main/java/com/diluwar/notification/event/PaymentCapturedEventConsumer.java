package com.diluwar.notification.event;

import com.diluwar.notification.entity.Notification;
import com.diluwar.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PaymentCapturedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment.captured",
            groupId = "notification-service"
    )
    public void handlePaymentCaptured(PaymentCapturedEvent event) {

        Notification notification = new Notification();

        notification.setCustomerId(0L);
        notification.setOrderId(event.orderId());
        notification.setType("PAYMENT_CAPTURED");
        notification.setMessage(
                "Payment captured successfully for order " + event.orderId()
        );
        notification.setStatus("UNREAD");
        notification.setCreatedAt(Instant.now());

        notificationService.create(notification);
    }
}