package com.sphwms.warehouse.internal.service;

import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import com.sphwms.shared.sequence.IdSequenceService;
import com.sphwms.stock.internal.repository.StockBatchRepository;
import com.sphwms.warehouse.internal.dto.requests.WarehouseRequest;
import com.sphwms.warehouse.internal.dto.responses.WarehouseOptionResponse;
import com.sphwms.warehouse.internal.dto.responses.WarehouseResponse;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final IdSequenceService idSequenceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses(String q, String status) {
        List<Warehouse> list = warehouseRepository.findAllByOrderByCodeNumberAsc();

        // Stock summary per warehouse: warehouseId -> [usedKg, stockAmount]
        Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();

        String query = (q != null) ? q.trim().toLowerCase() : "";
        String statusFilter = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";

        return list.stream()
                .filter(w -> {
                    if ("ACTIVE".equals(statusFilter)) return Boolean.TRUE.equals(w.getActive());
                    if ("INACTIVE".equals(statusFilter)) return Boolean.FALSE.equals(w.getActive());
                    return true;
                })
                .filter(w -> {
                    if (query.isEmpty()) return true;
                    return (w.getName() != null && w.getName().toLowerCase().contains(query)) ||
                           (w.getCode() != null && w.getCode().toLowerCase().contains(query)) ||
                           (w.getLocation() != null && w.getLocation().toLowerCase().contains(query));
                })
                .map(w -> toResponse(w, stockSummary))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found."));
        Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();
        return toResponse(w, stockSummary);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getNextCode() {
        return Map.of("code", idSequenceService.peekWarehouseCode());
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest req) {
        String name = req.name() != null ? req.name().trim().replaceAll("\\s+", " ") : "";
        if (warehouseRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new BusinessException("Warehouse name already exists.");
        }

        IdSequenceService.WarehouseCodeResult codeResult = idSequenceService.nextWarehouseCode();

        Warehouse w = new Warehouse();
        w.setCodeNumber(codeResult.codeNumber());
        w.setCode(codeResult.code());
        w.setName(name);
        w.setLocation(req.location() != null ? req.location().trim() : "");
        String inCharge = (req.inCharge() != null && !req.inCharge().isBlank())
                ? req.inCharge().trim() : "Warehouse In-charge";
        w.setInCharge(inCharge);
        w.setCapacityKg(req.capacityKg());
        w.setActive(req.status() == null || !"INACTIVE".equalsIgnoreCase(req.status()));
        w.setCreatedBy(currentUserProvider.getCurrentUserId());
        w.setCreatedAt(LocalDateTime.now());

        Warehouse saved = warehouseRepository.save(w);
        auditService.log("WAREHOUSE_CREATE", "WAREHOUSE", saved.getWarehouseId(),
                "Created warehouse " + saved.getCode() + " (" + saved.getName() + ")");

        Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();
        return toResponse(saved, stockSummary);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest req) {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found."));

        String name = req.name() != null ? req.name().trim().replaceAll("\\s+", " ") : "";
        if (warehouseRepository.findByNameIgnoreCaseAndIdNot(name, id).isPresent()) {
            throw new BusinessException("Warehouse name already exists.");
        }

        Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();
        BigDecimal currentStock = stockSummary.getOrDefault(id, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0];
        if (req.capacityKg().compareTo(currentStock) < 0) {
            throw new BusinessException("Capacity cannot be less than current stock (" + currentStock + " kg).");
        }

        w.setName(name);
        w.setLocation(req.location() != null ? req.location().trim() : "");
        if (req.inCharge() != null && !req.inCharge().isBlank()) {
            w.setInCharge(req.inCharge().trim());
        }
        w.setCapacityKg(req.capacityKg());
        w.setUpdatedBy(currentUserProvider.getCurrentUserId());
        w.setUpdatedAt(LocalDateTime.now());

        Warehouse saved = warehouseRepository.save(w);
        auditService.log("WAREHOUSE_UPDATE", "WAREHOUSE", saved.getWarehouseId(),
                "Updated warehouse " + saved.getCode());

        return toResponse(saved, stockSummary);
    }

    @Transactional
    public WarehouseResponse toggleStatus(Long id) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found."));

        boolean currentlyActive = Boolean.TRUE.equals(w.getActive());
        if (currentlyActive) {
            // Check stock is 0
            Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();
            BigDecimal currentStock = stockSummary.getOrDefault(id, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0];
            if (currentStock.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("Cannot deactivate: warehouse still has stock.");
            }

            // Check pending arrivals
            Long pendingArrivals = (Long) entityManager.createQuery(
                    "SELECT COUNT(a) FROM Arrival a WHERE a.warehouseId = :whId AND a.status = 'PENDING'")
                    .setParameter("whId", id)
                    .getSingleResult();
            if (pendingArrivals != null && pendingArrivals > 0) {
                throw new BusinessException("Cannot deactivate: pending arrivals exist.");
            }

            w.setActive(false);
        } else {
            w.setActive(true);
        }

        w.setUpdatedBy(currentUserProvider.getCurrentUserId());
        w.setUpdatedAt(LocalDateTime.now());
        Warehouse saved = warehouseRepository.save(w);
        auditService.log("WAREHOUSE_STATUS", "WAREHOUSE", saved.getWarehouseId(),
                "Set warehouse " + saved.getCode() + " active=" + saved.getActive());

        Map<Long, BigDecimal[]> stockSummary = getStockSummaryMap();
        return toResponse(saved, stockSummary);
    }

    @Transactional(readOnly = true)
    public List<WarehouseOptionResponse> getWarehouseOptions(boolean includeInactive) {
        List<Warehouse> list = includeInactive
                ? warehouseRepository.findAllByOrderByCodeNumberAsc()
                : warehouseRepository.findByActiveTrueOrderByCodeNumberAsc();

        return list.stream()
                .map(w -> new WarehouseOptionResponse(w.getWarehouseId(), w.getCode(), w.getName(), Boolean.TRUE.equals(w.getActive())))
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal[]> getStockSummaryMap() {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        List<Object[]> rows = stockBatchRepository.findStockSummaryByWarehouse();
        for (Object[] r : rows) {
            Long whId = (Long) r[0];
            BigDecimal qty = (BigDecimal) r[1];
            BigDecimal amt = (BigDecimal) r[2];
            map.put(whId, new BigDecimal[]{
                    qty != null ? qty : BigDecimal.ZERO,
                    amt != null ? amt : BigDecimal.ZERO
            });
        }
        return map;
    }

    private WarehouseResponse toResponse(Warehouse w, Map<Long, BigDecimal[]> stockSummary) {
        BigDecimal[] stock = stockSummary.getOrDefault(w.getWarehouseId(),
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal usedKg = stock[0];
        BigDecimal stockAmount = stock[1];

        BigDecimal capacity = w.getCapacityKg() != null ? w.getCapacityKg() : BigDecimal.ZERO;
        BigDecimal freeKg = capacity.subtract(usedKg);
        if (freeKg.compareTo(BigDecimal.ZERO) < 0) freeKg = BigDecimal.ZERO;

        int usedPercent = 0;
        if (capacity.compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = usedKg.multiply(BigDecimal.valueOf(100))
                    .divide(capacity, 0, RoundingMode.FLOOR)
                    .intValue();
        }

        String status = Boolean.TRUE.equals(w.getActive()) ? "ACTIVE" : "INACTIVE";

        return new WarehouseResponse(
                w.getWarehouseId(),
                w.getCodeNumber(),
                w.getCode(),
                w.getName(),
                w.getLocation(),
                w.getInCharge(),
                w.getCapacityKg(),
                usedKg,
                freeKg,
                usedPercent,
                stockAmount,
                status,
                usedKg, // totalStock
                Collections.emptyList() // stockBreakdown
        );
    }
}
