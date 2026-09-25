package com.diluwar.order.service;

import com.diluwar.order.dto.CreateOrderRequest;
import com.diluwar.order.dto.OrderItemRequest;
import com.diluwar.order.dto.OrderItemResponse;
import com.diluwar.order.dto.OrderResponse;
import com.diluwar.order.entity.Order;
import com.diluwar.order.entity.OrderItem;
import com.diluwar.order.entity.OrderStatus;
import com.diluwar.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Instant now = Instant.now();

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {

            OrderItem item = new OrderItem();

            item.setOrder(order);
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());

            order.getItems().add(item);

            BigDecimal lineTotal =
                    itemRequest.unitPrice()
                            .multiply(
                                    BigDecimal.valueOf(itemRequest.quantity())
                            );

            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse getOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(id)
                );

        return toResponse(order);
    }

    @Transactional
    public Page<OrderResponse> getOrders(Pageable pageable) {

        return orderRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateStatus(
            Long id,
            OrderStatus status
    ) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(id)
                );

        order.setStatus(status);
        order.setUpdatedAt(Instant.now());

        Order updatedOrder = orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(id)
                );

        orderRepository.delete(order);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}