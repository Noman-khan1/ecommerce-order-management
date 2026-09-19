package com.noman.ecommerce_order_management.order;

import com.noman.ecommerce_order_management.order.dto.CheckoutRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @Valid
            @RequestBody
            CheckoutRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        checkoutService.checkout(
                                authentication.getName(),
                                request
                        )
                );
    }

    @GetMapping
    public List<OrderResponse> getOrders(
            Authentication authentication
    ) {

        return orderService.getOrders(
                authentication.getName()
        );
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {

        return orderService.getOrder(
                authentication.getName(),
                orderId
        );
    }
}