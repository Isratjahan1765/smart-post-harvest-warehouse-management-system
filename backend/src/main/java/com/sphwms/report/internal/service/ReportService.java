package com.sphwms.report.internal.service;

import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.enums.ArrivalStatus;
import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.donation.internal.entity.Donation;
import com.sphwms.donation.internal.enums.DonationStatus;
import com.sphwms.donation.internal.repository.DonationRepository;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.repository.ProductRepository;
import com.sphwms.report.internal.dto.responses.CategoryRevenueResponse;
import com.sphwms.report.internal.dto.responses.ReportSummaryResponse;
import com.sphwms.report.internal.dto.responses.TopProductResponse;
import com.sphwms.report.internal.entity.ReportExportLog;
import com.sphwms.report.internal.repository.ReportExportLogRepository;
import com.sphwms.sale.internal.entity.Sale;
import com.sphwms.sale.internal.entity.SaleAllocation;
import com.sphwms.sale.internal.enums.SaleStatus;
import com.sphwms.sale.internal.repository.SaleAllocationRepository;
import com.sphwms.sale.internal.repository.SaleRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.security.CurrentUserProvider;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleAllocationRepository saleAllocationRepository;
    private final ArrivalRepository arrivalRepository;
    private final DonationRepository donationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ReportExportLogRepository reportExportLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(LocalDate from, LocalDate to, Long warehouseId) {
        DateRange range = resolveDateRange(from, to);

        BigDecimal revenue = calculateRevenue(range, warehouseId);
        BigDecimal expense = calculateExpense(range, warehouseId);
        BigDecimal donatedValue = calculateDonatedValue(range, warehouseId);
        BigDecimal netProfit = revenue.subtract(expense);

        return new ReportSummaryResponse(revenue, expense, netProfit, donatedValue);
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenueResponse> getRevenueByCategory(LocalDate from, LocalDate to, Long warehouseId) {
        DateRange range = resolveDateRange(from, to);

        List<MatchingSaleItem> items = getMatchingSaleItems(range, warehouseId);
        Map<Long, Product> prodMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        Map<String, BigDecimal> revByCategory = new HashMap<>();
        BigDecimal totalRev = BigDecimal.ZERO;

        for (MatchingSaleItem it : items) {
            Product p = prodMap.get(it.productId());
            String cat = (p != null) ? p.getCategory().name() : "OTHERS";
            revByCategory.merge(cat, it.revenue(), BigDecimal::add);
            totalRev = totalRev.add(it.revenue());
        }

        List<CategoryRevenueResponse> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revByCategory.entrySet()) {
            int pct = 0;
            if (totalRev.compareTo(BigDecimal.ZERO) > 0) {
                pct = entry.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalRev, 0, RoundingMode.HALF_UP)
                        .intValue();
            }
            list.add(new CategoryRevenueResponse(entry.getKey(), entry.getValue(), pct));
        }

        list.sort((a, b) -> b.revenue().compareTo(a.revenue()));
        return list;
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, Long warehouseId) {
        DateRange range = resolveDateRange(from, to);

        List<MatchingSaleItem> items = getMatchingSaleItems(range, warehouseId);
        Map<Long, Product> prodMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        Map<Long, BigDecimal[]> prodStats = new HashMap<>(); // [revenue, quantity, cost]

        for (MatchingSaleItem it : items) {
            BigDecimal[] stats = prodStats.computeIfAbsent(it.productId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            stats[0] = stats[0].add(it.revenue());
            stats[1] = stats[1].add(it.quantity());
            stats[2] = stats[2].add(it.cost());
        }

        List<TopProductResponse> list = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> entry : prodStats.entrySet()) {
            Long pId = entry.getKey();
            BigDecimal[] stats = entry.getValue();
            Product p = prodMap.get(pId);
            String name = (p != null) ? p.getName() : "Product #" + pId;
            BigDecimal rev = stats[0];
            BigDecimal qty = stats[1];
            BigDecimal cost = stats[2];
            BigDecimal profit = rev.subtract(cost);

            list.add(new TopProductResponse(pId, name, rev, qty, cost, profit));
        }

        list.sort((a, b) -> {
            int cmp = b.revenue().compareTo(a.revenue());
            if (cmp != 0) return cmp;
            return a.productName().compareTo(b.productName());
        });

        if (list.size() > 5) {
            return list.subList(0, 5);
        }
        return list;
    }

    @Transactional
    public byte[] exportReportToXlsx(LocalDate from, LocalDate to, Long warehouseId) {
        DateRange range = resolveDateRange(from, to);

        ReportSummaryResponse summary = getSummary(range.from(), range.to(), warehouseId);
        List<CategoryRevenueResponse> categories = getRevenueByCategory(range.from(), range.to(), warehouseId);
        List<TopProductResponse> topProducts = getTopProducts(range.from(), range.to(), warehouseId);

        String whName = (warehouseId != null)
                ? warehouseRepository.findById(warehouseId).map(Warehouse::getName).orElse("Warehouse #" + warehouseId)
                : "All Warehouses";

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Sheet 1: Summary
            Sheet sheet1 = workbook.createSheet("Summary");
            sheet1.createRow(0).createCell(0).setCellValue("SPHWMS Financial Report");
            sheet1.createRow(1).createCell(0).setCellValue("Period: " + range.from() + " to " + range.to() + " | Warehouse: " + whName);

            String[] s1Cols = {"Metric", "Amount (Tk)"};
            Row h1 = sheet1.createRow(3);
            h1.createCell(0).setCellValue(s1Cols[0]);
            h1.createCell(1).setCellValue(s1Cols[1]);
            h1.getCell(0).setCellStyle(headerStyle);
            h1.getCell(1).setCellStyle(headerStyle);

            Row r1 = sheet1.createRow(4);
            r1.createCell(0).setCellValue("Total Revenue");
            r1.createCell(1).setCellValue(summary.totalRevenue().doubleValue());

            Row r2 = sheet1.createRow(5);
            r2.createCell(0).setCellValue("Total Expense");
            r2.createCell(1).setCellValue(summary.totalExpense().doubleValue());

            Row r3 = sheet1.createRow(6);
            r3.createCell(0).setCellValue("Net Profit");
            r3.createCell(1).setCellValue(summary.netProfit().doubleValue());

            Row r4 = sheet1.createRow(7);
            r4.createCell(0).setCellValue("Donated Value");
            r4.createCell(1).setCellValue(summary.donatedValue().doubleValue());

            sheet1.autoSizeColumn(0);
            sheet1.autoSizeColumn(1);

            // Sheet 2: Revenue by Category
            Sheet sheet2 = workbook.createSheet("Revenue by Category");
            String[] s2Cols = {"Category", "Revenue (Tk)", "Percentage (%)"};
            Row h2 = sheet2.createRow(0);
            for (int i = 0; i < s2Cols.length; i++) {
                Cell c = h2.createCell(i);
                c.setCellValue(s2Cols[i]);
                c.setCellStyle(headerStyle);
            }
            int rIdx2 = 1;
            for (CategoryRevenueResponse cr : categories) {
                Row row = sheet2.createRow(rIdx2++);
                row.createCell(0).setCellValue(cr.category());
                row.createCell(1).setCellValue(cr.revenue().doubleValue());
                row.createCell(2).setCellValue(cr.percent());
            }
            for (int i = 0; i < s2Cols.length; i++) sheet2.autoSizeColumn(i);

            // Sheet 3: Top Products
            Sheet sheet3 = workbook.createSheet("Top Products");
            String[] s3Cols = {"Product", "Revenue (Tk)", "Quantity (kg)", "Cost (Tk)", "Profit (Tk)"};
            Row h3 = sheet3.createRow(0);
            for (int i = 0; i < s3Cols.length; i++) {
                Cell c = h3.createCell(i);
                c.setCellValue(s3Cols[i]);
                c.setCellStyle(headerStyle);
            }
            int rIdx3 = 1;
            for (TopProductResponse tp : topProducts) {
                Row row = sheet3.createRow(rIdx3++);
                row.createCell(0).setCellValue(tp.productName());
                row.createCell(1).setCellValue(tp.revenue().doubleValue());
                row.createCell(2).setCellValue(tp.quantity().doubleValue());
                row.createCell(3).setCellValue(tp.cost().doubleValue());
                row.createCell(4).setCellValue(tp.profit().doubleValue());
            }
            for (int i = 0; i < s3Cols.length; i++) sheet3.autoSizeColumn(i);

            workbook.write(out);

            // Log export
            ReportExportLog log = new ReportExportLog();
            log.setReportType("FINANCIAL_REPORT");
            log.setPeriodFrom(range.from());
            log.setPeriodTo(range.to());
            log.setWarehouseId(warehouseId);
            log.setExportedBy(currentUserProvider.getCurrentUserId());
            log.setExportedAt(LocalDateTime.now());
            reportExportLogRepository.save(log);

            auditService.log("REPORT_EXPORT", "REPORT", null, "Exported financial report to XLSX");

            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate the export file.");
        }
    }

    private BigDecimal calculateRevenue(DateRange range, Long warehouseId) {
        if (warehouseId == null) {
            return saleRepository.sumRevenueBetween(range.from(), range.to());
        } else {
            return saleRepository.sumRevenueBetweenForWarehouse(warehouseId, range.from(), range.to());
        }
    }

    private BigDecimal calculateExpense(DateRange range, Long warehouseId) {
        LocalDateTime start = range.from().atStartOfDay();
        LocalDateTime end = range.to().plusDays(1).atStartOfDay();
        if (warehouseId == null) {
            return arrivalRepository.sumConfirmedCostBetween(start, end);
        } else {
            return arrivalRepository.sumConfirmedCostBetweenForWarehouse(warehouseId, start, end);
        }
    }

    private BigDecimal calculateDonatedValue(DateRange range, Long warehouseId) {
        if (warehouseId == null) {
            return donationRepository.sumDonatedValueBetween(range.from(), range.to());
        } else {
            return donationRepository.sumDonatedValueBetweenForWarehouse(warehouseId, range.from(), range.to());
        }
    }

    private List<MatchingSaleItem> getMatchingSaleItems(DateRange range, Long warehouseId) {
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFIRMED)
                .filter(s -> !s.getSaleDate().isBefore(range.from()) && !s.getSaleDate().isAfter(range.to()))
                .collect(Collectors.toList());

        List<SaleAllocation> allocations = saleAllocationRepository.findAll();
        Map<Long, List<SaleAllocation>> allocBySale = allocations.stream()
                .collect(Collectors.groupingBy(SaleAllocation::getSaleId));

        List<MatchingSaleItem> items = new ArrayList<>();
        for (Sale s : sales) {
            List<SaleAllocation> sAllocs = allocBySale.getOrDefault(s.getSaleId(), Collections.emptyList());
            if (sAllocs.isEmpty()) {
                // If no allocations recorded, use whole sale
                if (warehouseId == null) {
                    BigDecimal rev = s.getQuantity().multiply(s.getRate());
                    BigDecimal cost = s.getQuantity().multiply(s.getPurchasePrice());
                    items.add(new MatchingSaleItem(s.getProductId(), s.getQuantity(), rev, cost));
                }
            } else {
                for (SaleAllocation a : sAllocs) {
                    if (warehouseId == null || a.getWarehouseId().equals(warehouseId)) {
                        BigDecimal rev = a.getQuantity().multiply(s.getRate());
                        BigDecimal cost = a.getQuantity().multiply(s.getPurchasePrice());
                        items.add(new MatchingSaleItem(s.getProductId(), a.getQuantity(), rev, cost));
                    }
                }
            }
        }
        return items;
    }

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Invalid date range.");
        }
        if (from == null && to == null) {
            YearMonth ym = YearMonth.now();
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else if (from == null) {
            from = to.withDayOfMonth(1);
        } else if (to == null) {
            to = from.plusMonths(1).minusDays(1);
        }
        return new DateRange(from, to);
    }

    private record DateRange(LocalDate from, LocalDate to) {}
    private record MatchingSaleItem(Long productId, BigDecimal quantity, BigDecimal revenue, BigDecimal cost) {}
}
