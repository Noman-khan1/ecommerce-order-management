package com.noman.ecommerce_order_management.discount;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "discount_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_discount_codes_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_discount_codes_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 50
    )
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private DiscountType type;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal value;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal minimumOrderAmount;

    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal maximumDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

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