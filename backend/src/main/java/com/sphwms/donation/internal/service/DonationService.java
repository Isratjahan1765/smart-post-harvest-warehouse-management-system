package com.sphwms.donation.internal.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.repository.ArrivalRepository;
import com.sphwms.donation.internal.dto.requests.DonationRequest;
import com.sphwms.donation.internal.dto.requests.ReceiverRequest;
import com.sphwms.donation.internal.dto.responses.*;
import com.sphwms.donation.internal.entity.Donation;
import com.sphwms.donation.internal.entity.Receiver;
import com.sphwms.donation.internal.enums.DonationStatus;
import com.sphwms.donation.internal.enums.ReceiverStatus;
import com.sphwms.donation.internal.repository.DonationRepository;
import com.sphwms.donation.internal.repository.ReceiverRepository;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final ReceiverRepository receiverRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ArrivalRepository arrivalRepository;
    private final IdSequenceService idSequenceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EligibleDonationResponse> getEligibleBatches(String search) {
        List<StockBatch> batches = stockBatchRepository.findAll();
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, w -> w));
        Map<Long, String> batchNoMap = arrivalRepository.findAll().stream()
                .collect(Collectors.toMap(Arrival::getArrivalId, Arrival::getBatchNo));

        LocalDate today = LocalDate.now();
        String searchLower = search != null ? search.trim().toLowerCase() : "";

        return batches.stream()
                .filter(b -> b.getRemainingQuantity() != null && b.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .filter(b -> {
                    // Eligible for donation if remaining > 0 and expiry is within 7 days or nearing expiry
                    if (b.getExpiryDate() == null) return false;
                    long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());
                    return days >= -2 && days <= 7;
                })
                .map(b -> {
                    Product prod = productMap.get(b.getProductId());
                    Warehouse wh = warehouseMap.get(b.getWarehouseId());
                    String pName = prod != null ? prod.getName() : "Product #" + b.getProductId();
                    String whName = wh != null ? wh.getName() : "Warehouse #" + b.getWarehouseId();
                    String bNo = batchNoMap.getOrDefault(b.getArrivalId(), "BATCH-" + b.getBatchId());
                    long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());

                    return new EligibleDonationResponse(
                            b.getBatchId(),
                            bNo,
                            b.getProductId(),
                            pName,
                            b.getWarehouseId(),
                            whName,
                            b.getRemainingQuantity(),
                            b.getRemainingQuantity(),
                            b.getExpiryDate(),
                            days
                    );
                })
                .filter(r -> searchLower.isEmpty() ||
                             r.productName().toLowerCase().contains(searchLower) ||
                             r.batchNo().toLowerCase().contains(searchLower))
                .sorted(Comparator.comparing(EligibleDonationResponse::expiryDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiverResponse> getAllReceivers(String status) {
        ReceiverStatus st = (status != null && !"ALL".equalsIgnoreCase(status))
                ? ReceiverStatus.valueOf(status.trim().toUpperCase())
                : ReceiverStatus.ACTIVE;
        return receiverRepository.findByStatusOrderByNameAsc(st).stream()
                .map(this::toReceiverResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReceiverResponse createReceiver(ReceiverRequest req) {
        Receiver r = new Receiver();
        r.setName(req.name().trim());
        r.setContactPerson(req.contactPerson() != null ? req.contactPerson().trim() : null);
        r.setPhone(req.phone().trim());
        r.setAddress(req.address().trim());
        r.setType(req.type());
        r.setStatus(ReceiverStatus.ACTIVE);
        r.setCreatedBy(currentUserProvider.getCurrentUserId());
        r.setCreatedAt(LocalDateTime.now());

        Receiver saved = receiverRepository.save(r);
        auditService.log("RECEIVER_CREATE", "RECEIVER", saved.getReceiverId(),
                "Created receiver " + saved.getName());
        return toReceiverResponse(saved);
    }

    @Transactional
    public ReceiverResponse updateReceiver(Long id, ReceiverRequest req) {
        Receiver r = receiverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Receiver not found."));

        r.setName(req.name().trim());
        r.setContactPerson(req.contactPerson() != null ? req.contactPerson().trim() : null);
        r.setPhone(req.phone().trim());
        r.setAddress(req.address().trim());
        r.setType(req.type());

        Receiver saved = receiverRepository.save(r);
        auditService.log("RECEIVER_UPDATE", "RECEIVER", saved.getReceiverId(),
                "Updated receiver " + saved.getName());
        return toReceiverResponse(saved);
    }

    @Transactional
    public DonationResponse createDonation(DonationRequest req) {
        if (req.safeConfirmed() == null || !req.safeConfirmed()) {
            throw new BusinessException("Please confirm the food is safe to eat.");
        }

        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invalid quantity - enter a positive number.");
        }

        StockBatch batch = stockBatchRepository.findByIdWithLock(req.batchId())
                .orElseThrow(() -> new BusinessException("Batch not found."));

        if (batch.getRemainingQuantity().compareTo(req.quantity()) < 0) {
            throw new BusinessException("Selected quantity is not available.");
        }

        Receiver receiver = receiverRepository.findById(req.receiverId())
                .orElseThrow(() -> new BusinessException("Receiver not found."));
        if (receiver.getStatus() != ReceiverStatus.ACTIVE) {
            throw new BusinessException("Cannot donate to inactive receiver.");
        }

        Product product = productRepository.findById(batch.getProductId())
                .orElseThrow(() -> new BusinessException("Product not found."));

        BigDecimal unitPrice = batch.getUnitCost() != null ? batch.getUnitCost() : product.getPurchasePrice();
        BigDecimal totalValue = req.quantity().multiply(unitPrice);

        String donationNo = idSequenceService.nextDonationNo();
        LocalDate dDate = req.donationDate() != null ? req.donationDate() : LocalDate.now();

        // Reduce batch remaining quantity
        batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(req.quantity()));
        stockBatchRepository.save(batch);

        Donation donation = new Donation();
        donation.setDonationNo(donationNo);
        donation.setReceiverId(receiver.getReceiverId());
        donation.setBatchId(batch.getBatchId());
        donation.setProductId(product.getProductId());
        donation.setWarehouseId(batch.getWarehouseId());
        donation.setQuantity(req.quantity());
        donation.setPurchasePrice(unitPrice);
        donation.setTotalValue(totalValue);
        donation.setConditionNote(req.conditionNote() != null ? req.conditionNote().trim() : "Safe to eat, packaging ok");
        donation.setSafeConfirmed(true);
        donation.setStatus(DonationStatus.CONFIRMED);
        donation.setDonationDate(dDate);
        donation.setDonatedBy(currentUserProvider.getCurrentUserId());
        donation.setCreatedAt(LocalDateTime.now());

        Donation saved = donationRepository.save(donation);

        StockTransaction st = new StockTransaction();
        st.setProductId(product.getProductId());
        st.setBatchId(batch.getBatchId());
        st.setWarehouseId(batch.getWarehouseId());
        st.setTxnType(StockTxnType.DONATION);
        st.setQuantity(req.quantity());
        st.setRefType("DONATION");
        st.setRefId(saved.getDonationId());
        st.setTxnDate(LocalDateTime.now());
        st.setUserId(currentUserProvider.getCurrentUserId());
        stockTransactionRepository.save(st);

        auditService.log("DONATION_CREATE", "DONATION", saved.getDonationId(),
                "Created donation " + saved.getDonationNo() + " for " + receiver.getName());

        Warehouse wh = warehouseRepository.findById(batch.getWarehouseId()).orElse(null);
        String whName = wh != null ? wh.getName() : "";
        String batchNo = arrivalRepository.findById(batch.getArrivalId()).map(Arrival::getBatchNo).orElse("");

        return toDonationResponse(saved, receiver.getName(), product.getName(), batchNo, whName);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getAllDonations(String search, Long receiverId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Invalid date range.");
        }

        List<Donation> list = (receiverId != null)
                ? donationRepository.findByReceiverIdOrderByDonationIdDesc(receiverId)
                : donationRepository.findAllByOrderByDonationIdDesc();

        Map<Long, String> receiverNames = receiverRepository.findAll().stream()
                .collect(Collectors.toMap(Receiver::getReceiverId, Receiver::getName, (a, b) -> a));
        Map<Long, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (a, b) -> a));
        Map<Long, String> batchNoMap = arrivalRepository.findAll().stream()
                .collect(Collectors.toMap(Arrival::getArrivalId, Arrival::getBatchNo, (a, b) -> a));
        Map<Long, Long> batchArrivalMap = stockBatchRepository.findAll().stream()
                .collect(Collectors.toMap(StockBatch::getBatchId, StockBatch::getArrivalId, (a, b) -> a));

        String searchLower = search != null ? search.trim().toLowerCase() : "";

        return list.stream()
                .filter(d -> from == null || !d.getDonationDate().isBefore(from))
                .filter(d -> to == null || !d.getDonationDate().isAfter(to))
                .map(d -> {
                    String rName = receiverNames.getOrDefault(d.getReceiverId(), "");
                    String pName = productNames.getOrDefault(d.getProductId(), "");
                    String whName = warehouseNames.getOrDefault(d.getWarehouseId(), "");
                    Long arrivalId = batchArrivalMap.get(d.getBatchId());
                    String bNo = arrivalId != null ? batchNoMap.getOrDefault(arrivalId, "") : "";
                    return toDonationResponse(d, rName, pName, bNo, whName);
                })
                .filter(res -> searchLower.isEmpty() ||
                               res.donationNo().toLowerCase().contains(searchLower) ||
                               res.receiverName().toLowerCase().contains(searchLower) ||
                               res.productName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReceiverSummaryResponse getSummaryByReceiver() {
        List<Donation> list = donationRepository.findByStatus(DonationStatus.CONFIRMED);
        Map<Long, String> receiverNames = receiverRepository.findAll().stream()
                .collect(Collectors.toMap(Receiver::getReceiverId, Receiver::getName, (a, b) -> a));
        Map<Long, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));

        BigDecimal totalKg = list.stream()
                .map(Donation::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> qtyByReceiver = new HashMap<>();
        Map<Long, Set<String>> prodsByReceiver = new HashMap<>();

        for (Donation d : list) {
            qtyByReceiver.merge(d.getReceiverId(), d.getQuantity(), BigDecimal::add);
            String pName = productNames.getOrDefault(d.getProductId(), "Product");
            prodsByReceiver.computeIfAbsent(d.getReceiverId(), k -> new HashSet<>()).add(pName);
        }

        List<ReceiverSummaryResponse.ReceiverItem> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : qtyByReceiver.entrySet()) {
            Long recId = entry.getKey();
            BigDecimal kg = entry.getValue();
            int percent = 0;
            if (totalKg.compareTo(BigDecimal.ZERO) > 0) {
                percent = kg.multiply(BigDecimal.valueOf(100))
                        .divide(totalKg, 0, RoundingMode.HALF_UP)
                        .intValue();
            }
            String name = receiverNames.getOrDefault(recId, "Receiver #" + recId);
            List<String> prods = new ArrayList<>(prodsByReceiver.getOrDefault(recId, Collections.emptySet()));
            items.add(new ReceiverSummaryResponse.ReceiverItem(recId, name, kg, percent, prods));
        }

        return new ReceiverSummaryResponse(totalKg, items);
    }

    @Transactional
    public DonationResponse cancelDonation(Long id, String reason) {
        Donation d = donationRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Donation not found."));

        if (d.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessException("Donation is already cancelled.");
        }

        StockBatch batch = stockBatchRepository.findByIdWithLock(d.getBatchId()).orElse(null);
        if (batch != null) {
            batch.setRemainingQuantity(batch.getRemainingQuantity().add(d.getQuantity()));
            stockBatchRepository.save(batch);

            StockTransaction st = new StockTransaction();
            st.setProductId(d.getProductId());
            st.setBatchId(batch.getBatchId());
            st.setWarehouseId(d.getWarehouseId());
            st.setTxnType(StockTxnType.DONATION_CANCEL);
            st.setQuantity(d.getQuantity());
            st.setRefType("DONATION_CANCEL");
            st.setRefId(d.getDonationId());
            st.setTxnDate(LocalDateTime.now());
            st.setUserId(currentUserProvider.getCurrentUserId());
            stockTransactionRepository.save(st);
        }

        d.setStatus(DonationStatus.CANCELLED);
        d.setCancelReason(reason != null ? reason.trim() : "Cancelled by manager");
        Donation saved = donationRepository.save(d);

        auditService.log("DONATION_CANCEL", "DONATION", saved.getDonationId(),
                "Cancelled donation " + saved.getDonationNo());

        String rName = receiverRepository.findById(saved.getReceiverId()).map(Receiver::getName).orElse("");
        String pName = productRepository.findById(saved.getProductId()).map(Product::getName).orElse("");
        Warehouse wh = warehouseRepository.findById(saved.getWarehouseId()).orElse(null);
        String whName = wh != null ? wh.getName() : "";
        return toDonationResponse(saved, rName, pName, "", whName);
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(Long id) {
        Donation d = donationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Donation not found."));

        Receiver r = receiverRepository.findById(d.getReceiverId()).orElse(null);
        Product p = productRepository.findById(d.getProductId()).orElse(null);
        Warehouse w = warehouseRepository.findById(d.getWarehouseId()).orElse(null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Smart Post-Harvest Warehouse Management System (SPHWMS)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.GRAY);
            Paragraph sub = new Paragraph("FOOD DONATION RECEIPT\n\n", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addTableRow(table, "Receipt / Donation No:", d.getDonationNo());
            addTableRow(table, "Donation Date:", d.getDonationDate().toString());
            addTableRow(table, "Receiver Name:", r != null ? r.getName() : "N/A");
            addTableRow(table, "Receiver Type:", r != null ? r.getType().name() : "N/A");
            addTableRow(table, "Contact Person:", r != null && r.getContactPerson() != null ? r.getContactPerson() : "N/A");
            addTableRow(table, "Phone:", r != null ? r.getPhone() : "N/A");
            addTableRow(table, "Address:", r != null ? r.getAddress() : "N/A");
            addTableRow(table, "Donated Product:", p != null ? p.getName() : "N/A");
            addTableRow(table, "Warehouse:", w != null ? w.getName() : "N/A");
            addTableRow(table, "Quantity Donated:", d.getQuantity().toPlainString() + " kg");
            addTableRow(table, "Estimated Value:", "Tk " + d.getTotalValue().toPlainString());
            addTableRow(table, "Food Condition Note:", d.getConditionNote() != null ? d.getConditionNote() : "Safe to eat");
            addTableRow(table, "Safety Status:", "Certified Safe for Consumption");
            addTableRow(table, "Authorized By:", "Warehouse Manager (ID: " + d.getDonatedBy() + ")");

            doc.add(table);

            Paragraph footer = new Paragraph("\nThis is an automated system generated receipt.\nSPHWMS - Ensuring Zero Food Wastage & Community Welfare",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            auditService.log("DONATION_RECEIPT", "DONATION", id, "Generated receipt PDF for " + d.getDonationNo());
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Unable to generate receipt PDF.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportDonationsToXlsx(String search, Long receiverId, LocalDate from, LocalDate to) {
        List<DonationResponse> donations = getAllDonations(search, receiverId, from, to);
        if (donations.isEmpty()) {
            throw new BusinessException("No data to export.");
        }

        try (org.apache.poi.ss.usermodel.Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Donation History");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] columns = {"Donation No", "Date", "Receiver", "Product", "Quantity (kg)", "Value (Tk)", "Condition Note", "Status"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal totalKg = BigDecimal.ZERO;
            BigDecimal totalVal = BigDecimal.ZERO;

            for (DonationResponse d : donations) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.donationNo());
                row.createCell(1).setCellValue(d.donationDate() != null ? d.donationDate().toString() : "");
                row.createCell(2).setCellValue(d.receiverName());
                row.createCell(3).setCellValue(d.productName());
                row.createCell(4).setCellValue(d.quantity().doubleValue());
                row.createCell(5).setCellValue(d.totalValue().doubleValue());
                row.createCell(6).setCellValue(d.conditionNote() != null ? d.conditionNote() : "");
                row.createCell(7).setCellValue(d.status().name());

                totalKg = totalKg.add(d.quantity());
                totalVal = totalVal.add(d.totalValue());
            }

            org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowIdx);
            org.apache.poi.ss.usermodel.Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(headerStyle);
            totalRow.createCell(4).setCellValue(totalKg.doubleValue());
            totalRow.createCell(5).setCellValue(totalVal.doubleValue());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            auditService.log("DONATION_EXPORT", "DONATION", null, "Exported donation history to XLSX");
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate the export file.");
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(6f);
        c1.setBackgroundColor(new Color(245, 245, 245));

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        c2.setPadding(6f);

        table.addCell(c1);
        table.addCell(c2);
    }

    private ReceiverResponse toReceiverResponse(Receiver r) {
        return new ReceiverResponse(
                r.getReceiverId(),
                r.getName(),
                r.getContactPerson(),
                r.getPhone(),
                r.getAddress(),
                r.getType(),
                r.getStatus().name()
        );
    }

    private DonationResponse toDonationResponse(Donation d, String rName, String pName, String bNo, String whName) {
        return new DonationResponse(
                d.getDonationId(),
                d.getDonationNo(),
                d.getReceiverId(),
                rName,
                d.getProductId(),
                pName,
                bNo,
                whName,
                d.getQuantity(),
                d.getPurchasePrice(),
                d.getTotalValue(),
                d.getConditionNote(),
                d.getStatus(),
                d.getDonationDate(),
                d.getCreatedAt()
        );
    }
}
