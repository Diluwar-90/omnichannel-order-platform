package com.diluwar.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diluwar.payment.dto.CreatePaymentRequest;
import com.diluwar.payment.dto.PaymentResponse;
import com.diluwar.payment.dto.UpdatePaymentStatusRequest;
import com.diluwar.payment.entity.Payment;
import com.diluwar.payment.entity.PaymentStatus;
import com.diluwar.payment.event.PaymentCapturedEvent;
import com.diluwar.payment.event.PaymentEventProducer;
import com.diluwar.payment.event.PaymentFailedEvent;
import com.diluwar.payment.event.PaymentRefundedEvent;
import com.diluwar.payment.exception.InvalidPaymentStatusException;
import com.diluwar.payment.exception.PaymentAlreadyExistsException;
import com.diluwar.payment.exception.PaymentNotFoundException;
import com.diluwar.payment.exception.PaymentProcessingException;
import com.diluwar.payment.mapper.PaymentMapper;
import com.diluwar.payment.provider.PaymentProvider;
import com.diluwar.payment.provider.PaymentProviderResult;
import com.diluwar.payment.repository.PaymentRepository;

/**
 * Enterprise Payment Orchestration Engine.
 * <p>
 * Core Architectural Capabilities:
 * - Distributed Idempotency Control: Enforces strict deduplication using Idempotency-Key headers
 *   and unique database constraints, guaranteeing zero double-charging during network retry loops.
 * - Circuit-Breaker Protected Provider Delegations: Integrates third-party banking/payment gateway
 *   adapters shielded by Resilience4j to prevent cascading provider outages from affecting order systems.
 * - Choreographed Saga Event Sourcing: Asynchronously broadcasts 'payment.captured', 'payment.failed',
 *   and 'payment.refunded' events to Apache Kafka KRaft cluster for eventual consistency.
 */
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PaymentEventProducer paymentEventProducer;

    @Autowired
    private PaymentProvider paymentProvider;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        return create(request, null);
    }

    /**
     * Idempotently processes payment creations. If an existing record matches the idempotency key,
     * returns the cached response; otherwise, invokes external payment provider and persists results.
     */
    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, String idempotencyKey) {

        // Step 1: Check idempotency
        Payment existingPayment = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        }
        if (existingPayment == null) {
            existingPayment = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);
        }

        // Branch 1: Existing payment found
        if (existingPayment != null) {
            if (existingPayment.getAmount().compareTo(request.getAmount()) != 0
                    || !existingPayment.getCurrency().equals(request.getCurrency())) {
                throw new PaymentAlreadyExistsException(request.getOrderId());
            }
            return paymentMapper.toResponse(existingPayment);
        }

        // Branch 2: New payment -> Call Payment Provider
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "order-" + request.getOrderId() + "-" + UUID.randomUUID();

        PaymentProviderResult providerResult = paymentProvider.processPayment(
                request.getOrderId(),
                request.getAmount(),
                request.getCurrency(),
                effectiveKey
        );

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setIdempotencyKey(effectiveKey);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());

        Instant now = Instant.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        if (providerResult.success()) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setTransactionId(providerResult.transactionId());
            Payment savedPayment = paymentRepository.save(payment);

            paymentEventProducer.publishPaymentCaptured(
                    new PaymentCapturedEvent(
                            savedPayment.getId(),
                            savedPayment.getOrderId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getTransactionId(),
                            savedPayment.getUpdatedAt()
                    )
            );

            return paymentMapper.toResponse(savedPayment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            Payment savedPayment = paymentRepository.save(payment);

            paymentEventProducer.publishPaymentFailed(
                    new PaymentFailedEvent(
                            savedPayment.getId(),
                            savedPayment.getOrderId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getUpdatedAt()
                    )
            );

            throw new PaymentProcessingException(providerResult.message());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {

        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for order: " + orderId
                        )
                );

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse updateStatus(
            Long paymentId,
            UpdatePaymentStatusRequest request) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        if (!isValidTransition(
                payment.getStatus(),
                request.getStatus())) {

            throw new InvalidPaymentStatusException(
                    payment.getStatus().name(),
                    request.getStatus().name()
            );
        }

        payment.setStatus(request.getStatus());

        if (request.getStatus() == PaymentStatus.CAPTURED
                && payment.getTransactionId() == null) {

            payment.setTransactionId(UUID.randomUUID().toString());
        }

        payment.setUpdatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        if (request.getStatus() == PaymentStatus.CAPTURED) {

            paymentEventProducer.publishPaymentCaptured(
                    new PaymentCapturedEvent(
                            savedPayment.getId(),
                            savedPayment.getOrderId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getTransactionId(),
                            savedPayment.getUpdatedAt()
                    )
            );
        }

        if (request.getStatus() == PaymentStatus.FAILED) {

            paymentEventProducer.publishPaymentFailed(
                    new PaymentFailedEvent(
                            savedPayment.getId(),
                            savedPayment.getOrderId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getUpdatedAt()
                    )
            );
        }

        if (request.getStatus() == PaymentStatus.REFUNDED) {

            paymentEventProducer.publishPaymentRefunded(
                    new PaymentRefundedEvent(
                            savedPayment.getId(),
                            savedPayment.getOrderId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getTransactionId(),
                            savedPayment.getUpdatedAt()
                    )
            );
        }

        return paymentMapper.toResponse(savedPayment);
    }

    private boolean isValidTransition(
            PaymentStatus current,
            PaymentStatus requested) {

        return switch (current) {

            case PENDING ->
                    requested == PaymentStatus.AUTHORIZED
                            || requested == PaymentStatus.FAILED;

            case AUTHORIZED ->
                    requested == PaymentStatus.CAPTURED
                            || requested == PaymentStatus.FAILED;

            case CAPTURED ->
                    requested == PaymentStatus.REFUNDED;

            case FAILED, REFUNDED ->
                    false;
        };
    }
}