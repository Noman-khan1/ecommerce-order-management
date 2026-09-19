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

    @Transactional
    public Payment refundPayment(
            Long orderId
    ) {

        Payment payment =
                paymentRepository
                        .findByOrderIdForUpdate(
                                orderId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Payment not found for order"
                                )
                        );

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has already been refunded"
            );
        }

        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only successful payments can be refunded"
            );
        }

        /*
         * Assignment assumption:
         *
         * Refund provider is simulated.
         *
         * A real external refund should use
         * idempotency + compensation/outbox/Saga
         * rather than holding a DB transaction
         * during an external HTTP request.
         */
        payment.setStatus(
                PaymentStatus.REFUNDED
        );

        payment.setRefundReference(
                generateRefundReference()
        );

        payment.setRefundedAt(
                LocalDateTime.now()
        );

        return paymentRepository
                .saveAndFlush(payment);
    }

    private String generatePaymentReference() {

        return "PAY-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }

    private String generateRefundReference() {

        return "REF-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }
}