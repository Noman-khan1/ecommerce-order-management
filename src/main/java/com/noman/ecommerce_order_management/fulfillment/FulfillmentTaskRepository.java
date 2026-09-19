package com.noman.ecommerce_order_management.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FulfillmentTaskRepository
        extends JpaRepository<FulfillmentTask, Long> {

    boolean existsByOrderIdAndWarehouseId(
            Long orderId,
            Long warehouseId
    );

    List<FulfillmentTask>
    findByOrderIdOrderByIdAsc(
            Long orderId
    );
}