package com.noman.ecommerce_order_management.catalog.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class SkuResponse {

    private Long id;

    private String skuCode;

    private String variantName;

    private BigDecimal price;

    private boolean active;
}