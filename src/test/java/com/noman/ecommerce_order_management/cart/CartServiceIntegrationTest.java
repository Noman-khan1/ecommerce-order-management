package com.noman.ecommerce_order_management.cart;

import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.cart.dto.CartResponse;
import com.noman.ecommerce_order_management.cart.dto.UpdateCartItemRequest;
import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class CartServiceIntegrationTest {

    private static final String CUSTOMER_EMAIL =
            "customer@ecommerce.com";

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CartService cartService;

    @Test
    void shouldAddUpdateAndRemoveItemsFromCustomerCart() {

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                "Cart Test Footwear",
                                "Products used for cart testing"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                "Cart Test Shoe",
                                "Running shoe",
                                "TestBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                "CART-SHOE-BLK-9",
                                "Black / Size 9",
                                new BigDecimal("2500.00")
                        )
                );

        CartResponse firstAdd =
                cartService.addItem(
                        CUSTOMER_EMAIL,
                        new AddCartItemRequest(
                                sku.getId(),
                                2
                        )
                );

        assertNotNull(
                firstAdd.getCartId()
        );

        assertEquals(
                1,
                firstAdd.getItems().size()
        );

        assertEquals(
                2,
                firstAdd.getTotalItems()
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                firstAdd.getSubtotal()
                        )
        );

        CartResponse secondAdd =
                cartService.addItem(
                        CUSTOMER_EMAIL,
                        new AddCartItemRequest(
                                sku.getId(),
                                1
                        )
                );

        assertEquals(
                1,
                secondAdd.getItems().size()
        );

        assertEquals(
                3,
                secondAdd.getTotalItems()
        );

        assertEquals(
                0,
                new BigDecimal("7500.00")
                        .compareTo(
                                secondAdd.getSubtotal()
                        )
        );

        Long itemId =
                secondAdd.getItems()
                        .get(0)
                        .getItemId();

        CartResponse updated =
                cartService.updateItemQuantity(
                        CUSTOMER_EMAIL,
                        itemId,
                        new UpdateCartItemRequest(4)
                );

        assertEquals(
                4,
                updated.getTotalItems()
        );

        assertEquals(
                0,
                new BigDecimal("10000.00")
                        .compareTo(
                                updated.getSubtotal()
                        )
        );

        CartResponse removed =
                cartService.removeItem(
                        CUSTOMER_EMAIL,
                        itemId
                );

        assertEquals(
                0,
                removed.getItems().size()
        );

        assertEquals(
                0,
                removed.getTotalItems()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        removed.getSubtotal()
                )
        );
    }
}