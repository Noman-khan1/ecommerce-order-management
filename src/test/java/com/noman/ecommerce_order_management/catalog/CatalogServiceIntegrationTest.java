package com.noman.ecommerce_order_management.catalog;

import com.noman.ecommerce_order_management.catalog.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CatalogServiceIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Test
    void shouldCreateCategoryProductAndSku() {

        CategoryRequest categoryRequest =
                new CategoryRequest(
                        "Footwear",
                        "Shoes and footwear"
                );

        CategoryResponse category =
                catalogService.createCategory(
                        categoryRequest
                );

        assertNotNull(category.getId());
        assertEquals(
                "Footwear",
                category.getName()
        );

        ProductRequest productRequest =
                new ProductRequest(
                        "Nike Air Max",
                        "Running shoes",
                        "Nike",
                        category.getId()
                );

        ProductResponse product =
                catalogService.createProduct(
                        productRequest
                );

        assertNotNull(product.getId());
        assertEquals(
                "Nike Air Max",
                product.getName()
        );

        SkuRequest skuRequest =
                new SkuRequest(
                        "NIKE-AM-BLK-9",
                        "Black / Size 9",
                        new BigDecimal("4999.00")
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        skuRequest
                );

        assertNotNull(sku.getId());
        assertEquals(
                "NIKE-AM-BLK-9",
                sku.getSkuCode()
        );

        ProductDetailsResponse details =
                catalogService.getProductDetails(
                        product.getId()
                );

        assertEquals(
                1,
                details.getSkus().size()
        );

        assertEquals(
                "NIKE-AM-BLK-9",
                details.getSkus()
                        .get(0)
                        .getSkuCode()
        );
    }
}