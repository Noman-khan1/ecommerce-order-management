package com.noman.ecommerce_order_management.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequest {

    @NotBlank(message = "Warehouse code is required")
    @Size(
            max = 50,
            message = "Warehouse code cannot exceed 50 characters"
    )
    private String code;

    @NotBlank(message = "Warehouse name is required")
    @Size(
            max = 150,
            message = "Warehouse name cannot exceed 150 characters"
    )
    private String name;

    @NotBlank(message = "Warehouse city is required")
    @Size(
            max = 100,
            message = "Warehouse city cannot exceed 100 characters"
    )
    private String city;

    @Size(
            max = 500,
            message = "Warehouse address cannot exceed 500 characters"
    )
    private String address;
}