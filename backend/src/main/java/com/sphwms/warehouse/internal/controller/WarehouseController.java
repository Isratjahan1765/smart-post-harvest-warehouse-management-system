package com.sphwms.warehouse.internal.controller;

import com.sphwms.warehouse.internal.dto.requests.WarehouseRequest;
import com.sphwms.warehouse.internal.dto.responses.WarehouseOptionResponse;
import com.sphwms.warehouse.internal.dto.responses.WarehouseResponse;
import com.sphwms.warehouse.internal.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status) {
        String searchTerm = q != null ? q : search;
        return ResponseEntity.ok(warehouseService.getAllWarehouses(searchTerm, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> getNextCode() {
        return ResponseEntity.ok(warehouseService.getNextCode());
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest req) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WarehouseResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.toggleStatus(id));
    }

    @GetMapping("/options")
    public ResponseEntity<List<WarehouseOptionResponse>> getOptions(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(warehouseService.getWarehouseOptions(includeInactive));
    }
}
