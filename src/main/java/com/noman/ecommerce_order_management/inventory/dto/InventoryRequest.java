package com.noman.ecommerce_order_management.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest {

    @NotNull(message = "SKU id is required")
    private Long skuId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotNull(message = "Available quantity is required")
    @Min(
            value = 0,
            message = "Available quantity cannot be negative"
    )
    private Integer availableQuantity;
}