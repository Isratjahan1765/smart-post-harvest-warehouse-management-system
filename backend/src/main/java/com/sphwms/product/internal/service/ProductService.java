package com.sphwms.product.internal.service;

import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.entity.QualityCheck;
import com.sphwms.arrival.internal.enums.ArrivalGrade;
import com.sphwms.arrival.internal.enums.ArrivalStatus;
import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.arrival.internal.repository.QualityCheckRepository;
import com.sphwms.product.internal.dto.requests.ProductRequest;
import com.sphwms.product.internal.dto.responses.CategorySummaryResponse;
import com.sphwms.product.internal.dto.responses.ProductResponse;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.enums.ProductCategory;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ArrivalRepository arrivalRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final IdSequenceService idSequenceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search, String category, Long warehouseId, String status) {
        List<Product> products = productRepository.findByActiveTrueOrderByProductIdAsc();
        List<StockBatch> allBatches = stockBatchRepository.findAll();
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, w -> w));

        // Group batches by productId
        Map<Long, List<StockBatch>> batchesByProduct = allBatches.stream()
                .collect(Collectors.groupingBy(StockBatch::getProductId));

        String searchLower = (search != null) ? search.trim().toLowerCase() : "";
        String catFilter = (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category))
                ? category.trim().toUpperCase() : null;
        String statusFilter = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase().replace(" ", "_") : null;

        LocalDate today = LocalDate.now();

        return products.stream()
                .filter(p -> {
                    if (searchLower.isEmpty()) return true;
                    return p.getName().toLowerCase().contains(searchLower) ||
                           p.getCode().toLowerCase().contains(searchLower);
                })
                .filter(p -> {
                    if (catFilter == null) return true;
                    return p.getCategory().name().equalsIgnoreCase(catFilter);
                })
                .map(p -> {
                    List<StockBatch> pBatches = batchesByProduct.getOrDefault(p.getProductId(), Collections.emptyList());

                    // If warehouse filter is specified, filter batches
                    if (warehouseId != null) {
                        pBatches = pBatches.stream()
                                .filter(b -> b.getWarehouseId().equals(warehouseId))
                                .collect(Collectors.toList());
                    }

                    BigDecimal totalRemaining = BigDecimal.ZERO;
                    LocalDate nearestExpiry = null;
                    LocalDate earliestEntry = null;
                    boolean hasExpiredBatch = false;
                    boolean hasNearExpiryBatch = false;

                    // Available in: "Warehouse A: 900 kg"
                    Map<Long, BigDecimal> qtyByWarehouse = new HashMap<>();

                    for (StockBatch b : pBatches) {
                        BigDecimal rem = b.getRemainingQuantity() != null ? b.getRemainingQuantity() : BigDecimal.ZERO;
                        if (rem.compareTo(BigDecimal.ZERO) > 0) {
                            totalRemaining = totalRemaining.add(rem);
                            qtyByWarehouse.merge(b.getWarehouseId(), rem, BigDecimal::add);

                            if (b.getExpiryDate() != null) {
                                if (nearestExpiry == null || b.getExpiryDate().isBefore(nearestExpiry)) {
                                    nearestExpiry = b.getExpiryDate();
                                }
                                if (b.getExpiryDate().isBefore(today)) {
                                    hasExpiredBatch = true;
                                } else if (!b.getExpiryDate().isAfter(today.plusDays(7))) {
                                    hasNearExpiryBatch = true;
                                }
                            }
                            if (b.getEntryDate() != null) {
                                if (earliestEntry == null || b.getEntryDate().isBefore(earliestEntry)) {
                                    earliestEntry = b.getEntryDate();
                                }
                            }
                        }
                    }

                    // Format availableIn list
                    List<String> availableIn = new ArrayList<>();
                    for (Map.Entry<Long, BigDecimal> entry : qtyByWarehouse.entrySet()) {
                        Warehouse wh = warehouseMap.get(entry.getKey());
                        String whName = wh != null ? wh.getName() : "Warehouse #" + entry.getKey();
                        availableIn.add(whName + ": " + entry.getValue().stripTrailingZeros().toPlainString() + " kg");
                    }

                    // Compute status: Expired first, then Out of Stock, Near Expiry, Low Stock, In Stock
                    String pStatus;
                    if (hasExpiredBatch && totalRemaining.compareTo(BigDecimal.ZERO) == 0) {
                        pStatus = "EXPIRED";
                    } else if (totalRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        pStatus = "OUT_OF_STOCK";
                    } else if (hasNearExpiryBatch) {
                        pStatus = "NEAR_EXPIRY";
                    } else if (p.getReorderLevel() != null && totalRemaining.compareTo(p.getReorderLevel()) <= 0) {
                        pStatus = "LOW_STOCK";
                    } else {
                        pStatus = "IN_STOCK";
                    }

                    return new ProductResponse(
                            p.getProductId(),
                            p.getCode(),
                            p.getName(),
                            p.getCategory(),
                            p.getUnit(),
                            p.getPurchasePrice(),
                            p.getSellingPrice(),
                            p.getReorderLevel(),
                            totalRemaining,
                            availableIn,
                            earliestEntry,
                            nearestExpiry,
                            pStatus,
                            Boolean.TRUE.equals(p.getActive())
                    );
                })
                .filter(res -> {
                    if (warehouseId != null && res.currentStock().compareTo(BigDecimal.ZERO) <= 0) {
                        return false;
                    }
                    if (statusFilter == null) return true;
                    return res.status().equalsIgnoreCase(statusFilter);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found."));
        List<ProductResponse> all = getAllProducts(null, null, null, null);
        return all.stream().filter(pr -> pr.productId().equals(id)).findFirst()
                .orElse(toBasicResponse(p));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (req.purchasePrice() == null || req.purchasePrice().compareTo(BigDecimal.ZERO) <= 0 ||
            req.sellingPrice() == null || req.sellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid price - enter a positive number.");
        }

        if (req.expiryDate() != null && req.entryDate() != null && req.expiryDate().isBefore(req.entryDate())) {
            throw new BusinessException("Expiry date must be after entry date.");
        }

        String code = idSequenceService.nextProductCode();
        Product p = new Product();
        p.setCode(code);
        p.setName(req.name().trim());
        p.setCategory(req.category());
        p.setUnit("kg");
        p.setPurchasePrice(req.purchasePrice());
        p.setSellingPrice(req.sellingPrice());
        p.setReorderLevel(req.reorderLevel() != null ? req.reorderLevel() : BigDecimal.ZERO);
        p.setActive(true);
        p.setCreatedBy(currentUserProvider.getCurrentUserId());
        p.setCreatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(p);

        // If initial quantity is specified, create opening-stock arrival, batch, and transaction
        BigDecimal qty = req.quantity() != null ? req.quantity() : BigDecimal.ZERO;
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            if (req.warehouseId() == null) {
                throw new BusinessException("Warehouse is required when specifying stock quantity.");
            }
            Warehouse warehouse = warehouseRepository.findByIdWithLock(req.warehouseId())
                    .orElseThrow(() -> new BusinessException("Warehouse not found."));
            if (!Boolean.TRUE.equals(warehouse.getActive())) {
                throw new BusinessException("Cannot add stock to an inactive warehouse.");
            }

            LocalDate entry = req.entryDate() != null ? req.entryDate() : LocalDate.now();
            LocalDate expiry = req.expiryDate() != null ? req.expiryDate() : entry.plusMonths(6);

            String batchNo = idSequenceService.nextBatchNo(entry);

            Arrival arrival = new Arrival();
            arrival.setBatchNo(batchNo);
            arrival.setProductId(savedProduct.getProductId());
            arrival.setWarehouseId(warehouse.getWarehouseId());
            arrival.setQuantity(qty);
            arrival.setGrade(ArrivalGrade.A);
            arrival.setExpiryDate(expiry);
            arrival.setPurchasePrice(req.purchasePrice());
            arrival.setTotalCost(qty.multiply(req.purchasePrice()));
            arrival.setStatus(ArrivalStatus.CONFIRMED);
            arrival.setCreatedBy(currentUserProvider.getCurrentUserId());
            arrival.setCreatedAt(LocalDateTime.now());
            arrival.setConfirmedBy(currentUserProvider.getCurrentUserId());
            arrival.setConfirmedAt(LocalDateTime.now());
            Arrival savedArrival = arrivalRepository.save(arrival);

            QualityCheck qc = new QualityCheck();
            qc.setArrivalId(savedArrival.getArrivalId());
            qc.setPackagingOk(true);
            qc.setNoDamage(true);
            qc.setQuantityMatch(true);
            qc.setExpiryValid(true);
            qc.setGradeMatch(true);
            qc.setCheckedBy(currentUserProvider.getCurrentUserId());
            qc.setCheckedAt(LocalDateTime.now());
            qualityCheckRepository.save(qc);

            StockBatch batch = new StockBatch();
            batch.setArrivalId(savedArrival.getArrivalId());
            batch.setProductId(savedProduct.getProductId());
            batch.setWarehouseId(warehouse.getWarehouseId());
            batch.setInitialQuantity(qty);
            batch.setRemainingQuantity(qty);
            batch.setUnitCost(req.purchasePrice());
            batch.setEntryDate(entry);
            batch.setExpiryDate(expiry);
            batch.setCreatedAt(LocalDateTime.now());
            StockBatch savedBatch = stockBatchRepository.save(batch);

            StockTransaction st = new StockTransaction();
            st.setProductId(savedProduct.getProductId());
            st.setBatchId(savedBatch.getBatchId());
            st.setWarehouseId(warehouse.getWarehouseId());
            st.setTxnType(StockTxnType.ARRIVAL);
            st.setQuantity(qty);
            st.setRefType("ARRIVAL");
            st.setRefId(savedArrival.getArrivalId());
            st.setTxnDate(LocalDateTime.now());
            st.setUserId(currentUserProvider.getCurrentUserId());
            stockTransactionRepository.save(st);
        }

        auditService.log("PRODUCT_CREATE", "PRODUCT", savedProduct.getProductId(),
                "Created product " + savedProduct.getCode() + " (" + savedProduct.getName() + ")");

        return getProductById(savedProduct.getProductId());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found."));

        if (req.purchasePrice() == null || req.purchasePrice().compareTo(BigDecimal.ZERO) <= 0 ||
            req.sellingPrice() == null || req.sellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid price - enter a positive number.");
        }

        p.setName(req.name().trim());
        p.setCategory(req.category());
        p.setPurchasePrice(req.purchasePrice());
        p.setSellingPrice(req.sellingPrice());
        if (req.reorderLevel() != null) {
            p.setReorderLevel(req.reorderLevel());
        }
        p.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(p);
        auditService.log("PRODUCT_UPDATE", "PRODUCT", saved.getProductId(),
                "Updated product " + saved.getCode());

        return getProductById(saved.getProductId());
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found."));
        p.setActive(false);
        p.setUpdatedAt(LocalDateTime.now());
        productRepository.save(p);

        auditService.log("PRODUCT_DELETE", "PRODUCT", p.getProductId(),
                "Soft deleted product " + p.getCode());
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategorySummary() {
        List<Object[]> rows = productRepository.countActiveByCategory();
        Map<ProductCategory, Long> map = new EnumMap<>(ProductCategory.class);
        for (Object[] r : rows) {
            map.put((ProductCategory) r[0], (Long) r[1]);
        }

        List<CategorySummaryResponse> list = new ArrayList<>();
        for (ProductCategory cat : ProductCategory.values()) {
            long count = map.getOrDefault(cat, 0L);
            String name = cat.name().charAt(0) + cat.name().substring(1).toLowerCase();
            list.add(new CategorySummaryResponse(cat, name, count));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return Arrays.stream(ProductCategory.values())
                .map(cat -> cat.name().charAt(0) + cat.name().substring(1).toLowerCase())
                .collect(Collectors.toList());
    }

    private ProductResponse toBasicResponse(Product p) {
        return new ProductResponse(
                p.getProductId(),
                p.getCode(),
                p.getName(),
                p.getCategory(),
                p.getUnit(),
                p.getPurchasePrice(),
                p.getSellingPrice(),
                p.getReorderLevel(),
                BigDecimal.ZERO,
                Collections.emptyList(),
                null,
                null,
                "OUT_OF_STOCK",
                Boolean.TRUE.equals(p.getActive())
        );
    }
}
