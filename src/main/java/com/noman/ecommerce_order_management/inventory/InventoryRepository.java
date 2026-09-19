package com.noman.ecommerce_order_management.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAllBySkuIdOrderByWarehouseIdAsc(
            Long skuId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            where i.sku.id = :skuId
              and i.warehouse.id = :warehouseId
            """)
    Optional<Inventory> findBySkuAndWarehouseForUpdate(
            @Param("skuId") Long skuId,
            @Param("warehouseId") Long warehouseId
    );

    List<Inventory> findByWarehouseIdOrderBySkuIdAsc(
            Long warehouseId
    );

    List<Inventory> findAllByOrderByIdAsc();
}