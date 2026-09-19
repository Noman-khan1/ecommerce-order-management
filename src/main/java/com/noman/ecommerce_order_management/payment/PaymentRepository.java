package com.noman.ecommerce_order_management.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(
            Long orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            where p.order.id = :orderId
            """)
    Optional<Payment> findByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );
}