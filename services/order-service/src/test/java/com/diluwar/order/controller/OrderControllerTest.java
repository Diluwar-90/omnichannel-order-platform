package com.diluwar.order.controller;

import com.diluwar.order.dto.CreateOrderRequest;
import com.diluwar.order.dto.OrderItemRequest;
import com.diluwar.order.dto.OrderItemResponse;
import com.diluwar.order.dto.OrderResponse;
import com.diluwar.order.entity.OrderStatus;
import com.diluwar.order.service.OrderNotFoundException;
import com.diluwar.order.service.OrderService;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse sampleOrderResponse() {

        Instant now = Instant.now();

        return new OrderResponse(
                1L,
                1001L,
                OrderStatus.CREATED,
                new BigDecimal("175.00"),
                now,
                now,
                List.of(
                        new OrderItemResponse(
                                1L,
                                101L,
                                2,
                                new BigDecimal("50.00")
                        ),
                        new OrderItemResponse(
                                2L,
                                102L,
                                3,
                                new BigDecimal("25.00")
                        )
                )
        );
    }

    @Test
    void shouldCreateOrder() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest(
                1001L,
                List.of(
                        new OrderItemRequest(
                                101L,
                                2,
                                new BigDecimal("50.00")
                        ),
                        new OrderItemRequest(
                                102L,
                                3,
                                new BigDecimal("25.00")
                        )
                )
        );

        OrderResponse response = sampleOrderResponse();

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(1001))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(175.00))
                .andExpect(jsonPath("$.items.length()").value(2));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void shouldGetOrderById() throws Exception {

        OrderResponse response = sampleOrderResponse();

        when(orderService.getOrder(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/orders/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(1001))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(175.00));

        verify(orderService).getOrder(1L);
    }

    @Test
    void shouldGetOrders() throws Exception {

        OrderResponse response = sampleOrderResponse();

        PageImpl<OrderResponse> page =
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 10),
                        1
                );

        when(orderService.getOrders(any()))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/orders")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(1001));

        verify(orderService).getOrders(any());
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {

        OrderResponse response = sampleOrderResponse();

        OrderResponse confirmedResponse =
                new OrderResponse(
                        response.id(),
                        response.customerId(),
                        OrderStatus.CONFIRMED,
                        response.totalAmount(),
                        response.createdAt(),
                        response.updatedAt(),
                        response.items()
                );

        when(orderService.updateStatus(1L, OrderStatus.CONFIRMED))
                .thenReturn(confirmedResponse);

        mockMvc.perform(
                        put("/api/orders/1/status")
                                .param("status", "CONFIRMED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderService)
                .updateStatus(1L, OrderStatus.CONFIRMED);
    }

    @Test
    void shouldDeleteOrder() throws Exception {

        doNothing()
                .when(orderService)
                .deleteOrder(1L);

        mockMvc.perform(
                        delete("/api/orders/1")
                )
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(1L);
    }

    @Test
    void shouldReturnBadRequestForInvalidCreateOrderRequest()
            throws Exception {

        CreateOrderRequest invalidRequest =
                new CreateOrderRequest(
                        null,
                        List.of()
                );

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                invalidRequest
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        verify(orderService,
                org.mockito.Mockito.never())
                .createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist()
            throws Exception {

        when(orderService.getOrder(999L))
                .thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(
                        get("/api/orders/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Order not found: 999"));

        verify(orderService).getOrder(999L);
    }
}