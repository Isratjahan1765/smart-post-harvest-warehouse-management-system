package com.sphwms.arrival.internal.controller;

import com.sphwms.arrival.internal.dto.requests.ArrivalRequest;
import com.sphwms.arrival.internal.dto.requests.QualityCheckRequest;
import com.sphwms.arrival.internal.dto.requests.RejectRequest;
import com.sphwms.arrival.internal.dto.responses.ArrivalResponse;
import com.sphwms.arrival.internal.dto.responses.ArrivalSummaryResponse;
import com.sphwms.arrival.internal.service.ArrivalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arrivals")
@RequiredArgsConstructor
public class ArrivalController {

    private final ArrivalService arrivalService;

    @GetMapping
    public ResponseEntity<List<ArrivalResponse>> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(arrivalService.getAllArrivals(status));
    }

    @GetMapping("/summary")
    public ResponseEntity<ArrivalSummaryResponse> getSummary() {
        return ResponseEntity.ok(arrivalService.getSummary());
    }

    @GetMapping("/next-batch-no")
    public ResponseEntity<Map<String, String>> getNextBatchNo() {
        return ResponseEntity.ok(arrivalService.getNextBatchNo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArrivalResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(arrivalService.getArrivalById(id));
    }

    @PostMapping
    public ResponseEntity<ArrivalResponse> create(@Valid @RequestBody ArrivalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arrivalService.createArrival(req));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ArrivalResponse> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) QualityCheckRequest qcReq) {
        return ResponseEntity.ok(arrivalService.confirmArrival(id, qcReq));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ArrivalResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRequest req) {
        return ResponseEntity.ok(arrivalService.rejectArrival(id, req));
    }

    @PostMapping("/{id}/quality-check")
    public ResponseEntity<ArrivalResponse> qualityCheck(
            @PathVariable Long id,
            @RequestBody(required = false) QualityCheckRequest qcReq) {
        return ResponseEntity.ok(arrivalService.confirmArrival(id, qcReq));
    }
}
