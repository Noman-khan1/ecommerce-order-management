package com.noman.ecommerce_order_management.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder>
    findByCustomerIdOrderByCreatedAtDesc(
            Long customerId
    );

    Optional<CustomerOrder>
    findByIdAndCustomerId(
            Long orderId,
            Long customerId
    );
}