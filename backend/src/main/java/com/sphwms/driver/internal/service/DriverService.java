package com.sphwms.driver.internal.service;

import com.sphwms.driver.internal.dto.requests.DriverRequest;
import com.sphwms.driver.internal.dto.responses.DriverOptionResponse;
import com.sphwms.driver.internal.dto.responses.DriverResponse;
import com.sphwms.driver.internal.entity.Driver;
import com.sphwms.driver.internal.repository.DriverRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.dto.PageResponse;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final EntityManager entityManager;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[3-9]\\d{8}$");

    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> getAllDrivers(String q, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 6;
        if (size > 50) size = 50;

        List<Driver> all = driverRepository.findAllByOrderByDriverIdAsc();

        String query = q != null ? q.trim() : "";
        String statusFilter = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";

        final boolean isPhoneQuery = !query.isEmpty() && query.matches("^[0-9+\\- ]+$");
        final String digitsOnlyQuery = query.replaceAll("[^0-9]", "");
        final String lowerQuery = query.toLowerCase();

        List<DriverResponse> filtered = all.stream()
                .filter(d -> {
                    if ("ACTIVE".equals(statusFilter)) return Boolean.TRUE.equals(d.getActive());
                    if ("INACTIVE".equals(statusFilter)) return Boolean.FALSE.equals(d.getActive());
                    return true;
                })
                .filter(d -> {
                    if (query.isEmpty()) return true;
                    if (isPhoneQuery) {
                        return d.getPhone() != null && d.getPhone().contains(digitsOnlyQuery);
                    } else {
                        return (d.getName() != null && d.getName().toLowerCase().contains(lowerQuery)) ||
                               (d.getLicenseNo() != null && d.getLicenseNo().toLowerCase().contains(lowerQuery));
                    }
                })
                .map(this::toResponse)
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (page - 1) * size;
        List<DriverResponse> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = List.of();
        } else {
            int toIndex = Math.min(fromIndex + size, totalElements);
            pageContent = filtered.subList(fromIndex, toIndex);
        }

        return new PageResponse<>(pageContent, totalElements, totalPages, page, size);
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {
        Driver d = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found."));
        return toResponse(d);
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest req) {
        String name = req.name() != null ? req.name().trim().replaceAll("\\s+", " ") : "";
        String licenseNo = req.licenseNo() != null ? req.licenseNo().trim().toUpperCase() : "";
        String vehicleNo = req.vehicleNo() != null ? req.vehicleNo().trim().toUpperCase() : "";
        String phone = normalizePhone(req.phone());

        if (driverRepository.findByLicenseNo(licenseNo).isPresent()) {
            throw new BusinessException("License number already exists.");
        }
        if (driverRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("Phone number already exists.");
        }

        Driver d = new Driver();
        d.setName(name);
        d.setLicenseNo(licenseNo);
        d.setLicenseExpiry(req.licenseExpiry());
        d.setPhone(phone);
        d.setVehicleType(req.vehicleType());
        d.setVehicleNo(vehicleNo);
        d.setActive(true);
        d.setCreatedBy(currentUserProvider.getCurrentUserId());
        d.setCreatedAt(LocalDateTime.now());

        Driver saved = driverRepository.save(d);
        auditService.log("DRIVER_CREATE", "DRIVER", saved.getDriverId(),
                "Created driver " + saved.getName() + " (" + saved.getLicenseNo() + ")");

        return toResponse(saved);
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest req) {
        Driver d = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found."));

        String name = req.name() != null ? req.name().trim().replaceAll("\\s+", " ") : "";
        String licenseNo = req.licenseNo() != null ? req.licenseNo().trim().toUpperCase() : "";
        String vehicleNo = req.vehicleNo() != null ? req.vehicleNo().trim().toUpperCase() : "";
        String phone = normalizePhone(req.phone());

        if (driverRepository.findByLicenseNoAndIdNot(licenseNo, id).isPresent()) {
            throw new BusinessException("License number already exists.");
        }
        if (driverRepository.findByPhoneAndIdNot(phone, id).isPresent()) {
            throw new BusinessException("Phone number already exists.");
        }

        d.setName(name);
        d.setLicenseNo(licenseNo);
        d.setLicenseExpiry(req.licenseExpiry());
        d.setPhone(phone);
        d.setVehicleType(req.vehicleType());
        d.setVehicleNo(vehicleNo);
        d.setUpdatedBy(currentUserProvider.getCurrentUserId());
        d.setUpdatedAt(LocalDateTime.now());

        Driver saved = driverRepository.save(d);
        auditService.log("DRIVER_UPDATE", "DRIVER", saved.getDriverId(),
                "Updated driver " + saved.getName());

        return toResponse(saved);
    }

    @Transactional
    public DriverResponse toggleStatus(Long id) {
        Driver d = driverRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Driver not found."));

        boolean currentlyActive = Boolean.TRUE.equals(d.getActive());
        if (currentlyActive) {
            // Ongoing trip check
            Long ongoingTrips = (Long) entityManager.createQuery(
                    "SELECT COUNT(t) FROM Trip t WHERE t.driverId = :drvId AND t.status IN ('PLANNED', 'ONGOING')")
                    .setParameter("drvId", id)
                    .getSingleResult();
            if (ongoingTrips != null && ongoingTrips > 0) {
                throw new BusinessException("Cannot deactivate: driver has an ongoing trip.");
            }
            d.setActive(false);
        } else {
            d.setActive(true);
        }

        d.setUpdatedBy(currentUserProvider.getCurrentUserId());
        d.setUpdatedAt(LocalDateTime.now());
        Driver saved = driverRepository.save(d);

        auditService.log("DRIVER_STATUS", "DRIVER", saved.getDriverId(),
                "Set driver " + saved.getName() + " active=" + saved.getActive());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DriverOptionResponse> getDriverOptions(boolean includeInactive) {
        List<Driver> list = includeInactive
                ? driverRepository.findAllByOrderByDriverIdAsc()
                : driverRepository.findByActiveTrueOrderByDriverIdAsc();

        return list.stream()
                .map(d -> new DriverOptionResponse(
                        d.getDriverId(),
                        d.getName(),
                        d.getVehicleType() + " " + d.getVehicleNo(),
                        Boolean.TRUE.equals(d.getActive())
                ))
                .collect(Collectors.toList());
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            throw new BusinessException("Phone number is invalid.");
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.startsWith("880")) {
            digits = digits.substring(2);
        }
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw new BusinessException("Phone number is invalid.");
        }
        return digits;
    }

    private DriverResponse toResponse(Driver d) {
        return new DriverResponse(
                d.getDriverId(),
                d.getUserId(),
                d.getName(),
                d.getLicenseNo(),
                d.getLicenseExpiry(),
                d.getPhone(),
                d.getVehicleType(),
                d.getVehicleNo(),
                Boolean.TRUE.equals(d.getActive())
        );
    }
}
