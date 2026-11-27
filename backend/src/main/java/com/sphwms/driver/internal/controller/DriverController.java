package com.sphwms.driver.internal.controller;

import com.sphwms.driver.internal.dto.requests.DriverRequest;
import com.sphwms.driver.internal.dto.responses.DriverOptionResponse;
import com.sphwms.driver.internal.dto.responses.DriverResponse;
import com.sphwms.driver.internal.service.DriverService;
import com.sphwms.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<PageResponse<DriverResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(driverService.getAllDrivers(q, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.createDriver(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest req) {
        return ResponseEntity.ok(driverService.updateDriver(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.toggleStatus(id));
    }

    @GetMapping("/options")
    public ResponseEntity<List<DriverOptionResponse>> getOptions(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(driverService.getDriverOptions(includeInactive));
    }
}
