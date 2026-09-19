package com.noman.ecommerce_order_management.returns.dto;

import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.payment.PaymentStatus;
import com.noman.ecommerce_order_management.returns.ReturnStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ReturnResponse {

    private Long returnId;

    private Long orderId;

    private String orderNumber;

    private OrderStatus orderStatus;

    private ReturnStatus returnStatus;

    private String reason;

    private BigDecimal refundAmount;

    private String refundReference;

    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private LocalDateTime refundedAt;
}