package com.sphwms.dashboard.internal.service;

import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.dashboard.internal.dto.responses.DashboardSummaryResponse;
import com.sphwms.dashboard.internal.dto.responses.InventoryRankingItem;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.repository.ProductRepository;
import com.sphwms.sale.internal.repository.SaleRepository;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.stock.internal.entity.StockBatch;
import com.sphwms.stock.internal.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final ArrivalRepository arrivalRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalProducts = productRepository.countByActiveTrue();

        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay();
        long arrivalsThisMonth = arrivalRepository.countConfirmedBetween(monthStart, monthEnd);

        List<StockBatch> batches = stockBatchRepository.findAll();
        BigDecimal stockValue = BigDecimal.ZERO;
        for (StockBatch b : batches) {
            if (b.getRemainingQuantity() != null && b.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cost = b.getUnitCost() != null ? b.getUnitCost() : BigDecimal.ZERO;
                stockValue = stockValue.add(b.getRemainingQuantity().multiply(cost));
            }
        }

        BigDecimal totalDue = saleRepository.sumTotalDue();

        return new DashboardSummaryResponse(totalProducts, arrivalsThisMonth, stockValue, totalDue);
    }

    @Transactional(readOnly = true)
    public List<InventoryRankingItem> getInventoryRanking(int limit) {
        if (limit <= 0) {
            throw new BusinessException("Enter a valid quantity.");
        }

        List<StockBatch> batches = stockBatchRepository.findAll();
        Map<Long, Product> prodMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        Map<Long, BigDecimal[]> prodStock = new HashMap<>(); // [quantity, totalValue]

        for (StockBatch b : batches) {
            if (b.getRemainingQuantity() != null && b.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal[] stats = prodStock.computeIfAbsent(b.getProductId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal cost = b.getUnitCost() != null ? b.getUnitCost() : BigDecimal.ZERO;
                stats[0] = stats[0].add(b.getRemainingQuantity());
                stats[1] = stats[1].add(b.getRemainingQuantity().multiply(cost));
            }
        }

        List<InventoryRankingItem> list = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> entry : prodStock.entrySet()) {
            Long pId = entry.getKey();
            BigDecimal[] stats = entry.getValue();
            Product p = prodMap.get(pId);
            if (p != null && Boolean.TRUE.equals(p.getActive()) && stats[1].compareTo(BigDecimal.ZERO) > 0) {
                String cat = p.getCategory().name().charAt(0) + p.getCategory().name().substring(1).toLowerCase();
                list.add(new InventoryRankingItem(pId, p.getName(), cat, stats[0], stats[1]));
            }
        }

        list.sort((a, b) -> {
            int cmp = b.inventoryValue().compareTo(a.inventoryValue());
            if (cmp != 0) return cmp;
            return a.productName().compareTo(b.productName());
        });

        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }
}
