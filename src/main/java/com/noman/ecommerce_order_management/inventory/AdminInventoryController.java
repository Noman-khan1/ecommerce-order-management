package com.noman.ecommerce_order_management.inventory;

import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.inventory.dto.InventoryStockUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody InventoryRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        inventoryService.createInventory(
                                request
                        )
                );
    }

    @PutMapping("/{inventoryId}/stock")
    public InventoryResponse updateAvailableStock(
            @PathVariable Long inventoryId,
            @Valid
            @RequestBody
            InventoryStockUpdateRequest request
    ) {

        return inventoryService
                .updateAvailableStock(
                        inventoryId,
                        request
                );
    }

    @GetMapping
    public List<InventoryResponse> getInventories(
            @RequestParam(required = false)
            Long skuId,

            @RequestParam(required = false)
            Long warehouseId
    ) {

        return inventoryService.getInventories(
                skuId,
                warehouseId
        );
    }
}