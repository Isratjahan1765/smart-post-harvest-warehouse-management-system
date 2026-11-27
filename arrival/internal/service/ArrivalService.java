package com.sphwms.arrival.internal.service;

import com.sphwms.arrival.internal.dto.requests.ArrivalRequest;
import com.sphwms.arrival.internal.dto.requests.QualityCheckRequest;
import com.sphwms.arrival.internal.dto.requests.RejectRequest;
import com.sphwms.arrival.internal.dto.responses.ArrivalResponse;
import com.sphwms.arrival.internal.dto.responses.ArrivalSummaryResponse;
import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.entity.QualityCheck;
import com.sphwms.arrival.internal.enums.ArrivalStatus;
import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.arrival.internal.repository.QualityCheckRepository;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.repository.ProductRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import com.sphwms.shared.sequence.IdSequenceService;
import com.sphwms.stock.internal.entity.StockBatch;
import com.sphwms.stock.internal.entity.StockTransaction;
import com.sphwms.stock.internal.enums.StockTxnType;
import com.sphwms.stock.internal.repository.StockBatchRepository;
import com.sphwms.stock.internal.repository.StockTransactionRepository;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArrivalService {

    private final ArrivalRepository arrivalRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final IdSequenceService idSequenceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ArrivalResponse> getAllArrivals(String status) {
        List<Arrival> list;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                ArrivalStatus st = ArrivalStatus.valueOf(status.trim().toUpperCase());
                list = arrivalRepository.findByStatusOrderByArrivalIdDesc(st);
            } catch (IllegalArgumentException ex) {
                list = arrivalRepository.findAllByOrderByArrivalIdDesc();
            }
        } else {
            list = arrivalRepository.findAllByOrderByArrivalIdDesc();
        }

        Map<Long, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (a, b) -> a));
        Map<Long, QualityCheck> qcMap = qualityCheckRepository.findAll().stream()
                .collect(Collectors.toMap(QualityCheck::getArrivalId, q -> q, (a, b) -> a));

        return list.stream()
                .map(a -> toResponse(a, productNames, warehouseNames, qcMap.get(a.getArrivalId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ArrivalSummaryResponse getSummary() {
        long pending = arrivalRepository.countByStatus(ArrivalStatus.PENDING);
        long confirmed = arrivalRepository.countByStatus(ArrivalStatus.CONFIRMED);
        long rejected = arrivalRepository.countByStatus(ArrivalStatus.REJECTED);
        return new ArrivalSummaryResponse(pending + confirmed + rejected, pending, confirmed, rejected);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getNextBatchNo() {
        return Map.of("batchNo", idSequenceService.peekBatchNo(LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public ArrivalResponse getArrivalById(Long id) {
        Arrival a = arrivalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Arrival not found."));
        String prodName = productRepository.findById(a.getProductId()).map(Product::getName).orElse("Unknown Product");
        String whName = warehouseRepository.findById(a.getWarehouseId()).map(Warehouse::getName).orElse("Unknown Warehouse");
        QualityCheck qc = qualityCheckRepository.findByArrivalId(id).orElse(null);
        return toResponse(a, Map.of(a.getProductId(), prodName), Map.of(a.getWarehouseId(), whName), qc);
    }

    @Transactional
    public ArrivalResponse createArrival(ArrivalRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException("Product not found."));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException("Cannot create arrival for inactive product.");
        }

        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found."));
        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new BusinessException("Cannot create arrival for inactive warehouse.");
        }

        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid quantity - enter a positive number.");
        }

        if (req.expiryDate() == null || !req.expiryDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Expiry date must be in the future.");
        }

        BigDecimal purchasePrice = product.getPurchasePrice();
        BigDecimal totalCost = req.quantity().multiply(purchasePrice);

        String batchNo = idSequenceService.nextBatchNo(LocalDate.now());

        Arrival a = new Arrival();
        a.setBatchNo(batchNo);
        a.setProductId(product.getProductId());
        a.setWarehouseId(warehouse.getWarehouseId());
        a.setQuantity(req.quantity());
        a.setGrade(req.grade());
        a.setExpiryDate(req.expiryDate());
        a.setPurchasePrice(purchasePrice);
        a.setTotalCost(totalCost);
        a.setStatus(ArrivalStatus.PENDING);
        a.setCreatedBy(currentUserProvider.getCurrentUserId());
        a.setCreatedAt(LocalDateTime.now());

        Arrival saved = arrivalRepository.save(a);
        auditService.log("ARRIVAL_CREATE", "ARRIVAL", saved.getArrivalId(),
                "Created pending arrival " + saved.getBatchNo());

        return getArrivalById(saved.getArrivalId());
    }

    @Transactional
    public ArrivalResponse confirmArrival(Long id, QualityCheckRequest qcReq) {
        Arrival a = arrivalRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Arrival not found."));

        if (a.getStatus() != ArrivalStatus.PENDING) {
            throw new BusinessException("Arrival already confirmed.");
        }

        // Validate QC flags if passed
        boolean pkgOk = true;
        boolean dmgOk = true;
        boolean qtyOk = true;
        boolean expOk = true;
        boolean grdOk = true;

        if (qcReq != null) {
            pkgOk = Boolean.TRUE.equals(qcReq.packagingOk());
            dmgOk = Boolean.TRUE.equals(qcReq.noDamage());
            qtyOk = Boolean.TRUE.equals(qcReq.quantityMatch());
            expOk = Boolean.TRUE.equals(qcReq.expiryValid());
            grdOk = Boolean.TRUE.equals(qcReq.gradeMatch());

            if (!pkgOk || !dmgOk || !qtyOk || !expOk || !grdOk) {
                throw new BusinessException("Arrival data is invalid - cannot confirm.");
            }
        }

        Warehouse warehouse = warehouseRepository.findByIdWithLock(a.getWarehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found."));
        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new BusinessException("Cannot confirm arrival into inactive warehouse.");
        }

        // Check warehouse capacity
        List<Object[]> rows = stockBatchRepository.findStockSummaryByWarehouse();
        BigDecimal currentStock = BigDecimal.ZERO;
        for (Object[] r : rows) {
            if (a.getWarehouseId().equals(r[0])) {
                currentStock = (BigDecimal) r[1];
                break;
            }
        }
        if (currentStock.add(a.getQuantity()).compareTo(warehouse.getCapacityKg()) > 0) {
            throw new BusinessException("Warehouse capacity exceeded.");
        }

        QualityCheck qc = new QualityCheck();
        qc.setArrivalId(a.getArrivalId());
        qc.setPackagingOk(pkgOk);
        qc.setNoDamage(dmgOk);
        qc.setQuantityMatch(qtyOk);
        qc.setExpiryValid(expOk);
        qc.setGradeMatch(grdOk);
        qc.setCheckedBy(currentUserProvider.getCurrentUserId());
        qc.setCheckedAt(LocalDateTime.now());
        qualityCheckRepository.save(qc);

        a.setStatus(ArrivalStatus.CONFIRMED);
        a.setConfirmedBy(currentUserProvider.getCurrentUserId());
        a.setConfirmedAt(LocalDateTime.now());
        Arrival savedArrival = arrivalRepository.save(a);

        StockBatch batch = new StockBatch();
        batch.setArrivalId(savedArrival.getArrivalId());
        batch.setProductId(savedArrival.getProductId());
        batch.setWarehouseId(savedArrival.getWarehouseId());
        batch.setInitialQuantity(savedArrival.getQuantity());
        batch.setRemainingQuantity(savedArrival.getQuantity());
        batch.setUnitCost(savedArrival.getPurchasePrice());
        batch.setEntryDate(savedArrival.getCreatedAt().toLocalDate());
        batch.setExpiryDate(savedArrival.getExpiryDate());
        batch.setCreatedAt(LocalDateTime.now());
        StockBatch savedBatch = stockBatchRepository.save(batch);

        StockTransaction st = new StockTransaction();
        st.setProductId(savedArrival.getProductId());
        st.setBatchId(savedBatch.getBatchId());
        st.setWarehouseId(savedArrival.getWarehouseId());
        st.setTxnType(StockTxnType.ARRIVAL);
        st.setQuantity(savedArrival.getQuantity());
        st.setRefType("ARRIVAL");
        st.setRefId(savedArrival.getArrivalId());
        st.setTxnDate(LocalDateTime.now());
        st.setUserId(currentUserProvider.getCurrentUserId());
        stockTransactionRepository.save(st);

        auditService.log("ARRIVAL_CONFIRM", "ARRIVAL", savedArrival.getArrivalId(),
                "Confirmed arrival " + savedArrival.getBatchNo());

        return getArrivalById(savedArrival.getArrivalId());
    }

    @Transactional
    public ArrivalResponse rejectArrival(Long id, RejectRequest req) {
        Arrival a = arrivalRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Arrival not found."));

        if (a.getStatus() != ArrivalStatus.PENDING) {
            throw new BusinessException("Arrival cannot be rejected.");
        }

        if (req == null || req.reason() == null || req.reason().isBlank()) {
            throw new BusinessException("Rejection reason is required.");
        }

        QualityCheck qc = new QualityCheck();
        qc.setArrivalId(a.getArrivalId());
        qc.setPackagingOk(false);
        qc.setNoDamage(false);
        qc.setQuantityMatch(false);
        qc.setExpiryValid(false);
        qc.setGradeMatch(false);
        qc.setCheckedBy(currentUserProvider.getCurrentUserId());
        qc.setCheckedAt(LocalDateTime.now());
        qualityCheckRepository.save(qc);

        a.setStatus(ArrivalStatus.REJECTED);
        a.setRejectionReason(req.reason().trim());
        Arrival saved = arrivalRepository.save(a);

        auditService.log("ARRIVAL_REJECT", "ARRIVAL", saved.getArrivalId(),
                "Rejected arrival " + saved.getBatchNo() + ": " + saved.getRejectionReason());

        return getArrivalById(saved.getArrivalId());
    }

    private ArrivalResponse toResponse(Arrival a, Map<Long, String> prodNames, Map<Long, String> whNames, QualityCheck qc) {
        ArrivalResponse.QualityCheckResponse qcResp = null;
        if (qc != null) {
            qcResp = new ArrivalResponse.QualityCheckResponse(
                    qc.getPackagingOk(),
                    qc.getNoDamage(),
                    qc.getQuantityMatch(),
                    qc.getExpiryValid(),
                    qc.getGradeMatch(),
                    qc.getCheckedAt()
            );
        }

        return new ArrivalResponse(
                a.getArrivalId(),
                a.getBatchNo(),
                a.getProductId(),
                prodNames.getOrDefault(a.getProductId(), "Product #" + a.getProductId()),
                a.getWarehouseId(),
                whNames.getOrDefault(a.getWarehouseId(), "Warehouse #" + a.getWarehouseId()),
                a.getQuantity(),
                a.getGrade(),
                a.getExpiryDate(),
                a.getPurchasePrice(),
                a.getTotalCost(),
                a.getStatus(),
                a.getRejectionReason(),
                a.getCreatedAt(),
                a.getConfirmedAt(),
                qcResp
        );
    }
}
