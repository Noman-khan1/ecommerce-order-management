package com.noman.ecommerce_order_management.catalog.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProductDetailsResponse {

    private Long id;

    private String name;

    private String description;

    private String brand;

    private Long categoryId;

    private String categoryName;

    private boolean active;

    private List<SkuResponse> skus;
}