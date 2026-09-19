package com.noman.ecommerce_order_management.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCodeIgnoreCase(
            String code
    );

    List<Warehouse> findAllByOrderByCodeAsc();
}