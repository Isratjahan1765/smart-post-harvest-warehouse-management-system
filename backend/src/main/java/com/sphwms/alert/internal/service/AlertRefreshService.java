package com.sphwms.alert.internal.service;

import com.sphwms.alert.internal.entity.Alert;
import com.sphwms.alert.internal.entity.AlertSetting;
import com.sphwms.alert.internal.enums.AlertEntityType;
import com.sphwms.alert.internal.enums.AlertType;
import com.sphwms.alert.internal.repository.AlertRepository;
import com.sphwms.alert.internal.repository.AlertSettingRepository;
import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.buyer.internal.entity.Buyer;
import com.sphwms.buyer.internal.enums.BuyerStatus;
import com.sphwms.buyer.internal.repository.BuyerRepository;
import com.sphwms.driver.internal.entity.Driver;
import com.sphwms.driver.internal.repository.DriverRepository;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.repository.ProductRepository;
import com.sphwms.sale.internal.entity.Sale;
import com.sphwms.sale.internal.enums.PaymentStatus;
import com.sphwms.sale.internal.enums.SaleStatus;
import com.sphwms.sale.internal.repository.SaleRepository;
import com.sphwms.stock.internal.entity.StockBatch;
import com.sphwms.stock.internal.repository.StockBatchRepository;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRefreshService {

    private final AlertRepository alertRepository;
    private final AlertSettingRepository alertSettingRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ArrivalRepository arrivalRepository;
    private final BuyerRepository buyerRepository;
    private final SaleRepository saleRepository;
    private final DriverRepository driverRepository;

    @PostConstruct
    public void init() {
        try {
            refreshAllAlerts();
        } catch (Exception e) {
            log.error("Failed to run initial alert refresh", e);
        }
    }

    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void refreshAllAlerts() {
        AlertSetting settings = alertSettingRepository.findById(1).orElseGet(() -> {
            AlertSetting s = new AlertSetting();
            s.setSettingId(1);
            return alertSettingRepository.save(s);
        });

        LocalDate today = LocalDate.now();
        Map<String, AlertCandidate> candidates = new HashMap<>();

        // 1. LOW_STOCK
        if (Boolean.TRUE.equals(settings.getLowStockEnabled())) {
            List<Product> products = productRepository.findByActiveTrueOrderByProductIdAsc();
            List<Warehouse> warehouses = warehouseRepository.findByActiveTrueOrderByCodeNumberAsc();
            List<StockBatch> allBatches = stockBatchRepository.findAll();

            Map<String, BigDecimal> stockByProdWh = new HashMap<>();
            for (StockBatch b : allBatches) {
                if (b.getRemainingQuantity() != null && b.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    String key = b.getProductId() + ":" + b.getWarehouseId();
                    stockByProdWh.merge(key, b.getRemainingQuantity(), BigDecimal::add);
                }
            }

            for (Product p : products) {
                for (Warehouse w : warehouses) {
                    String key = p.getProductId() + ":" + w.getWarehouseId();
                    BigDecimal stock = stockByProdWh.getOrDefault(key, BigDecimal.ZERO);
                    BigDecimal reorder = p.getReorderLevel() != null ? p.getReorderLevel() : BigDecimal.ZERO;
                    if (stock.compareTo(reorder) <= 0) {
                        String dedupeKey = "LOW_STOCK:" + p.getProductId() + ":" + w.getWarehouseId();
                        String details = stock.stripTrailingZeros().toPlainString() + " kg <= reorder level " +
                                reorder.stripTrailingZeros().toPlainString() + " kg in " + w.getName();
                        candidates.put(dedupeKey, new AlertCandidate(
                                AlertType.LOW_STOCK,
                                AlertEntityType.PRODUCT,
                                p.getProductId(),
                                w.getWarehouseId(),
                                p.getName(),
                                details,
                                dedupeKey
                        ));
                    }
                }
            }
        }

        // 2. NEAR_EXPIRY
        if (Boolean.TRUE.equals(settings.getNearExpiryEnabled())) {
            int daysThreshold = settings.getNearExpiryDays() != null ? settings.getNearExpiryDays() : 7;
            List<StockBatch> batches = stockBatchRepository.findAll();
            Map<Long, Product> prodMap = productRepository.findAll().stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
            Map<Long, String> batchNoMap = arrivalRepository.findAll().stream()
                    .collect(Collectors.toMap(Arrival::getArrivalId, Arrival::getBatchNo, (a, b) -> a));

            for (StockBatch b : batches) {
                if (b.getRemainingQuantity() != null && b.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 && b.getExpiryDate() != null) {
                    long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());
                    if (days <= daysThreshold) {
                        String dedupeKey = "NEAR_EXPIRY:" + b.getBatchId();
                        String bNo = batchNoMap.getOrDefault(b.getArrivalId(), "Batch #" + b.getBatchId());
                        Product p = prodMap.get(b.getProductId());
                        String pName = p != null ? p.getName() : "Product";
                        String details;
                        if (days < 0) {
                            details = "Expired on " + b.getExpiryDate() + " (" + b.getRemainingQuantity().stripTrailingZeros().toPlainString() + " kg left)";
                        } else if (days == 0) {
                            details = "Expires today (" + b.getRemainingQuantity().stripTrailingZeros().toPlainString() + " kg left)";
                        } else {
                            details = "Expires in " + days + " days (" + b.getRemainingQuantity().stripTrailingZeros().toPlainString() + " kg left)";
                        }

                        candidates.put(dedupeKey, new AlertCandidate(
                                AlertType.NEAR_EXPIRY,
                                AlertEntityType.BATCH,
                                b.getBatchId(),
                                b.getWarehouseId(),
                                pName + " (" + bNo + ")",
                                details,
                                dedupeKey
                        ));
                    }
                }
            }
        }

        // 3. DUE_PAYMENT
        if (Boolean.TRUE.equals(settings.getDueEnabled())) {
            List<Buyer> buyers = buyerRepository.findByStatusOrderByBuyerIdAsc(BuyerStatus.ACTIVE);
            List<Sale> dueSales = saleRepository.findAll().stream()
                    .filter(s -> s.getStatus() == SaleStatus.CONFIRMED && s.getPaymentStatus() == PaymentStatus.DUE)
                    .collect(Collectors.toList());

            Map<Long, BigDecimal> dueByBuyer = new HashMap<>();
            for (Sale s : dueSales) {
                BigDecimal paid = s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal due = s.getTotalAmount().subtract(paid);
                if (due.compareTo(BigDecimal.ZERO) > 0) {
                    dueByBuyer.merge(s.getBuyerId(), due, BigDecimal::add);
                }
            }

            for (Buyer b : buyers) {
                BigDecimal totalDue = dueByBuyer.getOrDefault(b.getBuyerId(), BigDecimal.ZERO);
                if (totalDue.compareTo(BigDecimal.ZERO) > 0) {
                    String dedupeKey = "DUE_PAYMENT:" + b.getBuyerId();
                    String details = "Total due Tk " + totalDue.toPlainString();
                    candidates.put(dedupeKey, new AlertCandidate(
                            AlertType.DUE_PAYMENT,
                            AlertEntityType.BUYER,
                            b.getBuyerId(),
                            null,
                            b.getName(),
                            details,
                            dedupeKey
                    ));
                }
            }
        }

        // 4. LICENSE_EXPIRY
        if (Boolean.TRUE.equals(settings.getLicenseEnabled())) {
            int licenseDays = settings.getLicenseExpiryDays() != null ? settings.getLicenseExpiryDays() : 30;
            List<Driver> drivers = driverRepository.findByActiveTrueOrderByDriverIdAsc();

            for (Driver d : drivers) {
                if (d.getLicenseExpiry() != null) {
                    long days = ChronoUnit.DAYS.between(today, d.getLicenseExpiry());
                    if (days <= licenseDays) {
                        String dedupeKey = "LICENSE_EXPIRY:" + d.getDriverId();
                        String details;
                        if (days < 0) {
                            details = "License expired on " + d.getLicenseExpiry();
                        } else if (days == 0) {
                            details = "License expires today";
                        } else {
                            details = "License expires in " + days + " days (" + d.getLicenseExpiry() + ")";
                        }

                        candidates.put(dedupeKey, new AlertCandidate(
                                AlertType.LICENSE_EXPIRY,
                                AlertEntityType.DRIVER,
                                d.getDriverId(),
                                null,
                                d.getName(),
                                details,
                                dedupeKey
                        ));
                    }
                }
            }
        }

        // Apply upserts
        for (AlertCandidate c : candidates.values()) {
            Optional<Alert> opt = alertRepository.findByDedupeKey(c.dedupeKey());
            if (opt.isPresent()) {
                Alert existing = opt.get();
                if (existing.getResolvedAt() != null) {
                    existing.setResolvedAt(null);
                    existing.setIsRead(false);
                    existing.setReadAt(null);
                    existing.setReadBy(null);
                }
                existing.setItemName(c.itemName());
                existing.setDetails(c.details());
                alertRepository.save(existing);
            } else {
                Alert a = new Alert();
                a.setType(c.type());
                a.setEntityType(c.entityType());
                a.setEntityId(c.entityId());
                a.setWarehouseId(c.warehouseId());
                a.setItemName(c.itemName());
                a.setDetails(c.details());
                a.setDedupeKey(c.dedupeKey());
                a.setIsRead(false);
                a.setCreatedAt(LocalDateTime.now());
                alertRepository.save(a);
            }
        }

        // Resolve active alerts that are no longer candidates
        List<Alert> allActive = alertRepository.findAllActive();
        for (Alert a : allActive) {
            if (!candidates.containsKey(a.getDedupeKey())) {
                a.setResolvedAt(LocalDateTime.now());
                alertRepository.save(a);
            }
        }
    }

    public record AlertCandidate(
            AlertType type,
            AlertEntityType entityType,
            Long entityId,
            Long warehouseId,
            String itemName,
            String details,
            String dedupeKey
    ) {}
}
