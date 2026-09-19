package com.noman.ecommerce_order_management.notification;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.order.CustomerOrderRepository;
import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final CustomerOrderRepository
            customerOrderRepository;

    private final NotificationLogRepository
            notificationLogRepository;

    @Transactional
    public void sendOrderStatusNotification(
            OrderStatusChangedEvent event
    ) {

        boolean alreadySent =
                notificationLogRepository
                        .existsByOrderIdAndOrderStatus(
                                event.orderId(),
                                event.status()
                        );

        if (alreadySent) {
            return;
        }

        CustomerOrder order =
                customerOrderRepository
                        .findById(event.orderId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found for notification"
                                )
                        );

        NotificationLog notification =
                NotificationLog.builder()
                        .order(order)
                        .customerEmail(
                                event.customerEmail()
                        )
                        .channel(
                                NotificationChannel.EMAIL
                        )
                        .orderStatus(
                                event.status()
                        )
                        .status(
                                NotificationStatus.SIMULATED_SENT
                        )
                        .message(
                                buildMessage(event)
                        )
                        .build();

        notificationLogRepository.save(
                notification
        );
    }

    private String buildMessage(
            OrderStatusChangedEvent event
    ) {

        return switch (event.status()) {

            case CONFIRMED ->
                    "Order "
                            + event.orderNumber()
                            + " has been confirmed.";

            case PACKED ->
                    "Order "
                            + event.orderNumber()
                            + " has been packed.";

            case SHIPPED ->
                    "Order "
                            + event.orderNumber()
                            + " has been shipped.";

            case DELIVERED ->
                    "Order "
                            + event.orderNumber()
                            + " has been delivered.";

            case RETURNED ->
                    "Order "
                            + event.orderNumber()
                            + " has been returned.";

            case CANCELLED ->
                    "Order "
                            + event.orderNumber()
                            + " has been cancelled.";

            case PLACED ->
                    "Order "
                            + event.orderNumber()
                            + " has been placed.";
        };
    }
}