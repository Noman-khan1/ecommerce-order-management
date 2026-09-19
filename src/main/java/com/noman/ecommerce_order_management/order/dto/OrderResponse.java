package com.noman.ecommerce_order_management.order.dto;

import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.payment.dto.PaymentResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private String orderNumber;

    private OrderStatus status;

    private BigDecimal subtotal;

    private String discountCode;

    private BigDecimal discountAmount;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String shippingAddress;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;

    private PaymentResponse payment;
}