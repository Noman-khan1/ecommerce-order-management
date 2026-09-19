package com.noman.ecommerce_order_management.returns;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import com.noman.ecommerce_order_management.fulfillment.FulfillmentService;
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
import com.noman.ecommerce_order_management.payment.PaymentStatus;
import com.noman.ecommerce_order_management.returns.dto.ReturnOrderRequest;
import com.noman.ecommerce_order_management.returns.dto.ReturnResponse;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ReturnServiceIntegrationTest {

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
    private ReturnService returnService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderReturnRepository
            orderReturnRepository;

    @Test
    void shouldReturnDeliveredOrderRefundPaymentAndRestockInventory() {

        OrderFixture fixture =
                createConfirmedOrder(
                        "RETURN",
                        5,
                        2
                );

        fulfillmentService.updateOrderStatus(
                fixture.orderId(),
                new UpdateOrderStatusRequest(
                        OrderStatus.PACKED
                )
        );

        fulfillmentService.updateOrderStatus(
                fixture.orderId(),
                new UpdateOrderStatusRequest(
                        OrderStatus.SHIPPED
                )
        );

        fulfillmentService.updateOrderStatus(
                fixture.orderId(),
                new UpdateOrderStatusRequest(
                        OrderStatus.DELIVERED
                )
        );

        /*
         * Two units physically left the warehouse.
         */
        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                3,
                0
        );

        ReturnResponse returnResponse =
                returnService.returnOrder(
                        CUSTOMER_EMAIL,
                        fixture.orderId(),
                        new ReturnOrderRequest(
                                "Product did not meet expectations"
                        )
                );

        assertNotNull(
                returnResponse.getReturnId()
        );

        assertEquals(
                fixture.orderId(),
                returnResponse.getOrderId()
        );

        assertEquals(
                OrderStatus.RETURNED,
                returnResponse.getOrderStatus()
        );

        assertEquals(
                ReturnStatus.COMPLETED,
                returnResponse.getReturnStatus()
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                returnResponse.getPaymentStatus()
        );

        assertNotNull(
                returnResponse.getRefundReference()
        );

        assertNotNull(
                returnResponse.getRefundedAt()
        );

        assertNotNull(
                returnResponse.getCompletedAt()
        );

        /*
         * Price = 1000 x 2 = 2000
         * Tax  = 18%      = 360
         * Total/refund    = 2360
         */
        assertMoney(
                "2360.00",
                returnResponse.getRefundAmount()
        );

        /*
         * Returned inventory is available again.
         */
        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                5,
                0
        );

        OrderResponse customerOrder =
                orderService.getOrder(
                        CUSTOMER_EMAIL,
                        fixture.orderId()
                );

        assertEquals(
                OrderStatus.RETURNED,
                customerOrder.getStatus()
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                customerOrder.getPayment()
                        .getStatus()
        );

        assertEquals(
                returnResponse.getRefundReference(),
                customerOrder
                        .getPayment()
                        .getRefundReference()
        );

        assertNotNull(
                customerOrder
                        .getPayment()
                        .getRefundedAt()
        );

        assertTrue(
                orderReturnRepository
                        .findByOrderId(
                                fixture.orderId()
                        )
                        .isPresent()
        );

        /*
         * A second return must not refund or
         * restock the order again.
         */
        ResponseStatusException duplicateReturn =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                returnService.returnOrder(
                                        CUSTOMER_EMAIL,
                                        fixture.orderId(),
                                        new ReturnOrderRequest(
                                                "Trying to return again"
                                        )
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                duplicateReturn
                        .getStatusCode()
                        .value()
        );

        /*
         * Inventory must still be 5, not 7.
         */
        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                5,
                0
        );
    }

    @Test
    void shouldRejectReturnBeforeOrderIsDelivered() {

        OrderFixture fixture =
                createConfirmedOrder(
                        "EARLYRETURN",
                        3,
                        1
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                returnService.returnOrder(
                                        CUSTOMER_EMAIL,
                                        fixture.orderId(),
                                        new ReturnOrderRequest(
                                                "Trying to return too early"
                                        )
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception
                        .getStatusCode()
                        .value()
        );

        /*
         * Checkout reserved one unit.
         * Failed return must not change anything.
         */
        assertInventory(
                fixture.skuId(),
                fixture.warehouseId(),
                2,
                1
        );

        OrderResponse order =
                orderService.getOrder(
                        CUSTOMER_EMAIL,
                        fixture.orderId()
                );

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                order.getPayment()
                        .getStatus()
        );

        assertTrue(
                orderReturnRepository
                        .findByOrderId(
                                fixture.orderId()
                        )
                        .isEmpty()
        );
    }

    private OrderFixture createConfirmedOrder(
            String prefix,
            int initialStock,
            int quantity
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
                                        + " Category "
                                        + suffix,
                                "Return test category"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                prefix
                                        + " Product "
                                        + suffix,
                                "Return test product",
                                "ReturnBrand",
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
                                        "1000.00"
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
                                        + " Warehouse "
                                        + suffix,
                                "Bangalore",
                                "Return Test Address"
                        )
                );

        inventoryService.createInventory(
                new InventoryRequest(
                        sku.getId(),
                        warehouse.getId(),
                        initialStock
                )
        );

        cartService.addItem(
                CUSTOMER_EMAIL,
                new AddCartItemRequest(
                        sku.getId(),
                        quantity
                )
        );

        OrderResponse order =
                checkoutService.checkout(
                        CUSTOMER_EMAIL,
                        new CheckoutRequest(
                                null,
                                PaymentMethod.UPI,
                                "123 Return Street, Bangalore"
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

    private record OrderFixture(
            Long orderId,
            Long skuId,
            Long warehouseId
    ) {
    }
}