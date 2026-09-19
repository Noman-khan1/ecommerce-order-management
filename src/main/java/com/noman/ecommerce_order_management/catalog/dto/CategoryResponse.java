package com.noman.ecommerce_order_management.catalog.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {

    private Long id;

    private String name;

    private String description;
}