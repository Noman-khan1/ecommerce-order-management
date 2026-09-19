package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_orders_order_number",
                        columnNames = "order_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_customer_orders_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_customer_orders_status",
                        columnList = "order_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_number",
            nullable = false,
            length = 60
    )
    private String orderNumber;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_customer_order_customer"
            )
    )
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "order_status",
            nullable = false,
            length = 30
    )
    private OrderStatus status;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal subtotal;

    @Column(
            name = "discount_code",
            length = 50
    )
    private String discountCode;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "taxable_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxableAmount;

    @Column(
            name = "tax_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal taxRate;

    @Column(
            name = "tax_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxAmount;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "shipping_address",
            nullable = false,
            length = 500
    )
    private String shippingAddress;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}