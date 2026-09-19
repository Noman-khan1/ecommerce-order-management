package com.noman.ecommerce_order_management.inventory;

import com.noman.ecommerce_order_management.catalog.Sku;
import com.noman.ecommerce_order_management.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_sku_warehouse",
                        columnNames = {
                                "sku_id",
                                "warehouse_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_inventory_sku",
                        columnList = "sku_id"
                ),
                @Index(
                        name = "idx_inventory_warehouse",
                        columnList = "warehouse_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "sku_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_sku"
            )
    )
    private Sku sku;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_warehouse"
            )
    )
    private Warehouse warehouse;

    @Column(nullable = false)
    private int availableQuantity;

    @Builder.Default
    @Column(nullable = false)
    private int reservedQuantity = 0;

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