package com.sphwms.buyer.internal.service;

import com.sphwms.buyer.internal.dto.requests.BuyerRequest;
import com.sphwms.buyer.internal.dto.requests.DuePaymentRequest;
import com.sphwms.buyer.internal.dto.responses.BuyerResponse;
import com.sphwms.buyer.internal.dto.responses.BuyerSummaryResponse;
import com.sphwms.buyer.internal.entity.Buyer;
import com.sphwms.buyer.internal.entity.DuePayment;
import com.sphwms.buyer.internal.enums.BuyerStatus;
import com.sphwms.buyer.internal.repository.BuyerRepository;
import com.sphwms.buyer.internal.repository.DuePaymentRepository;
import com.sphwms.sale.internal.entity.PaymentAllocation;
import com.sphwms.sale.internal.entity.Sale;
import com.sphwms.sale.internal.enums.PaymentStatus;
import com.sphwms.sale.internal.enums.SaleStatus;
import com.sphwms.sale.internal.repository.PaymentAllocationRepository;
import com.sphwms.sale.internal.repository.SaleRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerService {

    private final BuyerRepository buyerRepository;
    private final SaleRepository saleRepository;
    private final DuePaymentRepository duePaymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BuyerResponse> getAllBuyers(String search) {
        List<Buyer> buyers = buyerRepository.findByStatusOrderByBuyerIdAsc(BuyerStatus.ACTIVE);
        List<Sale> allSales = saleRepository.findAll();

        Map<Long, List<Sale>> salesByBuyer = allSales.stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFIRMED)
                .collect(Collectors.groupingBy(Sale::getBuyerId));

        String searchLower = (search != null) ? search.trim().toLowerCase() : "";

        return buyers.stream()
                .filter(b -> {
                    if (searchLower.isEmpty()) return true;
                    return b.getName().toLowerCase().contains(searchLower) ||
                           b.getPhone().contains(searchLower);
                })
                .map(b -> toResponse(b, salesByBuyer.getOrDefault(b.getBuyerId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuyerResponse getBuyerById(Long id) {
        Buyer b = buyerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Buyer not found."));
        List<Sale> sales = saleRepository.findByBuyerIdOrderBySaleIdDesc(id).stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFIRMED)
                .collect(Collectors.toList());
        return toResponse(b, sales);
    }

    @Transactional(readOnly = true)
    public BuyerSummaryResponse getBuyerSummary(Long id) {
        Buyer b = buyerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Buyer not found."));
        List<Sale> sales = saleRepository.findByBuyerIdOrderBySaleIdDesc(id).stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFIRMED)
                .collect(Collectors.toList());

        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        LocalDate lastPurchase = null;

        for (Sale s : sales) {
            totalPurchase = totalPurchase.add(s.getTotalAmount());
            totalPaid = totalPaid.add(s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO);
            if (s.getSaleDate() != null) {
                if (lastPurchase == null || s.getSaleDate().isAfter(lastPurchase)) {
                    lastPurchase = s.getSaleDate();
                }
            }
        }

        BigDecimal totalDue = totalPurchase.subtract(totalPaid);
        if (totalDue.compareTo(BigDecimal.ZERO) < 0) totalDue = BigDecimal.ZERO;

        int paidPercentage = 0;
        if (totalPurchase.compareTo(BigDecimal.ZERO) > 0) {
            paidPercentage = totalPaid.multiply(BigDecimal.valueOf(100))
                    .divide(totalPurchase, 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        return new BuyerSummaryResponse(totalPurchase, totalPaid, totalDue, lastPurchase, paidPercentage);
    }

    @Transactional
    public BuyerResponse createBuyer(BuyerRequest req) {
        String phone = normalizePhone(req.phone());
        if (buyerRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("Phone number already exists.");
        }

        Buyer b = new Buyer();
        b.setName(req.name().trim());
        b.setPhone(phone);
        b.setAddress(req.address().trim());
        b.setBuyerType(req.buyerType());
        b.setStatus(BuyerStatus.ACTIVE);
        b.setCreatedBy(currentUserProvider.getCurrentUserId());
        b.setCreatedAt(LocalDateTime.now());

        Buyer saved = buyerRepository.save(b);
        auditService.log("BUYER_CREATE", "BUYER", saved.getBuyerId(),
                "Created buyer " + saved.getName());

        return toResponse(saved, Collections.emptyList());
    }

    @Transactional
    public BuyerResponse updateBuyer(Long id, BuyerRequest req) {
        Buyer b = buyerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Buyer not found."));

        String phone = normalizePhone(req.phone());
        if (buyerRepository.findByPhoneAndIdNot(phone, id).isPresent()) {
            throw new BusinessException("Phone number already exists.");
        }

        b.setName(req.name().trim());
        b.setPhone(phone);
        b.setAddress(req.address().trim());
        if (req.buyerType() != null) {
            b.setBuyerType(req.buyerType());
        }

        Buyer saved = buyerRepository.save(b);
        auditService.log("BUYER_UPDATE", "BUYER", saved.getBuyerId(),
                "Updated buyer " + saved.getName());

        List<Sale> sales = saleRepository.findByBuyerIdOrderBySaleIdDesc(id).stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFIRMED)
                .collect(Collectors.toList());
        return toResponse(saved, sales);
    }

    @Transactional
    public void deleteBuyer(Long id) {
        Buyer b = buyerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Buyer not found."));
        List<Sale> sales = saleRepository.findByBuyerIdOrderBySaleIdDesc(id);
        if (!sales.isEmpty()) {
            b.setStatus(BuyerStatus.INACTIVE);
            buyerRepository.save(b);
            auditService.log("BUYER_DEACTIVATE", "BUYER", b.getBuyerId(),
                    "Marked buyer inactive " + b.getName());
        } else {
            buyerRepository.delete(b);
            auditService.log("BUYER_DELETE", "BUYER", id,
                    "Deleted buyer " + b.getName());
        }
    }

    @Transactional
    public BuyerSummaryResponse recordPayment(Long buyerId, DuePaymentRequest req) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("Buyer not found."));

        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive.");
        }

        List<Sale> dueSales = saleRepository.findDueSalesByBuyerWithLock(buyerId);
        BigDecimal totalDue = BigDecimal.ZERO;
        for (Sale s : dueSales) {
            BigDecimal due = s.getTotalAmount().subtract(s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO);
            totalDue = totalDue.add(due);
        }

        if (req.amount().compareTo(totalDue) > 0) {
            throw new BusinessException("Payment amount exceeds due amount.");
        }

        LocalDate pDate = req.paymentDate() != null ? req.paymentDate() : LocalDate.now();

        DuePayment dp = new DuePayment();
        dp.setBuyerId(buyerId);
        dp.setAmount(req.amount());
        dp.setPaymentDate(pDate);
        dp.setReceivedBy(currentUserProvider.getCurrentUserId());
        DuePayment savedPayment = duePaymentRepository.save(dp);

        BigDecimal remainingToAllocate = req.amount();
        for (Sale s : dueSales) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal salePaid = s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal unpaid = s.getTotalAmount().subtract(salePaid);

            BigDecimal allocate = remainingToAllocate.min(unpaid);
            s.setPaidAmount(salePaid.add(allocate));
            if (s.getPaidAmount().compareTo(s.getTotalAmount()) >= 0) {
                s.setPaymentStatus(PaymentStatus.PAID);
            }
            saleRepository.save(s);

            PaymentAllocation pa = new PaymentAllocation();
            pa.setPaymentId(savedPayment.getPaymentId());
            pa.setSaleId(s.getSaleId());
            pa.setAmount(allocate);
            paymentAllocationRepository.save(pa);

            remainingToAllocate = remainingToAllocate.subtract(allocate);
        }

        auditService.log("DUE_PAYMENT", "BUYER", buyerId,
                "Recorded due payment of " + req.amount() + " for buyer " + buyer.getName());

        return getBuyerSummary(buyerId);
    }

    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Invalid phone number.");
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("880")) {
            digits = digits.substring(2);
        }
        if (digits.length() != 11 || !digits.startsWith("01")) {
            throw new BusinessException("Invalid phone number.");
        }
        return digits;
    }

    private BuyerResponse toResponse(Buyer b, List<Sale> sales) {
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        LocalDate lastPurchase = null;

        for (Sale s : sales) {
            totalSales = totalSales.add(s.getTotalAmount());
            totalPaid = totalPaid.add(s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO);
            if (s.getSaleDate() != null) {
                if (lastPurchase == null || s.getSaleDate().isAfter(lastPurchase)) {
                    lastPurchase = s.getSaleDate();
                }
            }
        }
        BigDecimal totalDue = totalSales.subtract(totalPaid);
        if (totalDue.compareTo(BigDecimal.ZERO) < 0) totalDue = BigDecimal.ZERO;

        return new BuyerResponse(
                b.getBuyerId(),
                b.getName(),
                b.getPhone(),
                b.getAddress(),
                b.getBuyerType(),
                b.getStatus().name(),
                totalSales,
                totalSales,
                totalPaid,
                totalDue,
                lastPurchase
        );
    }
}
