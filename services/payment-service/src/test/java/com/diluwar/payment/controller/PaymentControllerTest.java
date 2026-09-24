package com.diluwar.payment.controller;

import com.diluwar.payment.dto.PaymentResponse;
import com.diluwar.payment.entity.PaymentStatus;
import com.diluwar.payment.service.PaymentService;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() throws Exception {

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .amount(new BigDecimal("1499.99"))
                        .currency("INR")
                        .status(PaymentStatus.PENDING)
                        .build();

        when(paymentService.create(any()))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": 1001,
                                    "amount": 1499.99,
                                    "currency": "INR"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.amount").value(1499.99))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetPaymentByOrderId() throws Exception {

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .amount(new BigDecimal("1499.99"))
                        .currency("INR")
                        .status(PaymentStatus.CAPTURED)
                        .transactionId("transaction-123")
                        .build();

        when(paymentService.getByOrderId(1001L))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/payments/order/1001")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.amount").value(1499.99))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.transactionId")
                        .value("transaction-123"));
    }

    @Test
    void shouldUpdatePaymentStatus() throws Exception {

        PaymentResponse response =
                PaymentResponse.builder()
                        .id(1L)
                        .orderId(1001L)
                        .amount(new BigDecimal("1499.99"))
                        .currency("INR")
                        .status(PaymentStatus.AUTHORIZED)
                        .build();

        when(paymentService.updateStatus(
                eq(1L),
                any()
        )).thenReturn(response);

        mockMvc.perform(
                patch("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "AUTHORIZED"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.status")
                        .value("AUTHORIZED"));
    }

    @Test
    void shouldRejectCreatePaymentWhenAmountIsZero() throws Exception {

        mockMvc.perform(
                post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": 1003,
                                    "amount": 0,
                                    "currency": "INR"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreatePaymentWhenCurrencyIsTooShort()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderId": 1003,
                                    "amount": 100.00,
                                    "currency": "IN"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUpdateStatusWhenStatusIsMissing()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUpdateStatusWhenStatusIsInvalid()
            throws Exception {

        mockMvc.perform(
                patch("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "SUCCESS"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }
}