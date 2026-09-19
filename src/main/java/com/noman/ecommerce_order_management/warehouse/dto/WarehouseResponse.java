package com.noman.ecommerce_order_management.warehouse.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class WarehouseResponse {

    private Long id;

    private String code;

    private String name;

    private String city;

    private String address;

    private boolean active;
}