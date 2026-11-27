package com.sphwms.donation.internal.controller;

import com.sphwms.donation.internal.dto.requests.DonationRequest;
import com.sphwms.donation.internal.dto.requests.ReceiverRequest;
import com.sphwms.donation.internal.dto.responses.*;
import com.sphwms.donation.internal.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    // Receivers endpoints
    @GetMapping("/receivers")
    public ResponseEntity<List<ReceiverResponse>> getReceivers(@RequestParam(defaultValue = "ACTIVE") String status) {
        return ResponseEntity.ok(donationService.getAllReceivers(status));
    }

    @PostMapping("/receivers")
    public ResponseEntity<ReceiverResponse> createReceiver(@Valid @RequestBody ReceiverRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createReceiver(req));
    }

    @PutMapping("/receivers/{id}")
    public ResponseEntity<ReceiverResponse> updateReceiver(@PathVariable Long id, @Valid @RequestBody ReceiverRequest req) {
        return ResponseEntity.ok(donationService.updateReceiver(id, req));
    }

    // Donation endpoints
    @GetMapping("/donations/eligible")
    public ResponseEntity<List<EligibleDonationResponse>> getEligible(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(donationService.getEligibleBatches(search));
    }

    @GetMapping("/donations")
    public ResponseEntity<List<DonationResponse>> getAllDonations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(donationService.getAllDonations(search, receiverId, from, to));
    }

    @PostMapping("/donations")
    public ResponseEntity<DonationResponse> createDonation(@Valid @RequestBody DonationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createDonation(req));
    }

    @GetMapping("/donations/summary/by-receiver")
    public ResponseEntity<ReceiverSummaryResponse> getSummaryByReceiver() {
        return ResponseEntity.ok(donationService.getSummaryByReceiver());
    }

    @PostMapping("/donations/{id}/cancel")
    public ResponseEntity<DonationResponse> cancelDonation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(donationService.cancelDonation(id, reason));
    }

    @GetMapping("/donations/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        byte[] pdf = donationService.generateReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"donation-receipt-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/donations/export")
    public ResponseEntity<byte[]> exportDonations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] bytes = donationService.exportDonationsToXlsx(search, receiverId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"donation-history.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
