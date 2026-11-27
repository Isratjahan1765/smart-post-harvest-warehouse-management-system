package com.sphwms.buyer.internal.controller;

import com.sphwms.buyer.internal.dto.requests.BuyerRequest;
import com.sphwms.buyer.internal.dto.requests.DuePaymentRequest;
import com.sphwms.buyer.internal.dto.responses.BuyerResponse;
import com.sphwms.buyer.internal.dto.responses.BuyerSummaryResponse;
import com.sphwms.buyer.internal.service.BuyerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buyers")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerService buyerService;

    @GetMapping
    public ResponseEntity<List<BuyerResponse>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(buyerService.getAllBuyers(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuyerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(buyerService.getBuyerById(id));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<BuyerSummaryResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(buyerService.getBuyerSummary(id));
    }

    @PostMapping
    public ResponseEntity<BuyerResponse> create(@Valid @RequestBody BuyerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(buyerService.createBuyer(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuyerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BuyerRequest req) {
        return ResponseEntity.ok(buyerService.updateBuyer(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buyerService.deleteBuyer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<BuyerSummaryResponse> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody DuePaymentRequest req) {
        return ResponseEntity.ok(buyerService.recordPayment(id, req));
    }
}
