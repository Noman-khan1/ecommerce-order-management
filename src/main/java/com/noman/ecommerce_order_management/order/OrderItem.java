package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.catalog.Sku;
import com.noman.ecommerce_order_management.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(
                        name = "idx_order_items_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_items_sku",
                        columnList = "sku_id"
                ),
                @Index(
                        name = "idx_order_items_warehouse",
                        columnList = "warehouse_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_item_order"
            )
    )
    private CustomerOrder order;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "sku_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_item_sku"
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
                    name = "fk_order_item_warehouse"
            )
    )
    private Warehouse warehouse;

    @Column(
            name = "sku_code",
            nullable = false,
            length = 100
    )
    private String skuCode;

    @Column(
            name = "product_name",
            nullable = false,
            length = 150
    )
    private String productName;

    @Column(
            name = "variant_name",
            length = 150
    )
    private String variantName;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(
            name = "line_total",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal lineTotal;
}