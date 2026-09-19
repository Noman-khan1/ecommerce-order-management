package com.noman.ecommerce_order_management.cart.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CartResponse {

    private Long cartId;

    private Long customerId;

    private String customerEmail;

    private List<CartItemResponse> items;

    private int totalItems;

    private BigDecimal subtotal;
}