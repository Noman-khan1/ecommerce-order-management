package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.order.dto.OrderItemResponse;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.Payment;
import com.noman.ecommerce_order_management.payment.PaymentRepository;
import com.noman.ecommerce_order_management.payment.dto.PaymentResponse;
import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(
            String customerEmail
    ) {

        User customer =
                getCustomer(customerEmail);

        return customerOrderRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customer.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            String customerEmail,
            Long orderId
    ) {

        User customer =
                getCustomer(customerEmail);

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

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse toResponse(
            CustomerOrder order
    ) {

        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrderIdOrderByIdAsc(
                                order.getId()
                        )
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        Payment payment =
                paymentRepository
                        .findByOrderId(
                                order.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Payment record missing for order"
                                )
                        );

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(
                        order.getOrderNumber()
                )
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountCode(
                        order.getDiscountCode()
                )
                .discountAmount(
                        order.getDiscountAmount()
                )
                .taxableAmount(
                        order.getTaxableAmount()
                )
                .taxRate(order.getTaxRate())
                .taxAmount(
                        order.getTaxAmount()
                )
                .totalAmount(
                        order.getTotalAmount()
                )
                .shippingAddress(
                        order.getShippingAddress()
                )
                .createdAt(order.getCreatedAt())
                .items(items)
                .payment(
                        toPaymentResponse(payment)
                )
                .build();
    }

    private OrderItemResponse toItemResponse(
            OrderItem item
    ) {

        return OrderItemResponse.builder()
                .orderItemId(item.getId())
                .skuId(
                        item.getSku().getId()
                )
                .skuCode(item.getSkuCode())
                .productId(
                        item.getSku()
                                .getProduct()
                                .getId()
                )
                .productName(
                        item.getProductName()
                )
                .variantName(
                        item.getVariantName()
                )
                .unitPrice(
                        item.getUnitPrice()
                )
                .quantity(item.getQuantity())
                .lineTotal(
                        item.getLineTotal()
                )
                .warehouseId(
                        item.getWarehouse()
                                .getId()
                )
                .warehouseCode(
                        item.getWarehouse()
                                .getCode()
                )
                .build();
    }

    private PaymentResponse toPaymentResponse(
            Payment payment
    ) {

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentReference(
                        payment.getPaymentReference()
                )
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .processedAt(
                        payment.getProcessedAt()
                )
                .build();
    }

    private User getCustomer(
            String customerEmail
    ) {

        User customer =
                userRepository
                        .findByEmail(customerEmail)
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
}