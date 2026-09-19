package com.noman.ecommerce_order_management.discount;

import com.noman.ecommerce_order_management.discount.dto.DiscountRequest;
import com.noman.ecommerce_order_management.discount.dto.DiscountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final DiscountRepository discountRepository;

    @Transactional
    public DiscountResponse createDiscount(
            DiscountRequest request
    ) {

        validateRequest(request);

        String code =
                normalizeCode(request.getCode());

        discountRepository
                .findByCodeIgnoreCase(code)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Discount code already exists"
                    );
                });

        DiscountCode discount =
                DiscountCode.builder()
                        .code(code)
                        .type(request.getType())
                        .value(request.getValue())
                        .minimumOrderAmount(
                                request.getMinimumOrderAmount()
                        )
                        .maximumDiscountAmount(
                                request.getMaximumDiscountAmount()
                        )
                        .validFrom(request.getValidFrom())
                        .validUntil(request.getValidUntil())
                        .active(true)
                        .build();

        return toResponse(
                discountRepository.save(discount)
        );
    }

    @Transactional
    public DiscountResponse updateDiscount(
            Long discountId,
            DiscountRequest request
    ) {

        validateRequest(request);

        DiscountCode discount =
                getDiscount(discountId);

        String code =
                normalizeCode(request.getCode());

        discountRepository
                .findByCodeIgnoreCase(code)
                .filter(existing ->
                        !existing.getId()
                                .equals(discountId)
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Discount code already exists"
                    );
                });

        discount.setCode(code);
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setMinimumOrderAmount(
                request.getMinimumOrderAmount()
        );
        discount.setMaximumDiscountAmount(
                request.getMaximumDiscountAmount()
        );
        discount.setValidFrom(
                request.getValidFrom()
        );
        discount.setValidUntil(
                request.getValidUntil()
        );

        return toResponse(discount);
    }

    @Transactional
    public DiscountResponse updateDiscountStatus(
            Long discountId,
            boolean active
    ) {

        DiscountCode discount =
                getDiscount(discountId);

        discount.setActive(active);

        return toResponse(discount);
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getDiscounts() {

        return discountRepository
                .findAllByOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(
            String discountCode,
            BigDecimal subtotal
    ) {

        if (subtotal == null
                || subtotal.compareTo(BigDecimal.ZERO) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subtotal cannot be negative"
            );
        }

        if (discountCode == null
                || discountCode.isBlank()) {

            return money(BigDecimal.ZERO);
        }

        String normalizedCode =
                normalizeCode(discountCode);

        DiscountCode discount =
                discountRepository
                        .findByCodeIgnoreCase(
                                normalizedCode
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid discount code"
                                )
                        );

        validateDiscountForUse(
                discount,
                subtotal
        );

        BigDecimal discountAmount;

        if (discount.getType()
                == DiscountType.PERCENTAGE) {

            discountAmount =
                    subtotal
                            .multiply(
                                    discount.getValue()
                            )
                            .divide(
                                    ONE_HUNDRED,
                                    2,
                                    RoundingMode.HALF_UP
                            );

        } else {

            discountAmount =
                    discount.getValue();
        }

        if (discount.getMaximumDiscountAmount()
                != null
                && discountAmount.compareTo(
                discount.getMaximumDiscountAmount()
        ) > 0) {

            discountAmount =
                    discount.getMaximumDiscountAmount();
        }

        if (discountAmount.compareTo(subtotal) > 0) {

            discountAmount = subtotal;
        }

        return money(discountAmount);
    }

    private void validateDiscountForUse(
            DiscountCode discount,
            BigDecimal subtotal
    ) {

        if (!discount.isActive()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount code is inactive"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (now.isBefore(
                discount.getValidFrom()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount code is not active yet"
            );
        }

        if (now.isAfter(
                discount.getValidUntil()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount code has expired"
            );
        }

        if (subtotal.compareTo(
                discount.getMinimumOrderAmount()
        ) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum order amount not met for discount"
            );
        }
    }

    private void validateRequest(
            DiscountRequest request
    ) {

        if (request.getValidUntil()
                .isBefore(request.getValidFrom())
                || request.getValidUntil()
                .isEqual(request.getValidFrom())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid until must be after valid from"
            );
        }

        if (request.getType()
                == DiscountType.PERCENTAGE
                && request.getValue()
                .compareTo(ONE_HUNDRED) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Percentage discount cannot exceed 100"
            );
        }
    }

    private DiscountCode getDiscount(
            Long discountId
    ) {

        return discountRepository
                .findById(discountId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Discount not found"
                        )
                );
    }

    private DiscountResponse toResponse(
            DiscountCode discount
    ) {

        return DiscountResponse.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .type(discount.getType())
                .value(discount.getValue())
                .minimumOrderAmount(
                        discount.getMinimumOrderAmount()
                )
                .maximumDiscountAmount(
                        discount.getMaximumDiscountAmount()
                )
                .validFrom(discount.getValidFrom())
                .validUntil(discount.getValidUntil())
                .active(discount.isActive())
                .build();
    }

    private String normalizeCode(
            String code
    ) {

        return code
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