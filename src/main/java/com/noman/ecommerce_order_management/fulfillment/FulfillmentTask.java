package com.noman.ecommerce_order_management.fulfillment;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fulfillment_tasks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fulfillment_order_warehouse",
                        columnNames = {
                                "order_id",
                                "warehouse_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_fulfillment_tasks_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_fulfillment_tasks_warehouse",
                        columnList = "warehouse_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FulfillmentTask {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_fulfillment_task_order"
            )
    )
    private CustomerOrder order;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_fulfillment_task_warehouse"
            )
    )
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "task_status",
            nullable = false,
            length = 30
    )
    private FulfillmentTaskStatus status;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {

        if (createdAt == null) {

            createdAt =
                    LocalDateTime.now();
        }
    }
}