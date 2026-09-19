package com.noman.ecommerce_order_management.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "skus",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skus_sku_code",
                        columnNames = "sku_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_skus_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_skus_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "sku_code",
            nullable = false,
            length = 100
    )
    private String skuCode;

    @Column(length = 150)
    private String variantName;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sku_product")
    )
    private Product product;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}