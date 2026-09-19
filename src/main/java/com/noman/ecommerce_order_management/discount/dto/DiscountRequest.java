package com.noman.ecommerce_order_management.discount.dto;

import com.noman.ecommerce_order_management.discount.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequest {

    @NotBlank(message = "Discount code is required")
    @Size(
            max = 50,
            message = "Discount code cannot exceed 50 characters"
    )
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(
            value = "0.01",
            message = "Discount value must be greater than zero"
    )
    @Digits(
            integer = 10,
            fraction = 2
    )
    private BigDecimal value;

    @NotNull(message = "Minimum order amount is required")
    @DecimalMin(
            value = "0.00",
            inclusive = true,
            message = "Minimum order amount cannot be negative"
    )
    @Digits(
            integer = 10,
            fraction = 2
    )
    private BigDecimal minimumOrderAmount;

    @DecimalMin(
            value = "0.01",
            message = "Maximum discount amount must be greater than zero"
    )
    @Digits(
            integer = 10,
            fraction = 2
    )
    private BigDecimal maximumDiscountAmount;

    @NotNull(message = "Valid from is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until is required")
    private LocalDateTime validUntil;
}