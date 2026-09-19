package com.noman.ecommerce_order_management.payment.dto;

import com.noman.ecommerce_order_management.payment.PaymentMethod;
import com.noman.ecommerce_order_management.payment.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;

    private String paymentReference;

    private String refundReference;

    private PaymentMethod method;

    private PaymentStatus status;

    private BigDecimal amount;

    private LocalDateTime processedAt;

    private LocalDateTime refundedAt;
}