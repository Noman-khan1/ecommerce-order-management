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

        Sku sku =
                getSku(
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
                        .sku(
                                sku
                        )
                        .warehouse(
                                warehouse
                        )
                        .availableQuantity(
                                request.getAvailableQuantity()
                        )
                        .reservedQuantity(
                                0
                        )
                        .build();

        return toResponse(
                inventoryRepository.save(
                        inventory
                )
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
                getInventory(
                        inventoryId
                );

        inventory.setAvailableQuantity(
                request.getAvailableQuantity()
        );

        return toResponse(
                inventory
        );
    }

    /*
     * Used during checkout.
     *
     * InventoryRepository loads rows using
     * PESSIMISTIC_WRITE so concurrent customers
     * cannot oversell the same SKU.
     */
    @Transactional
    public Inventory reserveInventory(
            Long skuId,
            int quantity
    ) {

        if (quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation quantity must be greater than zero"
            );
        }

        Sku sku =
                getSku(
                        skuId
                );

        if (!sku.isActive()
                || !sku.getProduct()
                .isActive()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SKU is not available for purchase"
            );
        }

        List<Inventory> inventories =
                inventoryRepository
                        .findAllBySkuIdOrderByWarehouseIdAsc(
                                skuId
                        );

        Inventory inventory =
                inventories
                        .stream()
                        .filter(candidate ->
                                candidate
                                        .getWarehouse()
                                        .isActive()
                        )
                        .filter(candidate ->
                                candidate
                                        .getAvailableQuantity()
                                        >= quantity
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Insufficient inventory for SKU "
                                                + sku.getSkuCode()
                                )
                        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity()
                        - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        + quantity
        );

        return inventory;
    }

    /*
     * Used when an order changes:
     *
     * PACKED → SHIPPED
     *
     * During checkout:
     *
     * available -= quantity
     * reserved  += quantity
     *
     * During shipping:
     *
     * reserved -= quantity
     *
     * Available stock does NOT increase because
     * the item has physically left the warehouse.
     */
    @Transactional
    public void consumeReservedInventory(
            Long skuId,
            Long warehouseId,
            int quantity
    ) {

        if (quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fulfillment quantity must be greater than zero"
            );
        }

        Inventory inventory =
                inventoryRepository
                        .findBySkuAndWarehouseForUpdate(
                                skuId,
                                warehouseId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Inventory not found for order item"
                                )
                        );

        if (inventory.getReservedQuantity()
                < quantity) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reserved inventory is insufficient for fulfillment"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        - quantity
        );
    }

    /*
     * Used during customer return.
     *
     * The order has already been shipped and delivered,
     * therefore its quantity is no longer in reserved stock.
     *
     * For this assignment returned goods are assumed to
     * be sellable immediately, so we add them back to
     * available inventory in the ORIGINAL warehouse.
     */
    @Transactional
    public void restockReturnedInventory(
            Long skuId,
            Long warehouseId,
            int quantity
    ) {

        if (quantity <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Return quantity must be greater than zero"
            );
        }

        Inventory inventory =
                inventoryRepository
                        .findBySkuAndWarehouseForUpdate(
                                skuId,
                                warehouseId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Inventory not found for returned order item"
                                )
                        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity()
                        + quantity
        );
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(
            Long skuId,
            Long warehouseId
    ) {

        if (skuId != null) {

            getSku(
                    skuId
            );
        }

        if (warehouseId != null) {

            getWarehouse(
                    warehouseId
            );
        }

        List<Inventory> inventories;

        if (skuId != null
                && warehouseId != null) {

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
                .map(
                        this::toResponse
                )
                .toList();
    }

    private Inventory getInventory(
            Long inventoryId
    ) {

        return inventoryRepository
                .findById(
                        inventoryId
                )
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
                .findById(
                        skuId
                )
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
                .findById(
                        warehouseId
                )
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

        if (quantity == null
                || quantity < 0) {

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
                .id(
                        inventory.getId()
                )
                .skuId(
                        inventory.getSku()
                                .getId()
                )
                .skuCode(
                        inventory.getSku()
                                .getSkuCode()
                )
                .warehouseId(
                        inventory.getWarehouse()
                                .getId()
                )
                .warehouseCode(
                        inventory.getWarehouse()
                                .getCode()
                )
                .availableQuantity(
                        inventory.getAvailableQuantity()
                )
                .reservedQuantity(
                        inventory.getReservedQuantity()
                )
                .totalQuantity(
                        totalQuantity
                )
                .build();
    }
}