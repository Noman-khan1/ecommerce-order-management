package com.noman.ecommerce_order_management.cart;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CartRepository
        extends JpaRepository<Cart, Long> {

    /*
     * Normal cart reads.
     */
    Optional<Cart> findByCustomerId(
            Long customerId
    );

    /*
     * Checkout uses this method.
     *
     * Locking the cart prevents the same customer
     * from processing the same cart twice when two
     * checkout requests arrive simultaneously.
     *
     * Database unique constraint guarantees at most
     * one cart for the customer.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Cart> findAllByCustomerId(
            Long customerId
    );
}