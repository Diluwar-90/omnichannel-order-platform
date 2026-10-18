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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import com.diluwar.payment.event.PaymentEventProducer;

import com.diluwar.payment.exception.PaymentProcessingException;
import com.diluwar.payment.provider.PaymentProvider;
import com.diluwar.payment.provider.PaymentProviderResult;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PaymentProvider paymentProvider;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyMatches() {
        String idempotencyKey = "idemp-key-12345";
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(2001L);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("INR");

        Payment existingPayment = new Payment();
        existingPayment.setId(10L);
        existingPayment.setOrderId(2001L);
        existingPayment.setIdempotencyKey(idempotencyKey);
        existingPayment.setAmount(new BigDecimal("500.00"));
        existingPayment.setCurrency("INR");
        existingPayment.setStatus(PaymentStatus.CAPTURED);

        PaymentResponse response = PaymentResponse.builder()
                .id(10L)
                .orderId(2001L)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingPayment));
        when(paymentMapper.toResponse(existingPayment))
                .thenReturn(response);

        PaymentResponse result = paymentService.create(request, idempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.CAPTURED, result.getStatus());
        verify(paymentProvider, never()).processPayment(any(), any(), any(), any());
    }

    @Test
    void shouldProcessNewPaymentThroughProviderSuccessfully() {
        String idempotencyKey = "idemp-key-new-1";
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(2002L);
        request.setAmount(new BigDecimal("750.00"));
        request.setCurrency("INR");

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(2002L))
                .thenReturn(Optional.empty());

        when(paymentProvider.processPayment(eq(2002L), eq(new BigDecimal("750.00")), eq("INR"), eq(idempotencyKey)))
                .thenReturn(PaymentProviderResult.success("txn_success_123"));

        Payment savedPayment = new Payment();
        savedPayment.setId(11L);
        savedPayment.setOrderId(2002L);
        savedPayment.setStatus(PaymentStatus.CAPTURED);
        savedPayment.setTransactionId("txn_success_123");
        savedPayment.setUpdatedAt(Instant.now());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = PaymentResponse.builder()
                .id(11L)
                .orderId(2002L)
                .status(PaymentStatus.CAPTURED)
                .transactionId("txn_success_123")
                .build();

        when(paymentMapper.toResponse(savedPayment)).thenReturn(response);

        PaymentResponse result = paymentService.create(request, idempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.CAPTURED, result.getStatus());
        assertEquals("txn_success_123", result.getTransactionId());
        verify(paymentEventProducer).publishPaymentCaptured(any());
    }

    @Test
    void shouldFailPaymentWhenPaymentProviderFails() {
        String idempotencyKey = "idemp-key-fail-1";
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(2003L);
        request.setAmount(new BigDecimal("300.00"));
        request.setCurrency("INR");

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(2003L))
                .thenReturn(Optional.empty());

        when(paymentProvider.processPayment(eq(2003L), eq(new BigDecimal("300.00")), eq("INR"), eq(idempotencyKey)))
                .thenReturn(PaymentProviderResult.failure("Insufficient funds"));

        Payment savedPayment = new Payment();
        savedPayment.setId(12L);
        savedPayment.setOrderId(2003L);
        savedPayment.setStatus(PaymentStatus.FAILED);
        savedPayment.setUpdatedAt(Instant.now());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.create(request, idempotencyKey)
        );

        verify(paymentEventProducer).publishPaymentFailed(any());
    }

    @Test
    void shouldAuthorizePendingPayment() {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1001L);
        payment.setStatus(PaymentStatus.PENDING);

        UpdatePaymentStatusRequest request =
                UpdatePaymentStatusRequest.builder()
                        .status(PaymentStatus.AUTHORIZED)
                        .build();

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .status(PaymentStatus.AUTHORIZED)
                        .build();

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result =
                paymentService.updateStatus(1L, request);

        assertEquals(
                PaymentStatus.AUTHORIZED,
                result.getStatus()
        );

        assertEquals(
                PaymentStatus.AUTHORIZED,
                payment.getStatus()
        );
    }

    @Test
    void shouldRejectInvalidPaymentStatusTransition() {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1001L);
        payment.setStatus(PaymentStatus.CAPTURED);

        UpdatePaymentStatusRequest request =
                UpdatePaymentStatusRequest.builder()
                        .status(PaymentStatus.PENDING)
                        .build();

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        assertThrows(
                InvalidPaymentStatusException.class,
                () -> paymentService.updateStatus(1L, request)
        );
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {

        when(paymentRepository.findById(9999L))
                .thenReturn(Optional.empty());

        UpdatePaymentStatusRequest request =
                UpdatePaymentStatusRequest.builder()
                        .status(PaymentStatus.AUTHORIZED)
                        .build();

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.updateStatus(9999L, request)
        );
    }

    @Test
    void shouldRejectDuplicatePayment() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("1499.99"));
        request.setCurrency("INR");

        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setOrderId(1001L);
        existingPayment.setAmount(new BigDecimal("999.99"));
        existingPayment.setCurrency("INR");

        when(paymentRepository.findByOrderId(1001L))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                PaymentAlreadyExistsException.class,
                () -> paymentService.create(request)
        );
    }

    @Test
    void shouldGenerateTransactionIdWhenPaymentIsCaptured() {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1001L);
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setTransactionId(null);

        UpdatePaymentStatusRequest request =
                UpdatePaymentStatusRequest.builder()
                        .status(PaymentStatus.CAPTURED)
                        .build();

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .status(PaymentStatus.CAPTURED)
                        .transactionId("generated-transaction-id")
                        .build();

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result =
                paymentService.updateStatus(1L, request);

        assertEquals(
                PaymentStatus.CAPTURED,
                payment.getStatus()
        );

        assertNotNull(payment.getTransactionId());

        assertEquals(
                PaymentStatus.CAPTURED,
                result.getStatus()
        );

        assertNotNull(result.getTransactionId());
    }

    @Test
    void shouldPreserveTransactionIdWhenPaymentIsRefunded() {

        String existingTransactionId =
                "0b4b8071-4f5b-46f2-b976-1b528fed7513";

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1001L);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setTransactionId(existingTransactionId);

        UpdatePaymentStatusRequest request =
                UpdatePaymentStatusRequest.builder()
                        .status(PaymentStatus.REFUNDED)
                        .build();

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .status(PaymentStatus.REFUNDED)
                        .transactionId(existingTransactionId)
                        .build();

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result =
                paymentService.updateStatus(1L, request);

        assertEquals(
                PaymentStatus.REFUNDED,
                payment.getStatus()
        );

        assertEquals(
                existingTransactionId,
                payment.getTransactionId()
        );

        assertEquals(
                existingTransactionId,
                result.getTransactionId()
        );
    }

    @Test
    void shouldGetPaymentByOrderId() {

        Instant createdAt =
                Instant.parse("2026-09-24T03:28:16Z");

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1001L);
        payment.setAmount(new BigDecimal("1499.99"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setTransactionId(
                "0b4b8071-4f5b-46f2-b976-1b528fed7513"
        );
        payment.setCreatedAt(createdAt);
        payment.setUpdatedAt(createdAt);

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .amount(new BigDecimal("1499.99"))
                        .currency("INR")
                        .status(PaymentStatus.CAPTURED)
                        .transactionId(
                                "0b4b8071-4f5b-46f2-b976-1b528fed7513"
                        )
                        .createdAt(createdAt)
                        .updatedAt(createdAt)
                        .build();

        when(paymentRepository.findByOrderId(1001L))
                .thenReturn(Optional.of(payment));

        when(paymentMapper.toResponse(payment))
                .thenReturn(response);

        PaymentResponse result =
                paymentService.getByOrderId(1001L);

        assertNotNull(result);

        assertEquals(1L, result.getId());
        assertEquals(1001L, result.getOrderId());
        assertEquals(
                new BigDecimal("1499.99"),
                result.getAmount()
        );
        assertEquals("INR", result.getCurrency());
        assertEquals(
                PaymentStatus.CAPTURED,
                result.getStatus()
        );
        assertEquals(
                "0b4b8071-4f5b-46f2-b976-1b528fed7513",
                result.getTransactionId()
        );
    }
}