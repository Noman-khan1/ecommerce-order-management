package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.audit.AuditLog;
import com.noman.ecommerce_order_management.audit.AuditLogRepository;
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
import com.noman.ecommerce_order_management.fulfillment.FulfillmentTask;
import com.noman.ecommerce_order_management.fulfillment.FulfillmentTaskRepository;
import com.noman.ecommerce_order_management.fulfillment.FulfillmentTaskStatus;
import com.noman.ecommerce_order_management.fulfillment.dto.UpdateOrderStatusRequest;
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.notification.NotificationLog;
import com.noman.ecommerce_order_management.notification.NotificationLogRepository;
import com.noman.ecommerce_order_management.notification.NotificationStatus;
import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.PaymentMethod;
import com.noman.ecommerce_order_management.warehouse.WarehouseService;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AsyncOrderPipelineIntegrationTest {

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
    private FulfillmentTaskRepository
            fulfillmentTaskRepository;

    @Autowired
    private NotificationLogRepository
            notificationLogRepository;

    @Autowired
    private AuditLogRepository
            auditLogRepository;

    @Test
    void shouldProcessDownstreamOrderEventsAsynchronously()
            throws Exception {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                "Async Category " + suffix,
                                "Async pipeline test category"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                "Async Product " + suffix,
                                "Async pipeline product",
                                "AsyncBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                "ASYNC-SKU-" + suffix,
                                "Default Variant",
                                new BigDecimal(
                                        "1500.00"
                                )
                        )
                );

        WarehouseResponse warehouse =
                warehouseService.createWarehouse(
                        new WarehouseRequest(
                                "ASYNC-WH-" + suffix,
                                "Async Warehouse "
                                        + suffix,
                                "Bangalore",
                                "Async Test Address"
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
                        1
                )
        );

        OrderResponse order =
                checkoutService.checkout(
                        CUSTOMER_EMAIL,
                        new CheckoutRequest(
                                null,
                                PaymentMethod.UPI,
                                "123 Async Street, Bangalore"
                        )
                );

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        /*
         * The downstream jobs run on other threads,
         * so wait until they become visible.
         */
        awaitCondition(
                () ->
                        fulfillmentTaskRepository
                                .findByOrderIdOrderByIdAsc(
                                        order.getOrderId()
                                )
                                .size()
                                == 1,
                "Fulfillment task was not created"
        );

        awaitCondition(
                () ->
                        hasNotification(
                                order.getOrderId(),
                                OrderStatus.CONFIRMED
                        ),
                "Confirmation notification was not created"
        );

        awaitCondition(
                () ->
                        hasAuditEvent(
                                order.getOrderId(),
                                "ORDER_CONFIRMED"
                        ),
                "Confirmation audit log was not created"
        );

        List<FulfillmentTask> tasks =
                fulfillmentTaskRepository
                        .findByOrderIdOrderByIdAsc(
                                order.getOrderId()
                        );

        assertEquals(
                1,
                tasks.size()
        );

        assertEquals(
                warehouse.getId(),
                tasks.get(0)
                        .getWarehouse()
                        .getId()
        );

        assertEquals(
                FulfillmentTaskStatus.PENDING,
                tasks.get(0)
                        .getStatus()
        );

        /*
         * Now verify that later fulfillment
         * status changes also generate async
         * notification + audit events.
         */
        OrderResponse packed =
                fulfillmentService
                        .updateOrderStatus(
                                order.getOrderId(),
                                new UpdateOrderStatusRequest(
                                        OrderStatus.PACKED
                                )
                        );

        assertEquals(
                OrderStatus.PACKED,
                packed.getStatus()
        );

        awaitCondition(
                () ->
                        hasNotification(
                                order.getOrderId(),
                                OrderStatus.PACKED
                        ),
                "Packed notification was not created"
        );

        awaitCondition(
                () ->
                        hasAuditEvent(
                                order.getOrderId(),
                                "ORDER_PACKED"
                        ),
                "Packed audit log was not created"
        );

        /*
         * Fulfillment routing happens once at
         * CONFIRMED, not again for every status.
         */
        assertEquals(
                1,
                fulfillmentTaskRepository
                        .findByOrderIdOrderByIdAsc(
                                order.getOrderId()
                        )
                        .size()
        );
    }

    private boolean hasNotification(
            Long orderId,
            OrderStatus orderStatus
    ) {

        List<NotificationLog> notifications =
                notificationLogRepository
                        .findByOrderIdOrderByCreatedAtAsc(
                                orderId
                        );

        return notifications
                .stream()
                .anyMatch(notification ->
                        notification.getOrderStatus()
                                == orderStatus
                                &&
                                notification.getStatus()
                                        == NotificationStatus.SIMULATED_SENT
                );
    }

    private boolean hasAuditEvent(
            Long orderId,
            String eventType
    ) {

        List<AuditLog> auditLogs =
                auditLogRepository
                        .findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                                "ORDER",
                                orderId
                        );

        return auditLogs
                .stream()
                .anyMatch(auditLog ->
                        auditLog.getEventType()
                                .equals(eventType)
                );
    }

    private void awaitCondition(
            BooleanSupplier condition,
            String failureMessage
    ) throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS
                        .toNanos(10);

        while (System.nanoTime()
                < deadline) {

            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(50);
        }

        assertTrue(
                condition.getAsBoolean(),
                failureMessage
        );
    }
}