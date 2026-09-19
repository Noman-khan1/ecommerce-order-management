package com.noman.ecommerce_order_management.inventory;

import com.noman.ecommerce_order_management.catalog.Sku;
import com.noman.ecommerce_order_management.catalog.SkuRepository;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.inventory.dto.InventoryStockUpdateRequest;
import com.noman.ecommerce_order_management.warehouse.Warehouse;
import com.noman.ecommerce_order_management.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final SkuRepository skuRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public InventoryResponse createInventory(
            InventoryRequest request
    ) {

        validateQuantity(
                request.getAvailableQuantity()
        );

        Sku sku = getSku(
                request.getSkuId()
        );

        if (!sku.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot add inventory for inactive SKU"
            );
        }

        Warehouse warehouse =
                getWarehouse(
                        request.getWarehouseId()
                );

        if (!warehouse.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot add inventory to inactive warehouse"
            );
        }

        if (inventoryRepository
                .existsBySkuIdAndWarehouseId(
                        sku.getId(),
                        warehouse.getId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory already exists for this SKU and warehouse"
            );
        }

        Inventory inventory =
                Inventory.builder()
                        .sku(sku)
                        .warehouse(warehouse)
                        .availableQuantity(
                                request.getAvailableQuantity()
                        )
                        .reservedQuantity(0)
                        .build();

        return toResponse(
                inventoryRepository.save(inventory)
        );
    }

    @Transactional
    public InventoryResponse updateAvailableStock(
            Long inventoryId,
            InventoryStockUpdateRequest request
    ) {

        validateQuantity(
                request.getAvailableQuantity()
        );

        Inventory inventory =
                getInventory(inventoryId);

        inventory.setAvailableQuantity(
                request.getAvailableQuantity()
        );

        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(
            Long skuId,
            Long warehouseId
    ) {

        if (skuId != null) {
            getSku(skuId);
        }

        if (warehouseId != null) {
            getWarehouse(warehouseId);
        }

        List<Inventory> inventories;

        if (skuId != null && warehouseId != null) {

            inventories =
                    inventoryRepository
                            .findBySkuIdAndWarehouseId(
                                    skuId,
                                    warehouseId
                            )
                            .stream()
                            .toList();

        } else if (skuId != null) {

            inventories =
                    inventoryRepository
                            .findBySkuIdOrderByWarehouseIdAsc(
                                    skuId
                            );

        } else if (warehouseId != null) {

            inventories =
                    inventoryRepository
                            .findByWarehouseIdOrderBySkuIdAsc(
                                    warehouseId
                            );

        } else {

            inventories =
                    inventoryRepository
                            .findAllByOrderByIdAsc();
        }

        return inventories
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Inventory getInventory(
            Long inventoryId
    ) {

        return inventoryRepository
                .findById(inventoryId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Inventory not found"
                        )
                );
    }

    private Sku getSku(
            Long skuId
    ) {

        return skuRepository
                .findById(skuId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "SKU not found"
                        )
                );
    }

    private Warehouse getWarehouse(
            Long warehouseId
    ) {

        return warehouseRepository
                .findById(warehouseId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Warehouse not found"
                        )
                );
    }

    private void validateQuantity(
            Integer quantity
    ) {

        if (quantity == null || quantity < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Available quantity cannot be negative"
            );
        }
    }

    private InventoryResponse toResponse(
            Inventory inventory
    ) {

        int totalQuantity =
                inventory.getAvailableQuantity()
                        + inventory.getReservedQuantity();

        return InventoryResponse.builder()
                .id(inventory.getId())
                .skuId(
                        inventory.getSku().getId()
                )
                .skuCode(
                        inventory.getSku().getSkuCode()
                )
                .warehouseId(
                        inventory.getWarehouse().getId()
                )
                .warehouseCode(
                        inventory.getWarehouse().getCode()
                )
                .availableQuantity(
                        inventory.getAvailableQuantity()
                )
                .reservedQuantity(
                        inventory.getReservedQuantity()
                )
                .totalQuantity(totalQuantity)
                .build();
    }
}