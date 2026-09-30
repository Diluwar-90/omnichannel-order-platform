package com.diluwar.payment.service;

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
import com.diluwar.payment.mapper.PaymentMapper;
import com.diluwar.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {

        Payment existingPayment = paymentRepository
                .findByOrderId(request.getOrderId())
                .orElse(null);

        if (existingPayment != null) {

            if (existingPayment.getAmount().compareTo(request.getAmount()) != 0
                    || !existingPayment.getCurrency().equals(request.getCurrency())) {

                throw new PaymentAlreadyExistsException(request.getOrderId());
            }

            return paymentMapper.toResponse(existingPayment);
        }

        Payment payment = new Payment();

        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        Instant now = Instant.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(savedPayment);
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