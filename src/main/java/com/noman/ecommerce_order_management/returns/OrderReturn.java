package com.noman.ecommerce_order_management.returns;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_returns",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_returns_order",
                        columnNames = "order_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_returns_order",
                        columnList = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturn {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_order_return_order"
            )
    )
    private CustomerOrder order;

    @Column(
            name = "return_reason",
            nullable = false,
            length = 500
    )
    private String reason;

    @Column(
            name = "refund_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "return_status",
            nullable = false,
            length = 30
    )
    private ReturnStatus status;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

    @PrePersist
    void prePersist() {

        if (createdAt == null) {

            createdAt =
                    LocalDateTime.now();
        }
    }
}