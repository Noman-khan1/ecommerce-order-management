package com.noman.ecommerce_order_management.cart;

import com.noman.ecommerce_order_management.cart.dto.AddCartItemRequest;
import com.noman.ecommerce_order_management.cart.dto.CartItemResponse;
import com.noman.ecommerce_order_management.cart.dto.CartResponse;
import com.noman.ecommerce_order_management.cart.dto.UpdateCartItemRequest;
import com.noman.ecommerce_order_management.catalog.Sku;
import com.noman.ecommerce_order_management.catalog.SkuRepository;
import com.noman.ecommerce_order_management.user.Role;
import com.noman.ecommerce_order_management.user.User;
import com.noman.ecommerce_order_management.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SkuRepository skuRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(String customerEmail) {

        User customer = getCustomer(customerEmail);

        return cartRepository
                .findByCustomerId(customer.getId())
                .map(this::toCartResponse)
                .orElseGet(() ->
                        emptyCartResponse(customer)
                );
    }

    @Transactional
    public CartResponse addItem(
            String customerEmail,
            AddCartItemRequest request
    ) {

        User customer = getCustomer(customerEmail);

        Cart cart = getOrCreateCart(customer);

        Sku sku = getPurchasableSku(
                request.getSkuId()
        );

        CartItem item = cartItemRepository
                .findByCartIdAndSkuId(
                        cart.getId(),
                        sku.getId()
                )
                .orElseGet(() ->
                        CartItem.builder()
                                .cart(cart)
                                .sku(sku)
                                .quantity(0)
                                .build()
                );

        item.setQuantity(
                item.getQuantity()
                        + request.getQuantity()
        );

        cartItemRepository.save(item);

        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(
            String customerEmail,
            Long itemId,
            UpdateCartItemRequest request
    ) {

        User customer = getCustomer(customerEmail);

        Cart cart = getExistingCart(customer);

        CartItem item = cartItemRepository
                .findByIdAndCartId(
                        itemId,
                        cart.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cart item not found"
                        )
                );

        getPurchasableSku(
                item.getSku().getId()
        );

        item.setQuantity(
                request.getQuantity()
        );

        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(
            String customerEmail,
            Long itemId
    ) {

        User customer = getCustomer(customerEmail);

        Cart cart = getExistingCart(customer);

        CartItem item = cartItemRepository
                .findByIdAndCartId(
                        itemId,
                        cart.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cart item not found"
                        )
                );

        cartItemRepository.delete(item);

        cartItemRepository.flush();

        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(
            String customerEmail
    ) {

        User customer = getCustomer(customerEmail);

        Cart cart = cartRepository
                .findByCustomerId(customer.getId())
                .orElse(null);

        if (cart == null) {
            return emptyCartResponse(customer);
        }

        cartItemRepository.deleteByCartId(
                cart.getId()
        );

        cartItemRepository.flush();

        return toCartResponse(cart);
    }

    private User getCustomer(
            String customerEmail
    ) {

        User customer = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Customer not found"
                        )
                );

        if (customer.getRole() != Role.CUSTOMER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not a customer"
            );
        }

        return customer;
    }

    private Cart getOrCreateCart(
            User customer
    ) {

        return cartRepository
                .findByCustomerId(customer.getId())
                .orElseGet(() ->
                        cartRepository.save(
                                Cart.builder()
                                        .customer(customer)
                                        .build()
                        )
                );
    }

    private Cart getExistingCart(
            User customer
    ) {

        return cartRepository
                .findByCustomerId(customer.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cart not found"
                        )
                );
    }

    private Sku getPurchasableSku(
            Long skuId
    ) {

        Sku sku = skuRepository
                .findById(skuId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "SKU not found"
                        )
                );

        if (!sku.isActive()
                || !sku.getProduct().isActive()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SKU is not available for purchase"
            );
        }

        return sku;
    }

    private CartResponse toCartResponse(
            Cart cart
    ) {

        List<CartItemResponse> items =
                cartItemRepository
                        .findDetailedByCartId(
                                cart.getId()
                        )
                        .stream()
                        .map(this::toCartItemResponse)
                        .toList();

        int totalItems = items
                .stream()
                .mapToInt(
                        CartItemResponse::getQuantity
                )
                .sum();

        BigDecimal subtotal = items
                .stream()
                .map(
                        CartItemResponse::getLineTotal
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        return CartResponse.builder()
                .cartId(cart.getId())
                .customerId(
                        cart.getCustomer().getId()
                )
                .customerEmail(
                        cart.getCustomer().getEmail()
                )
                .items(items)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .build();
    }

    private CartResponse emptyCartResponse(
            User customer
    ) {

        return CartResponse.builder()
                .cartId(null)
                .customerId(customer.getId())
                .customerEmail(customer.getEmail())
                .items(List.of())
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .build();
    }

    private CartItemResponse toCartItemResponse(
            CartItem item
    ) {

        BigDecimal lineTotal =
                item.getSku()
                        .getPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        item.getQuantity()
                                )
                        );

        return CartItemResponse.builder()
                .itemId(item.getId())
                .skuId(item.getSku().getId())
                .skuCode(
                        item.getSku().getSkuCode()
                )
                .productId(
                        item.getSku()
                                .getProduct()
                                .getId()
                )
                .productName(
                        item.getSku()
                                .getProduct()
                                .getName()
                )
                .variantName(
                        item.getSku()
                                .getVariantName()
                )
                .unitPrice(
                        item.getSku().getPrice()
                )
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}