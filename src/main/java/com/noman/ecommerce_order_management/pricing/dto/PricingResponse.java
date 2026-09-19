package com.noman.ecommerce_order_management.pricing.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class PricingResponse {

    private BigDecimal subtotal;

    private String discountCode;

    private BigDecimal discountAmount;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;
}