package com.noman.ecommerce_order_management.discount;

import com.noman.ecommerce_order_management.discount.dto.DiscountRequest;
import com.noman.ecommerce_order_management.discount.dto.DiscountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discounts")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(
            @Valid
            @RequestBody
            DiscountRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        discountService.createDiscount(
                                request
                        )
                );
    }

    @PutMapping("/{discountId}")
    public DiscountResponse updateDiscount(
            @PathVariable Long discountId,
            @Valid
            @RequestBody
            DiscountRequest request
    ) {

        return discountService.updateDiscount(
                discountId,
                request
        );
    }

    @PatchMapping("/{discountId}/status")
    public DiscountResponse updateDiscountStatus(
            @PathVariable Long discountId,
            @RequestParam boolean active
    ) {

        return discountService
                .updateDiscountStatus(
                        discountId,
                        active
                );
    }

    @GetMapping
    public List<DiscountResponse> getDiscounts() {

        return discountService.getDiscounts();
    }
}