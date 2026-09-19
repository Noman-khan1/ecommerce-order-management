package com.noman.ecommerce_order_management.catalog;

import com.noman.ecommerce_order_management.catalog.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogService catalogService;

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        catalogService.createCategory(request)
                );
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {

        return catalogService.updateCategory(
                categoryId,
                request
        );
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        catalogService.createProduct(request)
                );
    }

    @PutMapping("/products/{productId}")
    public ProductResponse updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request
    ) {

        return catalogService.updateProduct(
                productId,
                request
        );
    }

    @PatchMapping("/products/{productId}/status")
    public ProductResponse updateProductStatus(
            @PathVariable Long productId,
            @RequestParam boolean active
    ) {

        return catalogService.updateProductStatus(
                productId,
                active
        );
    }

    @PostMapping("/products/{productId}/skus")
    public ResponseEntity<SkuResponse> createSku(
            @PathVariable Long productId,
            @Valid @RequestBody SkuRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        catalogService.createSku(
                                productId,
                                request
                        )
                );
    }

    @PutMapping("/skus/{skuId}")
    public SkuResponse updateSku(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuRequest request
    ) {

        return catalogService.updateSku(
                skuId,
                request
        );
    }

    @PatchMapping("/skus/{skuId}/status")
    public SkuResponse updateSkuStatus(
            @PathVariable Long skuId,
            @RequestParam boolean active
    ) {

        return catalogService.updateSkuStatus(
                skuId,
                active
        );
    }
}