package com.noman.ecommerce_order_management.order.event;

import com.noman.ecommerce_order_management.audit.OrderAuditService;
import com.noman.ecommerce_order_management.fulfillment.FulfillmentRoutingService;
import com.noman.ecommerce_order_management.notification.CustomerNotificationService;
import com.noman.ecommerce_order_management.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusAsyncListener {

    private final FulfillmentRoutingService
            fulfillmentRoutingService;

    private final CustomerNotificationService
            customerNotificationService;

    private final OrderAuditService
            orderAuditService;

    @Async("orderEventsExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleFulfillmentRouting(
            OrderStatusChangedEvent event
    ) {

        if (event.status()
                != OrderStatus.CONFIRMED) {

            return;
        }

        try {

            fulfillmentRoutingService
                    .createFulfillmentTasks(
                            event.orderId()
                    );

        } catch (Exception exception) {

            log.error(
                    "Async fulfillment routing failed for order {}",
                    event.orderId(),
                    exception
            );
        }
    }

    @Async("orderEventsExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleCustomerNotification(
            OrderStatusChangedEvent event
    ) {

        try {

            customerNotificationService
                    .sendOrderStatusNotification(
                            event
                    );

        } catch (Exception exception) {

            log.error(
                    "Async notification failed for order {} and status {}",
                    event.orderId(),
                    event.status(),
                    exception
            );
        }
    }

    @Async("orderEventsExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleAuditLogging(
            OrderStatusChangedEvent event
    ) {

        try {

            orderAuditService
                    .recordOrderStatusChange(
                            event
                    );

        } catch (Exception exception) {

            log.error(
                    "Async audit logging failed for order {} and status {}",
                    event.orderId(),
                    event.status(),
                    exception
            );
        }
    }
}