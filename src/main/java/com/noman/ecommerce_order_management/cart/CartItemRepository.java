package com.noman.ecommerce_order_management.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndSkuId(
            Long cartId,
            Long skuId
    );

    Optional<CartItem> findByIdAndCartId(
            Long itemId,
            Long cartId
    );

    @Query("""
            select ci
            from CartItem ci
            join fetch ci.sku s
            join fetch s.product p
            where ci.cart.id = :cartId
            order by ci.id
            """)
    List<CartItem> findDetailedByCartId(
            @Param("cartId") Long cartId
    );

    void deleteByCartId(Long cartId);
}