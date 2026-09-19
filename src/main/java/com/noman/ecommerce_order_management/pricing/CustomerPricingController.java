package com.noman.ecommerce_order_management.pricing;

import com.noman.ecommerce_order_management.pricing.dto.PricingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/pricing")
@RequiredArgsConstructor
public class CustomerPricingController {

    private final PricingService pricingService;

    @GetMapping("/preview")
    public PricingResponse previewPricing(
            Authentication authentication,
            @RequestParam(
                    required = false
            )
            String discountCode
    ) {

        return pricingService.previewPricing(
                authentication.getName(),
                discountCode
        );
    }
}