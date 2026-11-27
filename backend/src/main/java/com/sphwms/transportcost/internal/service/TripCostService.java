package com.sphwms.transportcost.internal.service;

import com.sphwms.driver.internal.entity.Driver;
import com.sphwms.driver.internal.repository.DriverRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.dto.PageResponse;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import com.sphwms.transportcost.internal.dto.requests.TripCostRequest;
import com.sphwms.transportcost.internal.dto.responses.AvailableTripResponse;
import com.sphwms.transportcost.internal.dto.responses.MonthlyTripCostResponse;
import com.sphwms.transportcost.internal.dto.responses.TripCostResponse;
import com.sphwms.transportcost.internal.entity.TripCost;
import com.sphwms.transportcost.internal.repository.TripCostRepository;
import com.sphwms.trip.internal.entity.Trip;
import com.sphwms.trip.internal.enums.TripStatus;
import com.sphwms.trip.internal.repository.TripRepository;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripCostService {

    private final TripCostRepository tripCostRepository;
    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MonthlyTripCostResponse> getMonthlyTransportCost() {
        List<TripCost> costs = tripCostRepository.findAll();
        Map<Long, Trip> tripMap = tripRepository.findAll().stream()
                .collect(Collectors.toMap(Trip::getTripId, t -> t));

        // Last 6 months up to current month
        YearMonth currentYm = YearMonth.now();
        List<YearMonth> last6Months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            last6Months.add(currentYm.minusMonths(i));
        }

        Map<YearMonth, BigDecimal> monthlyTotals = new HashMap<>();
        for (YearMonth ym : last6Months) {
            monthlyTotals.put(ym, BigDecimal.ZERO);
        }

        for (TripCost tc : costs) {
            Trip t = tripMap.get(tc.getTripId());
            if (t != null && t.getTripDate() != null) {
                YearMonth ym = YearMonth.from(t.getTripDate());
                if (monthlyTotals.containsKey(ym)) {
                    monthlyTotals.put(ym, monthlyTotals.get(ym).add(tc.getTotalCost()));
                }
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        return last6Months.stream()
                .map(ym -> new MonthlyTripCostResponse(ym.format(fmt), monthlyTotals.get(ym)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<TripCostResponse> getAllTripCosts(String q, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 50) size = 50;

        List<TripCostResponse> all = getFilteredTripCosts(q);

        int totalElements = all.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (page - 1) * size;
        List<TripCostResponse> content;
        if (fromIndex >= totalElements) {
            content = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            content = all.subList(fromIndex, toIndex);
        }

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    @Transactional(readOnly = true)
    public List<AvailableTripResponse> getAvailableTrips() {
        List<Trip> trips = tripRepository.findDeliveredTripsWithoutCost();
        Map<Long, String> driverNames = driverRepository.findAll().stream()
                .collect(Collectors.toMap(Driver::getDriverId, Driver::getName, (a, b) -> a));

        return trips.stream()
                .map(t -> new AvailableTripResponse(
                        t.getTripId(),
                        t.getTripNo(),
                        driverNames.getOrDefault(t.getDriverId(), "Driver #" + t.getDriverId()),
                        t.getOrigin(),
                        t.getDestination(),
                        t.getTripDate()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public TripCostResponse createTripCost(TripCostRequest req) {
        Trip trip = tripRepository.findById(req.tripId())
                .orElseThrow(() -> new BusinessException("Trip not found."));

        if (trip.getStatus() != TripStatus.DELIVERED) {
            throw new BusinessException("Only delivered trips can have a cost.");
        }

        if (tripCostRepository.findByTripId(req.tripId()).isPresent()) {
            throw new BusinessException("Cost already added for this trip.");
        }

        BigDecimal fuel = req.fuelCost() != null ? req.fuelCost() : BigDecimal.ZERO;
        BigDecimal driver = req.driverCost() != null ? req.driverCost() : BigDecimal.ZERO;
        BigDecimal other = req.otherCost() != null ? req.otherCost() : BigDecimal.ZERO;

        if (fuel.compareTo(BigDecimal.ZERO) < 0 || driver.compareTo(BigDecimal.ZERO) < 0 || other.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Amount cannot be negative.");
        }

        BigDecimal total = fuel.add(driver).add(other);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total cost must be greater than zero.");
        }

        TripCost tc = new TripCost();
        tc.setTripId(trip.getTripId());
        tc.setFuelCost(fuel);
        tc.setDriverCost(driver);
        tc.setOtherCost(other);
        tc.setTotalCost(total);
        tc.setNote(req.note() != null ? req.note().trim() : null);
        tc.setCreatedBy(currentUserProvider.getCurrentUserId());
        tc.setCreatedAt(LocalDateTime.now());

        TripCost saved = tripCostRepository.save(tc);
        auditService.log("TRIP_COST_CREATE", "TRIP_COST", saved.getTripCostId(),
                "Recorded cost for trip " + trip.getTripNo() + " (Total: " + total + ")");

        String driverName = driverRepository.findById(trip.getDriverId()).map(Driver::getName).orElse("");
        return toResponse(saved, trip, driverName);
    }

    @Transactional
    public TripCostResponse updateTripCost(Long id, TripCostRequest req) {
        TripCost tc = tripCostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip cost not found."));

        BigDecimal fuel = req.fuelCost() != null ? req.fuelCost() : BigDecimal.ZERO;
        BigDecimal driver = req.driverCost() != null ? req.driverCost() : BigDecimal.ZERO;
        BigDecimal other = req.otherCost() != null ? req.otherCost() : BigDecimal.ZERO;

        if (fuel.compareTo(BigDecimal.ZERO) < 0 || driver.compareTo(BigDecimal.ZERO) < 0 || other.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Amount cannot be negative.");
        }

        BigDecimal total = fuel.add(driver).add(other);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total cost must be greater than zero.");
        }

        tc.setFuelCost(fuel);
        tc.setDriverCost(driver);
        tc.setOtherCost(other);
        tc.setTotalCost(total);
        if (req.note() != null) tc.setNote(req.note().trim());
        tc.setUpdatedBy(currentUserProvider.getCurrentUserId());
        tc.setUpdatedAt(LocalDateTime.now());

        TripCost saved = tripCostRepository.save(tc);
        auditService.log("TRIP_COST_UPDATE", "TRIP_COST", saved.getTripCostId(),
                "Updated cost for trip cost #" + saved.getTripCostId());

        Trip trip = tripRepository.findById(saved.getTripId()).orElse(null);
        String driverName = (trip != null)
                ? driverRepository.findById(trip.getDriverId()).map(Driver::getName).orElse("")
                : "";

        return toResponse(saved, trip, driverName);
    }

    @Transactional
    public void deleteTripCost(Long id) {
        TripCost tc = tripCostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip cost not found."));

        tripCostRepository.delete(tc);
        auditService.log("TRIP_COST_DELETE", "TRIP_COST", id,
                "Deleted cost #" + id);
    }

    @Transactional(readOnly = true)
    public byte[] exportTripCostsToXlsx(String q) {
        List<TripCostResponse> list = getFilteredTripCosts(q);
        if (list.isEmpty()) {
            throw new BusinessException("No data to export.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Trip Costs");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] columns = {"Trip", "Date", "Driver", "Route", "Fuel (Tk)", "Driver Cost (Tk)", "Other Cost (Tk)", "Total (Tk)", "Note"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal fuelSum = BigDecimal.ZERO;
            BigDecimal driverSum = BigDecimal.ZERO;
            BigDecimal otherSum = BigDecimal.ZERO;
            BigDecimal totalSum = BigDecimal.ZERO;

            for (TripCostResponse c : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.tripNo());
                row.createCell(1).setCellValue(c.tripDate() != null ? c.tripDate().toString() : "");
                row.createCell(2).setCellValue(c.driverName());
                row.createCell(3).setCellValue(c.origin() + " - " + c.destination());
                row.createCell(4).setCellValue(c.fuelCost().doubleValue());
                row.createCell(5).setCellValue(c.driverCost().doubleValue());
                row.createCell(6).setCellValue(c.otherCost().doubleValue());
                row.createCell(7).setCellValue(c.totalCost().doubleValue());
                row.createCell(8).setCellValue(c.note() != null ? c.note() : "");

                fuelSum = fuelSum.add(c.fuelCost());
                driverSum = driverSum.add(c.driverCost());
                otherSum = otherSum.add(c.otherCost());
                totalSum = totalSum.add(c.totalCost());
            }

            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(headerStyle);
            totalRow.createCell(4).setCellValue(fuelSum.doubleValue());
            totalRow.createCell(5).setCellValue(driverSum.doubleValue());
            totalRow.createCell(6).setCellValue(otherSum.doubleValue());
            totalRow.createCell(7).setCellValue(totalSum.doubleValue());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            auditService.log("TRIP_COST_EXPORT", "TRIP_COST", null, "Exported trip costs to XLSX");
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate the export file.");
        }
    }

    private List<TripCostResponse> getFilteredTripCosts(String q) {
        List<TripCost> costs = tripCostRepository.findAll();
        Map<Long, Trip> tripMap = tripRepository.findAll().stream()
                .collect(Collectors.toMap(Trip::getTripId, t -> t));
        Map<Long, String> driverNames = driverRepository.findAll().stream()
                .collect(Collectors.toMap(Driver::getDriverId, Driver::getName, (a, b) -> a));

        String query = q != null ? q.trim().toLowerCase() : "";

        return costs.stream()
                .map(tc -> {
                    Trip t = tripMap.get(tc.getTripId());
                    String drvName = (t != null) ? driverNames.getOrDefault(t.getDriverId(), "") : "";
                    return toResponse(tc, t, drvName);
                })
                .filter(res -> {
                    if (query.isEmpty()) return true;
                    return res.tripNo().toLowerCase().contains(query) ||
                           res.driverName().toLowerCase().contains(query);
                })
                .sorted((a, b) -> {
                    if (a.tripDate() != null && b.tripDate() != null) {
                        int cmp = b.tripDate().compareTo(a.tripDate());
                        if (cmp != 0) return cmp;
                    }
                    return b.tripCostId().compareTo(a.tripCostId());
                })
                .collect(Collectors.toList());
    }

    private TripCostResponse toResponse(TripCost tc, Trip t, String driverName) {
        return new TripCostResponse(
                tc.getTripCostId(),
                tc.getTripId(),
                t != null ? t.getTripNo() : "T-?",
                driverName,
                t != null ? t.getOrigin() : "",
                t != null ? t.getDestination() : "",
                tc.getFuelCost(),
                tc.getDriverCost(),
                tc.getOtherCost(),
                tc.getTotalCost(),
                tc.getNote(),
                t != null ? t.getTripDate() : null,
                tc.getCreatedAt()
        );
    }
}
