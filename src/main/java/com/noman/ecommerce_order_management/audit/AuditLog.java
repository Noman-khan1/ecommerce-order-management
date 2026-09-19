package com.noman.ecommerce_order_management.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_audit_entity_event",
                        columnNames = {
                                "entity_type",
                                "entity_id",
                                "event_type"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_audit_entity",
                        columnList = "entity_type,entity_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "entity_type",
            nullable = false,
            length = 50
    )
    private String entityType;

    @Column(
            name = "entity_id",
            nullable = false
    )
    private Long entityId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            nullable = false,
            length = 500
    )
    private String details;

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