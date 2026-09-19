package com.noman.ecommerce_order_management.payment;

import com.noman.ecommerce_order_management.order.CustomerOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment processPayment(
            CustomerOrder order,
            PaymentMethod paymentMethod,
            BigDecimal amount
    ) {

        if (paymentMethod == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required"
            );
        }

        if (amount == null
                || amount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount cannot be negative"
            );
        }

        /*
         * Assignment assumption:
         * external payment provider is simulated.
         *
         * A real production system would call Stripe,
         * Razorpay, etc. with idempotency and would not
         * hold a DB transaction during a remote API call.
         */
        Payment payment =
                Payment.builder()
                        .order(order)
                        .paymentReference(
                                generatePaymentReference()
                        )
                        .method(paymentMethod)
                        .status(
                                PaymentStatus.SUCCESS
                        )
                        .amount(amount)
                        .processedAt(
                                LocalDateTime.now()
                        )
                        .build();

        return paymentRepository
                .saveAndFlush(payment);
    }

    private String generatePaymentReference() {

        return "PAY-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }
}