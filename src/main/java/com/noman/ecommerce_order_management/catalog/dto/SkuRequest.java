package com.noman.ecommerce_order_management.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuRequest {

    @NotBlank(message = "SKU code is required")
    @Size(max = 100, message = "SKU code cannot exceed 100 characters")
    private String skuCode;

    @Size(max = 150, message = "Variant name cannot exceed 150 characters")
    private String variantName;

    @NotNull(message = "Price is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Price must be greater than zero"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Price must contain at most 2 decimal places"
    )
    private BigDecimal price;
}