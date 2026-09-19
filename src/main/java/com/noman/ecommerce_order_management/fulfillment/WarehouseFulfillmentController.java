package com.noman.ecommerce_order_management.fulfillment;

import com.noman.ecommerce_order_management.fulfillment.dto.UpdateOrderStatusRequest;
import com.noman.ecommerce_order_management.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/orders")
@RequiredArgsConstructor
public class WarehouseFulfillmentController {

    private final FulfillmentService fulfillmentService;

    @GetMapping
    public List<OrderResponse> getOrdersForFulfillment() {

        return fulfillmentService
                .getOrdersForFulfillment();
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid
            @RequestBody
            UpdateOrderStatusRequest request
    ) {

        return fulfillmentService
                .updateOrderStatus(
                        orderId,
                        request
                );
    }
}