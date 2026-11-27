package com.sphwms.trip.internal.controller;

import com.sphwms.trip.internal.dto.requests.TripRequest;
import com.sphwms.trip.internal.dto.responses.TripResponse;
import com.sphwms.trip.internal.enums.TripStatus;
import com.sphwms.trip.internal.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<List<TripResponse>> getAll() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateStatus(@PathVariable Long id, @RequestParam TripStatus status) {
        return ResponseEntity.ok(tripService.updateStatus(id, status));
    }
}
