package com.noman.ecommerce_order_management.discount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository
        extends JpaRepository<DiscountCode, Long> {

    Optional<DiscountCode> findByCodeIgnoreCase(
            String code
    );

    List<DiscountCode> findAllByOrderByCodeAsc();
}