package com.noman.ecommerce_order_management.catalog.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {

    private Long id;

    private String name;

    private String description;

    private String brand;

    private Long categoryId;

    private String categoryName;

    private boolean active;
}