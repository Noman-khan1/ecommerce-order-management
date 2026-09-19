package com.noman.ecommerce_order_management.inventory.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class InventoryResponse {

    private Long id;

    private Long skuId;

    private String skuCode;

    private Long warehouseId;

    private String warehouseCode;

    private int availableQuantity;

    private int reservedQuantity;

    private int totalQuantity;
}