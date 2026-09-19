package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.PaymentMethod;
import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import com.noman.ecommerce_order_management.warehouse.WarehouseService;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrentCheckoutIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    /*
     * Scenario:
     *
     * inventory = 1
     *
     * Customer A wants 1
     * Customer B wants 1
     *
     * Both checkout at the same time.
     *
     * Expected:
     *
     * exactly one succeeds
     * exactly one gets HTTP 409
     * available = 0
     * reserved = 1
     * total successful orders = 1
     */
    @Test
    void shouldPreventOversellingWhenTwoCustomersCompeteForLastUnit()
            throws Exception {

        TestStock stock =
                createStock(
                        "LASTUNIT",
                        1
                );

        User customerOne =
                createCustomer(
                        "lastunit-one"
                );

        User customerTwo =
                createCustomer(
                        "lastunit-two"
                );

        cartService.addItem(
                customerOne.getEmail(),
                new AddCartItemRequest(
                        stock.skuId(),
                        1
                )
        );

        cartService.addItem(
                customerTwo.getEmail(),
                new AddCartItemRequest(
                        stock.skuId(),
                        1
                )
        );

        List<AttemptResult> results =
                runConcurrentCheckouts(
                        customerOne.getEmail(),
                        customerTwo.getEmail()
                );

        List<AttemptResult> successfulAttempts =
                results
                        .stream()
                        .filter(
                                AttemptResult::success
                        )
                        .toList();

        List<AttemptResult> failedAttempts =
                results
                        .stream()
                        .filter(
                                result ->
                                        !result.success()
                        )
                        .toList();

        assertEquals(
                1,
                successfulAttempts.size()
        );

        assertEquals(
                1,
                failedAttempts.size()
        );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                failedAttempts
                        .get(0)
                        .statusCode()
        );

        List<InventoryResponse> inventories =
                inventoryService
                        .getInventories(
                                stock.skuId(),
                                stock.warehouseId()
                        );

        assertEquals(
                1,
                inventories.size()
        );

        assertEquals(
                0,
                inventories.get(0)
                        .getAvailableQuantity()
        );

        assertEquals(
                1,
                inventories.get(0)
                        .getReservedQuantity()
        );

        int totalOrders =
                customerOrderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(
                                customerOne.getId()
                        )
                        .size()
                        +
                        customerOrderRepository
                                .findByCustomerIdOrderByCreatedAtDesc(
                                        customerTwo.getId()
                                )
                                .size();

        assertEquals(
                1,
                totalOrders
        );

        AttemptResult winner =
                successfulAttempts.get(0);

        AttemptResult loser =
                failedAttempts.get(0);

        assertEquals(
                0,
                cartService
                        .getCart(
                                winner.email()
                        )
                        .getTotalItems()
        );

        /*
         * Failed checkout rolls back.
         * The losing customer's cart must remain.
         */
        assertEquals(
                1,
                cartService
                        .getCart(
                                loser.email()
                        )
                        .getTotalItems()
        );
    }

    /*
     * Separate concurrency problem:
     *
     * same customer double-clicks checkout.
     *
     * We deliberately create inventory = 2 so stock is NOT
     * the reason the second checkout fails.
     *
     * Cart locking itself must ensure only one order exists.
     */
    @Test
    void shouldPreventDuplicateConcurrentCheckoutForSameCustomer()
            throws Exception {

        TestStock stock =
                createStock(
                        "DOUBLECLICK",
                        2
                );

        User customer =
                createCustomer(
                        "double-click"
                );

        cartService.addItem(
                customer.getEmail(),
                new AddCartItemRequest(
                        stock.skuId(),
                        1
                )
        );

        List<AttemptResult> results =
                runConcurrentCheckouts(
                        customer.getEmail(),
                        customer.getEmail()
                );

        List<AttemptResult> successfulAttempts =
                results
                        .stream()
                        .filter(
                                AttemptResult::success
                        )
                        .toList();

        List<AttemptResult> failedAttempts =
                results
                        .stream()
                        .filter(
                                result ->
                                        !result.success()
                        )
                        .toList();

        assertEquals(
                1,
                successfulAttempts.size()
        );

        assertEquals(
                1,
                failedAttempts.size()
        );

        /*
         * Second request gets the cart lock after
         * the first checkout commits.
         *
         * At that point the cart has already been cleared.
         */
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                failedAttempts
                        .get(0)
                        .statusCode()
        );

        List<InventoryResponse> inventories =
                inventoryService
                        .getInventories(
                                stock.skuId(),
                                stock.warehouseId()
                        );

        assertEquals(
                1,
                inventories.size()
        );

        /*
         * Only ONE purchase happened:
         *
         * initial available = 2
         * final available   = 1
         * reserved          = 1
         */
        assertEquals(
                1,
                inventories.get(0)
                        .getAvailableQuantity()
        );

        assertEquals(
                1,
                inventories.get(0)
                        .getReservedQuantity()
        );

        assertEquals(
                1,
                customerOrderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(
                                customer.getId()
                        )
                        .size()
        );

        assertEquals(
                0,
                cartService
                        .getCart(
                                customer.getEmail()
                        )
                        .getTotalItems()
        );
    }

    private List<AttemptResult> runConcurrentCheckouts(
            String firstCustomerEmail,
            String secondCustomerEmail
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {

            Future<AttemptResult> firstFuture =
                    executor.submit(() ->
                            attemptCheckout(
                                    firstCustomerEmail,
                                    ready,
                                    start
                            )
                    );

            Future<AttemptResult> secondFuture =
                    executor.submit(() ->
                            attemptCheckout(
                                    secondCustomerEmail,
                                    ready,
                                    start
                            )
                    );

            /*
             * Ensure both threads are ready before
             * allowing either checkout to begin.
             */
            assertTrue(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    ),
                    "Checkout threads did not become ready in time"
            );

            /*
             * Release both threads at approximately
             * the same moment.
             */
            start.countDown();

            AttemptResult first =
                    firstFuture.get(
                            20,
                            TimeUnit.SECONDS
                    );

            AttemptResult second =
                    secondFuture.get(
                            20,
                            TimeUnit.SECONDS
                    );

            return List.of(
                    first,
                    second
            );

        } finally {

            /*
             * Safe even if already zero.
             */
            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
                            || executor.isShutdown()
            );
        }
    }

    private AttemptResult attemptCheckout(
            String customerEmail,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();

        boolean started =
                start.await(
                        5,
                        TimeUnit.SECONDS
                );

        if (!started) {

            throw new IllegalStateException(
                    "Timed out waiting to start concurrent checkout"
            );
        }

        try {

            OrderResponse order =
                    checkoutService.checkout(
                            customerEmail,
                            new CheckoutRequest(
                                    null,
                                    PaymentMethod.UPI,
                                    "Concurrency Test Address, Bangalore"
                            )
                    );

            return new AttemptResult(
                    customerEmail,
                    true,
                    null,
                    order.getOrderId()
            );

        } catch (ResponseStatusException exception) {

            return new AttemptResult(
                    customerEmail,
                    false,
                    exception
                            .getStatusCode()
                            .value(),
                    null
            );
        }
    }

    private User createCustomer(
            String prefix
    ) {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toLowerCase();

        User user =
                User.builder()
                        .name(
                                "Concurrency Customer "
                                        + suffix
                        )
                        .email(
                                prefix
                                        + "-"
                                        + suffix
                                        + "@test.com"
                        )
                        /*
                         * Authentication is not being exercised
                         * in this service-level integration test.
                         */
                        .password(
                                "test-password"
                        )
                        .role(
                                Role.CUSTOMER
                        )
                        .build();

        return userRepository
                .saveAndFlush(user);
    }

    private TestStock createStock(
            String prefix,
            int availableQuantity
    ) {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        CategoryResponse category =
                catalogService
                        .createCategory(
                                new CategoryRequest(
                                        prefix
                                                + " Category "
                                                + suffix,
                                        "Concurrency test category"
                                )
                        );

        ProductResponse product =
                catalogService
                        .createProduct(
                                new ProductRequest(
                                        prefix
                                                + " Product "
                                                + suffix,
                                        "Concurrency test product",
                                        "ConcurrencyBrand",
                                        category.getId()
                                )
                        );

        SkuResponse sku =
                catalogService
                        .createSku(
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
                warehouseService
                        .createWarehouse(
                                new WarehouseRequest(
                                        prefix
                                                + "-WH-"
                                                + suffix,
                                        prefix
                                                + " Warehouse "
                                                + suffix,
                                        "Bangalore",
                                        "Concurrency Test Address"
                                )
                        );

        inventoryService
                .createInventory(
                        new InventoryRequest(
                                sku.getId(),
                                warehouse.getId(),
                                availableQuantity
                        )
                );

        return new TestStock(
                sku.getId(),
                warehouse.getId()
        );
    }

    private record TestStock(
            Long skuId,
            Long warehouseId
    ) {
    }

    private record AttemptResult(
            String email,
            boolean success,
            Integer statusCode,
            Long orderId
    ) {
    }
}