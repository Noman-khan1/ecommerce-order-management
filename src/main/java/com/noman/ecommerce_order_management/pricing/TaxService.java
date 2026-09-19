package com.noman.ecommerce_order_management.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final BigDecimal taxRate;

    public TaxService(
            @Value("${app.tax.rate:18.00}")
            BigDecimal taxRate
    ) {

        if (taxRate.compareTo(BigDecimal.ZERO) < 0
                || taxRate.compareTo(
                ONE_HUNDRED
        ) > 0) {

            throw new IllegalArgumentException(
                    "Tax rate must be between 0 and 100"
            );
        }

        this.taxRate = taxRate;
    }

    public BigDecimal calculateTax(
            BigDecimal taxableAmount
    ) {

        if (taxableAmount == null
                || taxableAmount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return money(BigDecimal.ZERO);
        }

        return taxableAmount
                .multiply(taxRate)
                .divide(
                        ONE_HUNDRED,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public BigDecimal getTaxRate() {

        return money(taxRate);
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