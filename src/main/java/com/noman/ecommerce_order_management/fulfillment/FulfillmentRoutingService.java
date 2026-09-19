package com.noman.ecommerce_order_management.fulfillment;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.order.CustomerOrderRepository;
import com.noman.ecommerce_order_management.order.OrderItem;
import com.noman.ecommerce_order_management.order.OrderItemRepository;
import com.noman.ecommerce_order_management.warehouse.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FulfillmentRoutingService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;

    @Transactional
    public void createFulfillmentTasks(Long orderId) {

        CustomerOrder order =
                customerOrderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found for fulfillment routing"
                                )
                        );

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrderIdOrderByIdAsc(orderId);

        if (orderItems.isEmpty()) {

            throw new IllegalStateException(
                    "Order has no items for fulfillment routing"
            );
        }

        /*
         * An order may contain items assigned
         * to multiple warehouses.
         *
         * Create exactly one fulfillment task
         * for each unique warehouse.
         */
        Map<Long, Warehouse> warehouses =
                new LinkedHashMap<>();

        for (OrderItem item : orderItems) {

            Warehouse warehouse =
                    item.getWarehouse();

            warehouses.putIfAbsent(
                    warehouse.getId(),
                    warehouse
            );
        }

        for (Warehouse warehouse : warehouses.values()) {

            boolean taskAlreadyExists =
                    fulfillmentTaskRepository
                            .existsByOrderIdAndWarehouseId(
                                    orderId,
                                    warehouse.getId()
                            );

            if (taskAlreadyExists) {
                continue;
            }

            FulfillmentTask task =
                    FulfillmentTask.builder()
                            .order(order)
                            .warehouse(warehouse)
                            .status(
                                    FulfillmentTaskStatus.PENDING
                            )
                            .build();

            fulfillmentTaskRepository.save(task);
        }
    }
}