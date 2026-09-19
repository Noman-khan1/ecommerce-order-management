package com.noman.ecommerce_order_management.returns;

import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.order.CustomerOrder;
import com.noman.ecommerce_order_management.order.CustomerOrderRepository;
import com.noman.ecommerce_order_management.order.OrderItem;
import com.noman.ecommerce_order_management.order.OrderItemRepository;
import com.noman.ecommerce_order_management.order.OrderStatus;
import com.noman.ecommerce_order_management.order.event.OrderStatusChangedEvent;
import com.noman.ecommerce_order_management.payment.Payment;
import com.noman.ecommerce_order_management.payment.PaymentRepository;
import com.noman.ecommerce_order_management.payment.PaymentService;
import com.noman.ecommerce_order_management.returns.dto.ReturnOrderRequest;
import com.noman.ecommerce_order_management.returns.dto.ReturnResponse;
import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final UserRepository userRepository;

    private final CustomerOrderRepository
            customerOrderRepository;

    private final OrderItemRepository
            orderItemRepository;

    private final OrderReturnRepository
            orderReturnRepository;

    private final PaymentService
            paymentService;

    private final PaymentRepository
            paymentRepository;

    private final InventoryService
            inventoryService;

    private final ApplicationEventPublisher
            eventPublisher;

    @Transactional
    public ReturnResponse returnOrder(
            String customerEmail,
            Long orderId,
            ReturnOrderRequest request
    ) {

        validateRequest(request);

        User customer =
                getCustomer(
                        customerEmail
                );

        /*
         * Lock order first.
         *
         * This prevents duplicate/concurrent
         * return processing for the same order.
         */
        CustomerOrder order =
                customerOrderRepository
                        .findByIdForUpdate(
                                orderId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        /*
         * Return 404 instead of exposing that
         * another customer's order exists.
         */
        if (!order.getCustomer()
                .getId()
                .equals(
                        customer.getId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        if (order.getStatus()
                != OrderStatus.DELIVERED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only delivered orders can be returned"
            );
        }

        if (orderReturnRepository
                .existsByOrderId(
                        orderId
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has already been returned"
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrderIdOrderByIdAsc(
                                orderId
                        )
                        .stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                (OrderItem item) ->
                                                        item.getSku()
                                                                .getId()
                                        )
                                        .thenComparing(
                                                item ->
                                                        item.getWarehouse()
                                                                .getId()
                                        )
                        )
                        .toList();

        if (orderItems.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has no items to return"
            );
        }

        /*
         * Simulated full refund.
         *
         * This payment update participates in the
         * same database transaction.
         */
        Payment payment =
                paymentService
                        .refundPayment(
                                orderId
                        );

        /*
         * Restore each item to the same warehouse
         * that originally fulfilled it.
         *
         * Deterministic lock ordering reduces
         * deadlock risk.
         */
        for (OrderItem item
                : orderItems) {

            inventoryService
                    .restockReturnedInventory(
                            item.getSku()
                                    .getId(),
                            item.getWarehouse()
                                    .getId(),
                            item.getQuantity()
                    );
        }

        LocalDateTime completedAt =
                LocalDateTime.now();

        OrderReturn orderReturn =
                OrderReturn.builder()
                        .order(order)
                        .reason(
                                request
                                        .getReason()
                                        .trim()
                        )
                        .refundAmount(
                                payment.getAmount()
                        )
                        .status(
                                ReturnStatus.COMPLETED
                        )
                        .completedAt(
                                completedAt
                        )
                        .build();

        orderReturn =
                orderReturnRepository
                        .saveAndFlush(
                                orderReturn
                        );

        order.setStatus(
                OrderStatus.RETURNED
        );

        customerOrderRepository
                .saveAndFlush(
                        order
                );

        /*
         * Commit 10 listeners already know how
         * to handle RETURNED.
         *
         * Notification + audit occur asynchronously
         * only AFTER this transaction commits.
         */
        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(
                        order.getId(),
                        order.getOrderNumber(),
                        customer.getEmail(),
                        OrderStatus.RETURNED
                )
        );

        return toResponse(
                orderReturn,
                payment
        );
    }

    @Transactional(readOnly = true)
    public ReturnResponse getReturn(
            String customerEmail,
            Long orderId
    ) {

        User customer =
                getCustomer(
                        customerEmail
                );

        CustomerOrder order =
                customerOrderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customer.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        OrderReturn orderReturn =
                orderReturnRepository
                        .findByOrderId(
                                orderId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Return not found"
                                )
                        );

        Payment payment =
                paymentRepository
                        .findByOrderId(
                                orderId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Payment record missing for order"
                                )
                        );

        return toResponse(
                orderReturn,
                payment
        );
    }

    private void validateRequest(
            ReturnOrderRequest request
    ) {

        if (request == null
                || request.getReason() == null
                || request.getReason()
                .isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Return reason is required"
            );
        }

        if (request.getReason()
                .trim()
                .length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Return reason cannot exceed 500 characters"
            );
        }
    }

    private User getCustomer(
            String customerEmail
    ) {

        User customer =
                userRepository
                        .findByEmail(
                                customerEmail
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer not found"
                                )
                        );

        if (customer.getRole()
                != Role.CUSTOMER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not a customer"
            );
        }

        return customer;
    }

    private ReturnResponse toResponse(
            OrderReturn orderReturn,
            Payment payment
    ) {

        CustomerOrder order =
                orderReturn.getOrder();

        return ReturnResponse.builder()
                .returnId(
                        orderReturn.getId()
                )
                .orderId(
                        order.getId()
                )
                .orderNumber(
                        order.getOrderNumber()
                )
                .orderStatus(
                        order.getStatus()
                )
                .returnStatus(
                        orderReturn.getStatus()
                )
                .reason(
                        orderReturn.getReason()
                )
                .refundAmount(
                        orderReturn
                                .getRefundAmount()
                )
                .refundReference(
                        payment
                                .getRefundReference()
                )
                .paymentStatus(
                        payment.getStatus()
                )
                .createdAt(
                        orderReturn.getCreatedAt()
                )
                .completedAt(
                        orderReturn.getCompletedAt()
                )
                .refundedAt(
                        payment.getRefundedAt()
                )
                .build();
    }
}