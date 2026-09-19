package com.noman.ecommerce_order_management.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReturnRepository
        extends JpaRepository<OrderReturn, Long> {

    Optional<OrderReturn> findByOrderId(
            Long orderId
    );

    boolean existsByOrderId(
            Long orderId
    );
}