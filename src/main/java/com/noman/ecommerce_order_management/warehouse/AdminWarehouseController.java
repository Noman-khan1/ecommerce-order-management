package com.noman.ecommerce_order_management.warehouse;

import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouses")
@RequiredArgsConstructor
public class AdminWarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody WarehouseRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        warehouseService.createWarehouse(
                                request
                        )
                );
    }

    @PutMapping("/{warehouseId}")
    public WarehouseResponse updateWarehouse(
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseRequest request
    ) {

        return warehouseService.updateWarehouse(
                warehouseId,
                request
        );
    }

    @PatchMapping("/{warehouseId}/status")
    public WarehouseResponse updateWarehouseStatus(
            @PathVariable Long warehouseId,
            @RequestParam boolean active
    ) {

        return warehouseService
                .updateWarehouseStatus(
                        warehouseId,
                        active
                );
    }

    @GetMapping
    public List<WarehouseResponse> getWarehouses() {

        return warehouseService.getWarehouses();
    }
}