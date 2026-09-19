package com.noman.ecommerce_order_management.catalog;

import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductDetailsResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/catalog")
@RequiredArgsConstructor
public class CustomerCatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {

        return catalogService.getCategories();
    }

    @GetMapping("/products")
    public List<ProductResponse> getProducts(
            @RequestParam(required = false)
            Long categoryId
    ) {

        return catalogService.getProducts(
                categoryId
        );
    }

    @GetMapping("/products/{productId}")
    public ProductDetailsResponse getProductDetails(
            @PathVariable Long productId
    ) {

        return catalogService.getProductDetails(
                productId
        );
    }
}