package com.noman.ecommerce_order_management.fulfillment;

import com.noman.ecommerce_order_management.fulfillment.dto.UpdateOrderStatusRequest;
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.order.CustomerOrderRepository;
import com.noman.ecommerce_order_management.order.OrderItem;
import com.noman.ecommerce_order_management.order.OrderItemRepository;
import com.noman.ecommerce_order_management.order.OrderService;
import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private static final List<OrderStatus> ACTIVE_FULFILLMENT_STATUSES =
            List.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.PACKED,
                    OrderStatus.SHIPPED
            );

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForFulfillment() {

        return customerOrderRepository
                .findByStatusInOrderByCreatedAtAsc(
                        ACTIVE_FULFILLMENT_STATUSES
                )
                .stream()
                .map(orderService::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            UpdateOrderStatusRequest request
    ) {

        if (request == null
                || request.getStatus() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order status is required"
            );
        }

        CustomerOrder order =
                customerOrderRepository
                        .findByIdForUpdate(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        OrderStatus nextStatus =
                request.getStatus();

        validateTransition(
                order.getStatus(),
                nextStatus
        );

        if (nextStatus == OrderStatus.SHIPPED) {

            consumeReservedInventory(order);
        }

        order.setStatus(nextStatus);

        customerOrderRepository
                .saveAndFlush(order);

        return orderService.toResponse(order);
    }

    private void consumeReservedInventory(
            CustomerOrder order
    ) {

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrderIdOrderByIdAsc(
                                order.getId()
                        )
                        .stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                (OrderItem item) ->
                                                        item.getSku()
                                                                .getId()
                                        )
                                        .thenComparing(
                                                item ->
                                                        item.getWarehouse()
                                                                .getId()
                                        )
                        )
                        .toList();

        if (orderItems.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has no items to fulfill"
            );
        }

        for (OrderItem item : orderItems) {

            inventoryService
                    .consumeReservedInventory(
                            item.getSku().getId(),
                            item.getWarehouse().getId(),
                            item.getQuantity()
                    );
        }
    }

    private void validateTransition(
            OrderStatus currentStatus,
            OrderStatus nextStatus
    ) {

        boolean validTransition =
                (currentStatus == OrderStatus.CONFIRMED
                        && nextStatus == OrderStatus.PACKED)
                        ||
                        (currentStatus == OrderStatus.PACKED
                                && nextStatus == OrderStatus.SHIPPED)
                        ||
                        (currentStatus == OrderStatus.SHIPPED
                                && nextStatus == OrderStatus.DELIVERED);

        if (!validTransition) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order status transition from "
                            + currentStatus
                            + " to "
                            + nextStatus
            );
        }
    }
}