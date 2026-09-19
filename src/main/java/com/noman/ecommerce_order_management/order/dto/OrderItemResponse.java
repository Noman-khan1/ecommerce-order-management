package com.noman.ecommerce_order_management.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class OrderItemResponse {

    private Long orderItemId;

    private Long skuId;

    private String skuCode;

    private Long productId;

    private String productName;

    private String variantName;

    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal lineTotal;

    private Long warehouseId;

    private String warehouseCode;
}