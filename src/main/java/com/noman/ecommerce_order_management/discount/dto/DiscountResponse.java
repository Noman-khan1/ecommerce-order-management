package com.noman.ecommerce_order_management.discount.dto;

import com.noman.ecommerce_order_management.discount.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DiscountResponse {

    private Long id;

    private String code;

    private DiscountType type;

    private BigDecimal value;

    private BigDecimal minimumOrderAmount;

    private BigDecimal maximumDiscountAmount;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private boolean active;
}