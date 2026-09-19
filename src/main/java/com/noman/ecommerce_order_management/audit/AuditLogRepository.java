package com.noman.ecommerce_order_management.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    boolean existsByEntityTypeAndEntityIdAndEventType(
            String entityType,
            Long entityId,
            String eventType
    );

    List<AuditLog>
    findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            String entityType,
            Long entityId
    );
}