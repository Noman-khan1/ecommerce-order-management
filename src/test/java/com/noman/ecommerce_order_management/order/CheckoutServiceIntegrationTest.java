package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.cart.dto.CartResponse;
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
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.PaymentMethod;
import com.noman.ecommerce_order_management.payment.PaymentStatus;
import com.noman.ecommerce_order_management.warehouse.WarehouseService;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class CheckoutServiceIntegrationTest {

    private static final String CUSTOMER_EMAIL =
            "customer@ecommerce.com";

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartService cartService;

    @Autowired
    private DiscountService discountService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldCheckoutCartAndCreateConfirmedOrder() {

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                "Checkout Test Electronics",
                                "Products for checkout testing"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                "Checkout Test Phone",
                                "Test smartphone",
                                "TestBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                "CHECKOUT-PHONE-001",
                                "Black / 128 GB",
                                new BigDecimal(
                                        "2000.00"
                                )
                        )
                );

        WarehouseResponse warehouse =
                warehouseService
                        .createWarehouse(
                                new WarehouseRequest(
                                        "CHECKOUT-BLR-01",
                                        "Checkout Bangalore Warehouse",
                                        "Bangalore",
                                        "Whitefield, Bangalore"
                                )
                        );

        InventoryResponse inventory =
                inventoryService
                        .createInventory(
                                new InventoryRequest(
                                        sku.getId(),
                                        warehouse.getId(),
                                        10
                                )
                        );

        assertEquals(
                10,
                inventory.getAvailableQuantity()
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
                        "CHECKOUT10",
                        DiscountType.PERCENTAGE,
                        new BigDecimal(
                                "10.00"
                        ),
                        new BigDecimal(
                                "100.00"
                        ),
                        null,
                        LocalDateTime.now()
                                .minusDays(1),
                        LocalDateTime.now()
                                .plusDays(1)
                )
        );

        CheckoutRequest checkoutRequest =
                new CheckoutRequest(
                        "CHECKOUT10",
                        PaymentMethod.CARD,
                        "123 Test Street, Bangalore"
                );

        OrderResponse order =
                checkoutService.checkout(
                        CUSTOMER_EMAIL,
                        checkoutRequest
                );

        assertNotNull(
                order.getOrderId()
        );

        assertNotNull(
                order.getOrderNumber()
        );

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        assertMoney(
                "4000.00",
                order.getSubtotal()
        );

        assertMoney(
                "400.00",
                order.getDiscountAmount()
        );

        assertMoney(
                "3600.00",
                order.getTaxableAmount()
        );

        assertMoney(
                "18.00",
                order.getTaxRate()
        );

        assertMoney(
                "648.00",
                order.getTaxAmount()
        );

        assertMoney(
                "4248.00",
                order.getTotalAmount()
        );

        assertEquals(
                1,
                order.getItems().size()
        );

        assertEquals(
                2,
                order.getItems()
                        .get(0)
                        .getQuantity()
        );

        assertEquals(
                "CHECKOUT-BLR-01",
                order.getItems()
                        .get(0)
                        .getWarehouseCode()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                order.getPayment()
                        .getStatus()
        );

        assertEquals(
                PaymentMethod.CARD,
                order.getPayment()
                        .getMethod()
        );

        assertMoney(
                "4248.00",
                order.getPayment()
                        .getAmount()
        );

        List<InventoryResponse>
                inventories =
                inventoryService
                        .getInventories(
                                sku.getId(),
                                warehouse.getId()
                        );

        assertEquals(
                1,
                inventories.size()
        );

        assertEquals(
                8,
                inventories.get(0)
                        .getAvailableQuantity()
        );

        assertEquals(
                2,
                inventories.get(0)
                        .getReservedQuantity()
        );

        CartResponse cart =
                cartService.getCart(
                        CUSTOMER_EMAIL
                );

        assertEquals(
                0,
                cart.getTotalItems()
        );

        assertEquals(
                0,
                cart.getItems().size()
        );

        OrderResponse persistedOrder =
                orderService.getOrder(
                        CUSTOMER_EMAIL,
                        order.getOrderId()
                );

        assertEquals(
                order.getOrderNumber(),
                persistedOrder.getOrderNumber()
        );

        assertEquals(
                OrderStatus.CONFIRMED,
                persistedOrder.getStatus()
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