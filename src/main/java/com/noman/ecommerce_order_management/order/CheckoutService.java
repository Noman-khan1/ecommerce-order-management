package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.cart.Cart;
import com.noman.ecommerce_order_management.cart.CartItem;
import com.noman.ecommerce_order_management.cart.CartItemRepository;
import com.noman.ecommerce_order_management.cart.CartRepository;
import com.noman.ecommerce_order_management.catalog.Sku;
import com.noman.ecommerce_order_management.inventory.Inventory;
import com.noman.ecommerce_order_management.inventory.InventoryService;
import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import com.noman.ecommerce_order_management.payment.Payment;
import com.noman.ecommerce_order_management.payment.PaymentService;
import com.noman.ecommerce_order_management.payment.PaymentStatus;
import com.noman.ecommerce_order_management.pricing.PricingService;
import com.noman.ecommerce_order_management.pricing.dto.PricingResponse;
import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    @Transactional
    public OrderResponse checkout(
            String customerEmail,
            CheckoutRequest request
    ) {

        validateRequest(request);

        User customer =
                getCustomer(customerEmail);

        Cart cart =
                cartRepository
                        .findByCustomerId(
                                customer.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Cart is empty"
                                )
                        );

        List<CartItem> cartItems =
                cartItemRepository
                        .findDetailedByCartId(
                                cart.getId()
                        );

        if (cartItems.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart is empty"
            );
        }

        validateCartItems(cartItems);

        PricingResponse pricing =
                pricingService
                        .previewPricing(
                                customerEmail,
                                request.getDiscountCode()
                        );

        CustomerOrder order =
                CustomerOrder.builder()
                        .orderNumber(
                                generateOrderNumber()
                        )
                        .customer(customer)
                        .status(
                                OrderStatus.PLACED
                        )
                        .subtotal(
                                pricing.getSubtotal()
                        )
                        .discountCode(
                                pricing.getDiscountCode()
                        )
                        .discountAmount(
                                pricing.getDiscountAmount()
                        )
                        .taxableAmount(
                                pricing.getTaxableAmount()
                        )
                        .taxRate(
                                pricing.getTaxRate()
                        )
                        .taxAmount(
                                pricing.getTaxAmount()
                        )
                        .totalAmount(
                                pricing.getTotalAmount()
                        )
                        .shippingAddress(
                                request
                                        .getShippingAddress()
                                        .trim()
                        )
                        .build();

        order =
                customerOrderRepository
                        .saveAndFlush(order);

        List<OrderItem> orderItems =
                new ArrayList<>();

        for (CartItem cartItem
                : cartItems) {

            Sku sku =
                    cartItem.getSku();

            Inventory inventory =
                    inventoryService
                            .reserveInventory(
                                    sku.getId(),
                                    cartItem.getQuantity()
                            );

            BigDecimal lineTotal =
                    money(
                            sku.getPrice()
                                    .multiply(
                                            BigDecimal
                                                    .valueOf(
                                                            cartItem
                                                                    .getQuantity()
                                                    )
                                    )
                    );

            OrderItem orderItem =
                    OrderItem.builder()
                            .order(order)
                            .sku(sku)
                            .warehouse(
                                    inventory
                                            .getWarehouse()
                            )
                            .skuCode(
                                    sku.getSkuCode()
                            )
                            .productName(
                                    sku.getProduct()
                                            .getName()
                            )
                            .variantName(
                                    sku.getVariantName()
                            )
                            .unitPrice(
                                    sku.getPrice()
                            )
                            .quantity(
                                    cartItem
                                            .getQuantity()
                            )
                            .lineTotal(
                                    lineTotal
                            )
                            .build();

            orderItems.add(orderItem);
        }

        orderItemRepository
                .saveAllAndFlush(orderItems);

        Payment payment =
                paymentService
                        .processPayment(
                                order,
                                request.getPaymentMethod(),
                                pricing.getTotalAmount()
                        );

        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment failed"
            );
        }

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        customerOrderRepository
                .saveAndFlush(order);

        /*
         * Clear cart only after order,
         * inventory reservation and
         * payment all succeed.
         */
        cartItemRepository
                .deleteByCartId(
                        cart.getId()
                );

        cartItemRepository.flush();

        return orderService
                .toResponse(order);
    }

    private void validateCartItems(
            List<CartItem> cartItems
    ) {

        for (CartItem item
                : cartItems) {

            Sku sku =
                    item.getSku();

            if (!sku.isActive()
                    || !sku.getProduct()
                    .isActive()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cart contains unavailable SKU "
                                + sku.getSkuCode()
                );
            }

            if (item.getQuantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cart contains invalid quantity"
                );
            }
        }
    }

    private void validateRequest(
            CheckoutRequest request
    ) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Checkout request is required"
            );
        }

        if (request.getPaymentMethod()
                == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required"
            );
        }

        if (request.getShippingAddress()
                == null
                || request
                .getShippingAddress()
                .isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shipping address is required"
            );
        }

        if (request.getShippingAddress()
                .trim()
                .length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shipping address cannot exceed 500 characters"
            );
        }
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

    private String generateOrderNumber() {

        return "ORD-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    private BigDecimal money(
            BigDecimal amount
    ) {

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}