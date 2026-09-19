package com.noman.ecommerce_order_management.cart;

import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.cart.dto.CartResponse;
import com.noman.ecommerce_order_management.cart.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CustomerCartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(
            Authentication authentication
    ) {

        return cartService.getCart(
                authentication.getName()
        );
    }

    @PostMapping("/items")
    public CartResponse addItem(
            Authentication authentication,
            @Valid
            @RequestBody
            AddCartItemRequest request
    ) {

        return cartService.addItem(
                authentication.getName(),
                request
        );
    }

    @PutMapping("/items/{itemId}")
    public CartResponse updateItemQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid
            @RequestBody
            UpdateCartItemRequest request
    ) {

        return cartService.updateItemQuantity(
                authentication.getName(),
                itemId,
                request
        );
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {

        return cartService.removeItem(
                authentication.getName(),
                itemId
        );
    }

    @DeleteMapping
    public CartResponse clearCart(
            Authentication authentication
    ) {

        return cartService.clearCart(
                authentication.getName()
        );
    }
}