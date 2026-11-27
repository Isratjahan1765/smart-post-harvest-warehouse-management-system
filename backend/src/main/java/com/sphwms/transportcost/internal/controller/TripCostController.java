package com.sphwms.transportcost.internal.controller;

import com.sphwms.shared.dto.PageResponse;
import com.sphwms.transportcost.internal.dto.requests.TripCostRequest;
import com.sphwms.transportcost.internal.dto.responses.AvailableTripResponse;
import com.sphwms.transportcost.internal.dto.responses.MonthlyTripCostResponse;
import com.sphwms.transportcost.internal.dto.responses.TripCostResponse;
import com.sphwms.transportcost.internal.service.TripCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip-costs")
@RequiredArgsConstructor
public class TripCostController {

    private final TripCostService tripCostService;

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyTripCostResponse>> getMonthly() {
        return ResponseEntity.ok(tripCostService.getMonthlyTransportCost());
    }

    @GetMapping
    public ResponseEntity<PageResponse<TripCostResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(tripCostService.getAllTripCosts(q, page, size));
    }

    @GetMapping("/available-trips")
    public ResponseEntity<List<AvailableTripResponse>> getAvailableTrips() {
        return ResponseEntity.ok(tripCostService.getAvailableTrips());
    }

    @PostMapping
    public ResponseEntity<TripCostResponse> create(@Valid @RequestBody TripCostRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripCostService.createTripCost(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripCostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TripCostRequest req) {
        return ResponseEntity.ok(tripCostService.updateTripCost(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripCostService.deleteTripCost(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q) {
        byte[] bytes = tripCostService.exportTripCostsToXlsx(q);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trip-costs.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
