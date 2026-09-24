package com.diluwar.payment.service;

import com.diluwar.payment.dto.CreatePaymentRequest;
import com.diluwar.payment.dto.PaymentResponse;
import com.diluwar.payment.dto.UpdatePaymentStatusRequest;
import com.diluwar.payment.entity.Payment;
import com.diluwar.payment.entity.PaymentStatus;
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

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {

        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new PaymentAlreadyExistsException(request.getOrderId());
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

        return paymentMapper.toResponse(
            paymentRepository.save(payment)
        );
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