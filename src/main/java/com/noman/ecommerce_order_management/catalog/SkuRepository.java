package com.noman.ecommerce_order_management.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    Optional<Sku> findBySkuCodeIgnoreCase(String skuCode);

    List<Sku> findByProductIdAndActiveTrueOrderByPriceAsc(Long productId);
}