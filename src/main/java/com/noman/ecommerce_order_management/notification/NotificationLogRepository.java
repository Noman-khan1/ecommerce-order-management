package com.noman.ecommerce_order_management.notification;

import com.noman.ecommerce_order_management.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long> {

    boolean existsByOrderIdAndOrderStatus(
            Long orderId,
            OrderStatus orderStatus
    );

    List<NotificationLog>
    findByOrderIdOrderByCreatedAtAsc(
            Long orderId
    );
}