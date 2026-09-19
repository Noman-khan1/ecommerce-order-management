package com.noman.ecommerce_order_management.pricing;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import com.noman.ecommerce_order_management.discount.DiscountService;
import com.noman.ecommerce_order_management.discount.DiscountType;
import com.noman.ecommerce_order_management.discount.dto.DiscountRequest;
import com.noman.ecommerce_order_management.pricing.dto.PricingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class PricingServiceIntegrationTest {

    private static final String CUSTOMER_EMAIL =
            "customer@ecommerce.com";

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CartService cartService;

    @Autowired
    private DiscountService discountService;

    @Autowired
    private PricingService pricingService;

    @Test
    void shouldApplyDiscountAndTaxToCustomerCart() {

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                "Pricing Test Category",
                                "Products used for pricing testing"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                "Pricing Test Product",
                                "Test product",
                                "TestBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                "PRICING-SKU-001",
                                "Default Variant",
                                new BigDecimal("2500.00")
                        )
                );

        cartService.addItem(
                CUSTOMER_EMAIL,
                new AddCartItemRequest(
                        sku.getId(),
                        2
                )
        );

        discountService.createDiscount(
                new DiscountRequest(
                        "SAVE10",
                        DiscountType.PERCENTAGE,
                        new BigDecimal("10.00"),
                        new BigDecimal("1000.00"),
                        null,
                        LocalDateTime.now()
                                .minusDays(1),
                        LocalDateTime.now()
                                .plusDays(1)
                )
        );

        PricingResponse pricing =
                pricingService.previewPricing(
                        CUSTOMER_EMAIL,
                        "SAVE10"
                );

        assertMoney(
                "5000.00",
                pricing.getSubtotal()
        );

        assertMoney(
                "500.00",
                pricing.getDiscountAmount()
        );

        assertMoney(
                "4500.00",
                pricing.getTaxableAmount()
        );

        assertMoney(
                "18.00",
                pricing.getTaxRate()
        );

        assertMoney(
                "810.00",
                pricing.getTaxAmount()
        );

        assertMoney(
                "5310.00",
                pricing.getTotalAmount()
        );

        assertEquals(
                "SAVE10",
                pricing.getDiscountCode()
        );
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}