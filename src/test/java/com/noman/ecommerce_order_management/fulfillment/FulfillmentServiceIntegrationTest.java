package com.noman.ecommerce_order_management.fulfillment;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import com.noman.ecommerce_order_management.fulfillment.dto.UpdateOrderStatusRequest;
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.order.CheckoutService;
import com.noman.ecommerce_order_management.order.OrderService;
import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.PaymentMethod;
import com.noman.ecommerce_order_management.warehouse.WarehouseService;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class FulfillmentServiceIntegrationTest {

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
    private CheckoutService checkoutService;

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldMoveOrderThroughFulfillmentLifecycleAndConsumeReservedInventory() {

        OrderFixture fixture =
                createConfirmedOrder(
                        "LIFECYCLE"
                );

        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                3,
                2
        );

        OrderResponse packed =
                fulfillmentService.updateOrderStatus(
                        fixture.orderId(),
                        new UpdateOrderStatusRequest(
                                OrderStatus.PACKED
                        )
                );

        assertEquals(
                OrderStatus.PACKED,
                packed.getStatus()
        );

        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                3,
                2
        );

        OrderResponse shipped =
                fulfillmentService.updateOrderStatus(
                        fixture.orderId(),
                        new UpdateOrderStatusRequest(
                                OrderStatus.SHIPPED
                        )
                );

        assertEquals(
                OrderStatus.SHIPPED,
                shipped.getStatus()
        );

        /*
         * Reserved items have now physically
         * left the warehouse.
         */
        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                3,
                0
        );

        OrderResponse delivered =
                fulfillmentService.updateOrderStatus(
                        fixture.orderId(),
                        new UpdateOrderStatusRequest(
                                OrderStatus.DELIVERED
                        )
                );

        assertEquals(
                OrderStatus.DELIVERED,
                delivered.getStatus()
        );

        OrderResponse customerView =
                orderService.getOrder(
                        CUSTOMER_EMAIL,
                        fixture.orderId()
                );

        assertEquals(
                OrderStatus.DELIVERED,
                customerView.getStatus()
        );

        List<OrderResponse> fulfillmentQueue =
                fulfillmentService
                        .getOrdersForFulfillment();

        assertFalse(
                fulfillmentQueue
                        .stream()
                        .anyMatch(candidate ->
                                candidate.getOrderId()
                                        .equals(
                                                fixture.orderId()
                                        )
                        )
        );
    }

    @Test
    void shouldRejectSkippingFulfillmentStatuses() {

        OrderFixture fixture =
                createConfirmedOrder(
                        "INVALID"
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> fulfillmentService
                                .updateOrderStatus(
                                        fixture.orderId(),
                                        new UpdateOrderStatusRequest(
                                                OrderStatus.SHIPPED
                                        )
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode()
                        .value()
        );
    }

    private OrderFixture createConfirmedOrder(
            String prefix
    ) {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                prefix
                                        + " Fulfillment Category "
                                        + suffix,
                                "Products for fulfillment testing"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                prefix
                                        + " Fulfillment Product "
                                        + suffix,
                                "Fulfillment test product",
                                "TestBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                prefix
                                        + "-SKU-"
                                        + suffix,
                                "Default Variant",
                                new BigDecimal(
                                        "2000.00"
                                )
                        )
                );

        WarehouseResponse warehouse =
                warehouseService.createWarehouse(
                        new WarehouseRequest(
                                prefix
                                        + "-WH-"
                                        + suffix,
                                prefix
                                        + " Fulfillment Warehouse "
                                        + suffix,
                                "Bangalore",
                                "Whitefield, Bangalore"
                        )
                );

        inventoryService.createInventory(
                new InventoryRequest(
                        sku.getId(),
                        warehouse.getId(),
                        5
                )
        );

        cartService.addItem(
                CUSTOMER_EMAIL,
                new AddCartItemRequest(
                        sku.getId(),
                        2
                )
        );

        OrderResponse order =
                checkoutService.checkout(
                        CUSTOMER_EMAIL,
                        new CheckoutRequest(
                                null,
                                PaymentMethod.UPI,
                                "123 Fulfillment Street, Bangalore"
                        )
                );

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        return new OrderFixture(
                order.getOrderId(),
                sku.getId(),
                warehouse.getId()
        );
    }

    private void assertInventory(
            Long skuId,
            Long warehouseId,
            int expectedAvailable,
            int expectedReserved
    ) {

        List<InventoryResponse> inventories =
                inventoryService.getInventories(
                        skuId,
                        warehouseId
                );

        assertEquals(
                1,
                inventories.size()
        );

        assertEquals(
                expectedAvailable,
                inventories.get(0)
                        .getAvailableQuantity()
        );

        assertEquals(
                expectedReserved,
                inventories.get(0)
                        .getReservedQuantity()
        );
    }

    private record OrderFixture(
            Long orderId,
            Long skuId,
            Long warehouseId
    ) {
    }
}