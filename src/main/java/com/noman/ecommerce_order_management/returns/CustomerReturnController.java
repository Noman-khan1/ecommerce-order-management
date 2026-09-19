package com.noman.ecommerce_order_management.returns;

import com.noman.ecommerce_order_management.returns.dto.ReturnOrderRequest;
import com.noman.ecommerce_order_management.returns.dto.ReturnResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final ReturnService returnService;

    @PostMapping("/{orderId}/return")
    public ResponseEntity<ReturnResponse> returnOrder(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid
            @RequestBody
            ReturnOrderRequest request
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        returnService.returnOrder(
                                authentication.getName(),
                                orderId,
                                request
                        )
                );
    }

    @GetMapping("/{orderId}/return")
    public ReturnResponse getReturn(
            Authentication authentication,
            @PathVariable Long orderId
    ) {

        return returnService.getReturn(
                authentication.getName(),
                orderId
        );
    }
}