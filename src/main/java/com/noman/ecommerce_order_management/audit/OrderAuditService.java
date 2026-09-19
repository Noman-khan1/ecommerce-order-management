package com.noman.ecommerce_order_management.audit;

import com.noman.ecommerce_order_management.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderAuditService {

    private static final String ENTITY_TYPE =
            "ORDER";

    private final AuditLogRepository
            auditLogRepository;

    @Transactional
    public void recordOrderStatusChange(
            OrderStatusChangedEvent event
    ) {

        String eventType =
                "ORDER_"
                        + event.status().name();

        boolean alreadyExists =
                auditLogRepository
                        .existsByEntityTypeAndEntityIdAndEventType(
                                ENTITY_TYPE,
                                event.orderId(),
                                eventType
                        );

        if (alreadyExists) {
            return;
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .entityType(
                                ENTITY_TYPE
                        )
                        .entityId(
                                event.orderId()
                        )
                        .eventType(
                                eventType
                        )
                        .details(
                                "Order "
                                        + event.orderNumber()
                                        + " changed to "
                                        + event.status().name()
                        )
                        .build();

        auditLogRepository.save(
                auditLog
        );
    }
}