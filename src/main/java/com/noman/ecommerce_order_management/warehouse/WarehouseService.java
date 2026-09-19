package com.noman.ecommerce_order_management.warehouse;

import com.noman.ecommerce_order_management.warehouse.dto.WarehouseRequest;
import com.noman.ecommerce_order_management.warehouse.dto.WarehouseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponse createWarehouse(
            WarehouseRequest request
    ) {

        String warehouseCode =
                normalizeCode(request.getCode());

        warehouseRepository
                .findByCodeIgnoreCase(warehouseCode)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Warehouse code already exists"
                    );
                });

        Warehouse warehouse = Warehouse.builder()
                .code(warehouseCode)
                .name(request.getName().trim())
                .city(request.getCity().trim())
                .address(
                        trimToNull(request.getAddress())
                )
                .active(true)
                .build();

        return toResponse(
                warehouseRepository.save(warehouse)
        );
    }

    @Transactional
    public WarehouseResponse updateWarehouse(
            Long warehouseId,
            WarehouseRequest request
    ) {

        Warehouse warehouse =
                getWarehouse(warehouseId);

        String warehouseCode =
                normalizeCode(request.getCode());

        warehouseRepository
                .findByCodeIgnoreCase(warehouseCode)
                .filter(existing ->
                        !existing.getId().equals(warehouseId)
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Warehouse code already exists"
                    );
                });

        warehouse.setCode(warehouseCode);
        warehouse.setName(
                request.getName().trim()
        );
        warehouse.setCity(
                request.getCity().trim()
        );
        warehouse.setAddress(
                trimToNull(request.getAddress())
        );

        return toResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse updateWarehouseStatus(
            Long warehouseId,
            boolean active
    ) {

        Warehouse warehouse =
                getWarehouse(warehouseId);

        warehouse.setActive(active);

        return toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehouses() {

        return warehouseRepository
                .findAllByOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
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

    private WarehouseResponse toResponse(
            Warehouse warehouse
    ) {

        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .city(warehouse.getCity())
                .address(warehouse.getAddress())
                .active(warehouse.isActive())
                .build();
    }

    private String normalizeCode(
            String code
    ) {

        return code
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String trimToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}