package com.noman.ecommerce_order_management.notification;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.order.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_order_status",
                        columnNames = {
                                "order_id",
                                "order_status"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_notification_logs_order",
                        columnList = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

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
                    name = "fk_notification_order"
            )
    )
    private CustomerOrder order;

    @Column(
            name = "customer_email",
            nullable = false,
            length = 150
    )
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_channel",
            nullable = false,
            length = 30
    )
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "order_status",
            nullable = false,
            length = 30
    )
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_status",
            nullable = false,
            length = 30
    )
    private NotificationStatus status;

    @Column(
            name = "notification_message",
            nullable = false,
            length = 500
    )
    private String message;

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