package com.sphwms.sale.internal.service;

import com.sphwms.buyer.internal.entity.Buyer;
import com.sphwms.buyer.internal.enums.BuyerStatus;
import com.sphwms.buyer.internal.repository.BuyerRepository;
import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.repository.ProductRepository;
import com.sphwms.sale.internal.dto.requests.SaleRequest;
import com.sphwms.sale.internal.dto.responses.SaleResponse;
import com.sphwms.sale.internal.entity.Sale;
import com.sphwms.sale.internal.entity.SaleAllocation;
import com.sphwms.sale.internal.enums.PaymentStatus;
import com.sphwms.sale.internal.enums.SaleStatus;
import com.sphwms.sale.internal.repository.SaleAllocationRepository;
import com.sphwms.sale.internal.repository.SaleRepository;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleAllocationRepository saleAllocationRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final IdSequenceService idSequenceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SaleResponse> getAllSales(Long buyerId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Invalid date range.");
        }

        List<Sale> sales = (buyerId != null)
                ? saleRepository.findByBuyerIdOrderBySaleIdDesc(buyerId)
                : saleRepository.findAllByOrderBySaleIdDesc();

        Map<Long, String> buyerNames = buyerRepository.findAll().stream()
                .collect(Collectors.toMap(Buyer::getBuyerId, Buyer::getName, (a, b) -> a));
        Map<Long, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));

        return sales.stream()
                .filter(s -> from == null || !s.getSaleDate().isBefore(from))
                .filter(s -> to == null || !s.getSaleDate().isAfter(to))
                .map(s -> toResponse(s, buyerNames, productNames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getNextSaleNo() {
        return Map.of("saleNo", idSequenceService.peekSaleNo());
    }

    @Transactional
    public SaleResponse createSale(SaleRequest req) {
        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid quantity - enter a positive number.");
        }

        LocalDate saleDate = req.saleDate() != null ? req.saleDate() : LocalDate.now();
        if (saleDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Sale date cannot be in the future.");
        }

        Buyer buyer = buyerRepository.findById(req.buyerId())
                .orElseThrow(() -> new BusinessException("Buyer not found."));
        if (buyer.getStatus() != BuyerStatus.ACTIVE) {
            throw new BusinessException("Cannot create sale for inactive buyer.");
        }

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException("Product not found."));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException("Cannot create sale for inactive product.");
        }

        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new BusinessException("Warehouse not found."));
        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new BusinessException("Cannot create sale for inactive warehouse.");
        }

        BigDecimal rate = product.getSellingPrice();
        BigDecimal purchasePrice = product.getPurchasePrice();
        BigDecimal totalAmount = req.quantity().multiply(rate);

        BigDecimal paid = req.paidNow() != null ? req.paidNow()
                : (req.paidAmount() != null ? req.paidAmount() : BigDecimal.ZERO);
        if (paid.compareTo(totalAmount) > 0) {
            throw new BusinessException("Paid amount cannot be more than the total.");
        }

        // Lock batches FIFO by expiryDate ASC, batchId ASC
        List<StockBatch> batches = stockBatchRepository.findAvailableBatchesForSaleWithLock(
                req.productId(), req.warehouseId(), LocalDate.now());

        BigDecimal availableStock = batches.stream()
                .map(StockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (availableStock.compareTo(req.quantity()) < 0) {
            throw new BusinessException("Insufficient stock for this sale.");
        }

        String saleNo = idSequenceService.nextSaleNo();

        PaymentStatus paymentStatus = (paid.compareTo(totalAmount) >= 0) ? PaymentStatus.PAID : PaymentStatus.DUE;
        LocalDate dueDate = req.dueDate();
        if (paymentStatus == PaymentStatus.DUE && dueDate == null) {
            dueDate = saleDate.plusDays(15);
        }

        Sale sale = new Sale();
        sale.setSaleNo(saleNo);
        sale.setBuyerId(buyer.getBuyerId());
        sale.setProductId(product.getProductId());
        sale.setQuantity(req.quantity());
        sale.setRate(rate);
        sale.setPurchasePrice(purchasePrice);
        sale.setTotalAmount(totalAmount);
        sale.setPaidAmount(paid);
        sale.setPaymentStatus(paymentStatus);
        sale.setStatus(SaleStatus.CONFIRMED);
        sale.setSaleDate(saleDate);
        sale.setDueDate(dueDate);
        sale.setCreatedBy(currentUserProvider.getCurrentUserId());
        sale.setCreatedAt(LocalDateTime.now());

        Sale savedSale = saleRepository.save(sale);

        // Deduct stock FIFO
        BigDecimal needed = req.quantity();
        for (StockBatch b : batches) {
            if (needed.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal take = b.getRemainingQuantity().min(needed);
            b.setRemainingQuantity(b.getRemainingQuantity().subtract(take));
            stockBatchRepository.save(b);

            SaleAllocation alloc = new SaleAllocation();
            alloc.setSaleId(savedSale.getSaleId());
            alloc.setBatchId(b.getBatchId());
            alloc.setWarehouseId(req.warehouseId());
            alloc.setQuantity(take);
            saleAllocationRepository.save(alloc);

            StockTransaction st = new StockTransaction();
            st.setProductId(product.getProductId());
            st.setBatchId(b.getBatchId());
            st.setWarehouseId(req.warehouseId());
            st.setTxnType(StockTxnType.SALE);
            st.setQuantity(take);
            st.setRefType("SALE");
            st.setRefId(savedSale.getSaleId());
            st.setTxnDate(LocalDateTime.now());
            st.setUserId(currentUserProvider.getCurrentUserId());
            stockTransactionRepository.save(st);

            needed = needed.subtract(take);
        }

        auditService.log("SALE_CREATE", "SALE", savedSale.getSaleId(),
                "Created sale " + savedSale.getSaleNo() + " for buyer " + buyer.getName());

        return toResponse(savedSale, Map.of(buyer.getBuyerId(), buyer.getName()), Map.of(product.getProductId(), product.getName()));
    }

    @Transactional
    public SaleResponse cancelSale(Long id, String reason) {
        Sale sale = saleRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Sale not found."));

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Sale is already cancelled.");
        }

        List<SaleAllocation> allocations = saleAllocationRepository.findBySaleId(id);
        for (SaleAllocation alloc : allocations) {
            StockBatch batch = stockBatchRepository.findByIdWithLock(alloc.getBatchId())
                    .orElse(null);
            if (batch != null) {
                batch.setRemainingQuantity(batch.getRemainingQuantity().add(alloc.getQuantity()));
                stockBatchRepository.save(batch);

                StockTransaction st = new StockTransaction();
                st.setProductId(sale.getProductId());
                st.setBatchId(batch.getBatchId());
                st.setWarehouseId(alloc.getWarehouseId());
                st.setTxnType(StockTxnType.SALE_CANCEL);
                st.setQuantity(alloc.getQuantity());
                st.setRefType("SALE_CANCEL");
                st.setRefId(sale.getSaleId());
                st.setTxnDate(LocalDateTime.now());
                st.setUserId(currentUserProvider.getCurrentUserId());
                stockTransactionRepository.save(st);
            }
        }

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelReason(reason != null ? reason.trim() : "Cancelled by manager");
        Sale saved = saleRepository.save(sale);

        auditService.log("SALE_CANCEL", "SALE", saved.getSaleId(),
                "Cancelled sale " + saved.getSaleNo());

        String buyerName = buyerRepository.findById(saved.getBuyerId()).map(Buyer::getName).orElse("");
        String prodName = productRepository.findById(saved.getProductId()).map(Product::getName).orElse("");
        return toResponse(saved, Map.of(saved.getBuyerId(), buyerName), Map.of(saved.getProductId(), prodName));
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesToXlsx(Long buyerId, LocalDate from, LocalDate to) {
        List<SaleResponse> sales = getAllSales(buyerId, from, to);
        if (sales.isEmpty()) {
            throw new BusinessException("No data to export.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Purchase History");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] columns = {"Sale No", "Date", "Buyer", "Product", "Quantity (kg)", "Rate (Tk)", "Total (Tk)", "Paid (Tk)", "Due (Tk)", "Payment Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal totalAmountSum = BigDecimal.ZERO;
            BigDecimal paidAmountSum = BigDecimal.ZERO;
            BigDecimal dueAmountSum = BigDecimal.ZERO;

            for (SaleResponse s : sales) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.saleNo());
                row.createCell(1).setCellValue(s.saleDate() != null ? s.saleDate().toString() : "");
                row.createCell(2).setCellValue(s.buyerName());
                row.createCell(3).setCellValue(s.productName());
                row.createCell(4).setCellValue(s.quantity().doubleValue());
                row.createCell(5).setCellValue(s.rate().doubleValue());
                row.createCell(6).setCellValue(s.totalAmount().doubleValue());
                row.createCell(7).setCellValue(s.paidAmount().doubleValue());
                row.createCell(8).setCellValue(s.dueAmount().doubleValue());
                row.createCell(9).setCellValue(s.paymentStatus().name());

                totalAmountSum = totalAmountSum.add(s.totalAmount());
                paidAmountSum = paidAmountSum.add(s.paidAmount());
                dueAmountSum = dueAmountSum.add(s.dueAmount());
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(headerStyle);
            totalRow.createCell(6).setCellValue(totalAmountSum.doubleValue());
            totalRow.createCell(7).setCellValue(paidAmountSum.doubleValue());
            totalRow.createCell(8).setCellValue(dueAmountSum.doubleValue());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            auditService.log("SALES_EXPORT", "SALE", null, "Exported purchase history to XLSX");
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate the export file.");
        }
    }

    private SaleResponse toResponse(Sale s, Map<Long, String> buyerNames, Map<Long, String> prodNames) {
        BigDecimal paid = s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal due = s.getTotalAmount().subtract(paid);
        if (due.compareTo(BigDecimal.ZERO) < 0) due = BigDecimal.ZERO;

        return new SaleResponse(
                s.getSaleId(),
                s.getSaleNo(),
                s.getBuyerId(),
                buyerNames.getOrDefault(s.getBuyerId(), "Buyer #" + s.getBuyerId()),
                s.getProductId(),
                prodNames.getOrDefault(s.getProductId(), "Product #" + s.getProductId()),
                s.getQuantity(),
                s.getRate(),
                s.getPurchasePrice(),
                s.getTotalAmount(),
                paid,
                due,
                s.getPaymentStatus(),
                s.getStatus(),
                s.getSaleDate(),
                s.getDueDate(),
                s.getCreatedAt()
        );
    }
}
