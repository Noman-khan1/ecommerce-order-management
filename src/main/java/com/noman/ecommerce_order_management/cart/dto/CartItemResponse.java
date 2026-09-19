package com.noman.ecommerce_order_management.cart.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CartItemResponse {

    private Long itemId;

    private Long skuId;

    private String skuCode;

    private Long productId;

    private String productName;

    private String variantName;

    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal lineTotal;
}