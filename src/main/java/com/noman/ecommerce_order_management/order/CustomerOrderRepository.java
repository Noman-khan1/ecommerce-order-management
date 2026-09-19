package com.noman.ecommerce_order_management.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    List<CustomerOrder>
    findByStatusInOrderByCreatedAtAsc(
            Collection<OrderStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from CustomerOrder o
            where o.id = :orderId
            """)
    Optional<CustomerOrder> findByIdForUpdate(
            @Param("orderId") Long orderId
    );
}