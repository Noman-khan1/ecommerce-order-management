package com.noman.ecommerce_order_management.pricing;

import com.noman.ecommerce_order_management.cart.CartService;
import com.noman.ecommerce_order_management.cart.dto.CartResponse;
import com.noman.ecommerce_order_management.discount.DiscountService;
import com.noman.ecommerce_order_management.pricing.dto.PricingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final CartService cartService;
    private final DiscountService discountService;
    private final TaxService taxService;

    @Transactional(readOnly = true)
    public PricingResponse previewPricing(
            String customerEmail,
            String discountCode
    ) {

        CartResponse cart =
                cartService.getCart(
                        customerEmail
                );

        BigDecimal subtotal =
                money(cart.getSubtotal());

        BigDecimal discountAmount =
                discountService.calculateDiscount(
                        discountCode,
                        subtotal
                );

        BigDecimal taxableAmount =
                subtotal.subtract(
                        discountAmount
                );

        if (taxableAmount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            taxableAmount =
                    BigDecimal.ZERO;
        }

        taxableAmount =
                money(taxableAmount);

        BigDecimal taxAmount =
                taxService.calculateTax(
                        taxableAmount
                );

        BigDecimal totalAmount =
                taxableAmount.add(
                        taxAmount
                );

        return PricingResponse.builder()
                .subtotal(subtotal)
                .discountCode(
                        normalizeDiscountCode(
                                discountCode
                        )
                )
                .discountAmount(
                        discountAmount
                )
                .taxableAmount(
                        taxableAmount
                )
                .taxRate(
                        taxService.getTaxRate()
                )
                .taxAmount(
                        taxAmount
                )
                .totalAmount(
                        money(totalAmount)
                )
                .build();
    }

    private String normalizeDiscountCode(
            String discountCode
    ) {

        if (discountCode == null
                || discountCode.isBlank()) {

            return null;
        }

        return discountCode
                .trim()
                .toUpperCase(Locale.ROOT);
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