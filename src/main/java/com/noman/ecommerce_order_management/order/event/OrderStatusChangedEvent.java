package com.noman.ecommerce_order_management.order.event;

import com.noman.ecommerce_order_management.order.OrderStatus;

public record OrderStatusChangedEvent(
        Long orderId,
        String orderNumber,
        String customerEmail,
        OrderStatus status
) {
}