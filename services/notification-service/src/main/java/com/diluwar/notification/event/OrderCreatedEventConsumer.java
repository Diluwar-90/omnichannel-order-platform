package com.diluwar.notification.event;

import com.diluwar.notification.entity.Notification;
import com.diluwar.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "order.created",
            groupId = "notification-service",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {

        System.out.println(
                "Order created event received: orderId="
                        + event.orderId()
                        + ", customerId="
                        + event.customerId()
        );

        Notification notification = new Notification();
        notification.setCustomerId(event.customerId());
        notification.setOrderId(event.orderId());
        notification.setType("ORDER_CREATED");
        notification.setMessage(
                "Order created successfully for order " + event.orderId()
        );
        notification.setStatus("UNREAD");
        notification.setCreatedAt(Instant.now());

        notificationService.create(notification);
    }
}