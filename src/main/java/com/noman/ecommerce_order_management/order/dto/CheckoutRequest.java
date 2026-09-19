package com.noman.ecommerce_order_management.order.dto;

import com.noman.ecommerce_order_management.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @Size(
            max = 50,
            message = "Discount code cannot exceed 50 characters"
    )
    private String discountCode;

    @NotNull(
            message = "Payment method is required"
    )
    private PaymentMethod paymentMethod;

    @NotBlank(
            message = "Shipping address is required"
    )
    @Size(
            max = 500,
            message = "Shipping address cannot exceed 500 characters"
    )
    private String shippingAddress;
}