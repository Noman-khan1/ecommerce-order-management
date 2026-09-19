package com.noman.ecommerce_order_management.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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

    /*
     * Normal read-only lookup.
     * Used by admin/read APIs.
     */
    List<Inventory> findBySkuIdOrderByWarehouseIdAsc(
            Long skuId
    );

    /*
     * Checkout-only lookup.
     *
     * PESSIMISTIC_WRITE locks all inventory rows
     * belonging to this SKU until the surrounding
     * transaction completes.
     *
     * If another checkout tries to reserve the same
     * SKU concurrently, it must wait for this lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAllBySkuIdOrderByWarehouseIdAsc(
            Long skuId
    );

    List<Inventory> findByWarehouseIdOrderBySkuIdAsc(
            Long warehouseId
    );

    List<Inventory> findAllByOrderByIdAsc();
}