package com.noman.ecommerce_order_management.inventory;

import com.noman.ecommerce_order_management.catalog.CatalogService;
import com.noman.ecommerce_order_management.catalog.dto.CategoryRequest;
import com.noman.ecommerce_order_management.catalog.dto.CategoryResponse;
import com.noman.ecommerce_order_management.catalog.dto.ProductRequest;
import com.noman.ecommerce_order_management.catalog.dto.ProductResponse;
import com.noman.ecommerce_order_management.catalog.dto.SkuRequest;
import com.noman.ecommerce_order_management.catalog.dto.SkuResponse;
import com.noman.ecommerce_order_management.inventory.dto.InventoryRequest;
import com.noman.ecommerce_order_management.inventory.dto.InventoryResponse;
import com.noman.ecommerce_order_management.inventory.dto.InventoryStockUpdateRequest;
import com.noman.ecommerce_order_management.warehouse.WarehouseService;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class WarehouseInventoryServiceIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void shouldTrackSameSkuAcrossMultipleWarehouses() {

        CategoryResponse category =
                catalogService.createCategory(
                        new CategoryRequest(
                                "Electronics",
                                "Electronic products"
                        )
                );

        ProductResponse product =
                catalogService.createProduct(
                        new ProductRequest(
                                "Smartphone",
                                "Premium smartphone",
                                "ExampleBrand",
                                category.getId()
                        )
                );

        SkuResponse sku =
                catalogService.createSku(
                        product.getId(),
                        new SkuRequest(
                                "PHONE-BLK-128",
                                "Black / 128 GB",
                                new BigDecimal("59999.00")
                        )
                );

        WarehouseResponse bangaloreWarehouse =
                warehouseService.createWarehouse(
                        new WarehouseRequest(
                                "BLR-01",
                                "Bangalore Warehouse",
                                "Bangalore",
                                "Whitefield, Bangalore"
                        )
                );

        WarehouseResponse delhiWarehouse =
                warehouseService.createWarehouse(
                        new WarehouseRequest(
                                "DEL-01",
                                "Delhi Warehouse",
                                "Delhi",
                                "Delhi NCR"
                        )
                );

        InventoryResponse bangaloreInventory =
                inventoryService.createInventory(
                        new InventoryRequest(
                                sku.getId(),
                                bangaloreWarehouse.getId(),
                                5
                        )
                );

        InventoryResponse delhiInventory =
                inventoryService.createInventory(
                        new InventoryRequest(
                                sku.getId(),
                                delhiWarehouse.getId(),
                                3
                        )
                );

        assertNotNull(
                bangaloreInventory.getId()
        );

        assertNotNull(
                delhiInventory.getId()
        );

        List<InventoryResponse> inventories =
                inventoryService.getInventories(
                        sku.getId(),
                        null
                );

        assertEquals(
                2,
                inventories.size()
        );

        int totalAvailable =
                inventories
                        .stream()
                        .mapToInt(
                                InventoryResponse::getAvailableQuantity
                        )
                        .sum();

        assertEquals(
                8,
                totalAvailable
        );

        assertEquals(
                0,
                bangaloreInventory.getReservedQuantity()
        );

        InventoryResponse updatedInventory =
                inventoryService.updateAvailableStock(
                        bangaloreInventory.getId(),
                        new InventoryStockUpdateRequest(7)
                );

        assertEquals(
                7,
                updatedInventory.getAvailableQuantity()
        );

        assertEquals(
                7,
                updatedInventory.getTotalQuantity()
        );
    }
}