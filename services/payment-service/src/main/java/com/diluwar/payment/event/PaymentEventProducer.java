package com.diluwar.payment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PAYMENT_CAPTURED_TOPIC = "payment.captured";

    private final KafkaTemplate<String, PaymentCapturedEvent> kafkaTemplate;

    public void publishPaymentCaptured(PaymentCapturedEvent event) {

        kafkaTemplate.send(
                PAYMENT_CAPTURED_TOPIC,
                event.orderId().toString(),
                event
        );
    }
}
