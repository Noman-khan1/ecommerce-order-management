package com.noman.ecommerce_order_management.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findBySkuIdAndWarehouseId(
            Long skuId,
            Long warehouseId
    );

    boolean existsBySkuIdAndWarehouseId(
            Long skuId,
            Long warehouseId
    );

    List<Inventory> findBySkuIdOrderByWarehouseIdAsc(
            Long skuId
    );

    List<Inventory> findByWarehouseIdOrderBySkuIdAsc(
            Long warehouseId
    );

    List<Inventory> findAllByOrderByIdAsc();
}