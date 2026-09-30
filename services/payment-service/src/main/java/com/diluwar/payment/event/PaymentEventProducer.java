package com.diluwar.payment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PAYMENT_CAPTURED_TOPIC = "payment.captured";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String PAYMENT_REFUNDED_TOPIC = "payment.refunded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCaptured(PaymentCapturedEvent event) {

        kafkaTemplate.send(
                PAYMENT_CAPTURED_TOPIC,
                event.orderId().toString(),
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {

        kafkaTemplate.send(
                PAYMENT_FAILED_TOPIC,
                event.orderId().toString(),
                event
        );
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {

        kafkaTemplate.send(
                PAYMENT_REFUNDED_TOPIC,
                event.orderId().toString(),
                event
        );
    }
}