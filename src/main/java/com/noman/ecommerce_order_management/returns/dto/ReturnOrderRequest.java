package com.noman.ecommerce_order_management.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOrderRequest {

    @NotBlank(
            message = "Return reason is required"
    )
    @Size(
            max = 500,
            message = "Return reason cannot exceed 500 characters"
    )
    private String reason;
}