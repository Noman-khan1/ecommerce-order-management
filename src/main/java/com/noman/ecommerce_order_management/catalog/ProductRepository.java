package com.noman.ecommerce_order_management.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);

    Optional<Product> findByIdAndActiveTrue(Long id);
}