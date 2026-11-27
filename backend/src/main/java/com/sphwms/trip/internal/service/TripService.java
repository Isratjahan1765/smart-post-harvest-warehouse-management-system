package com.sphwms.trip.internal.service;

import com.sphwms.driver.internal.entity.Driver;
import com.sphwms.driver.internal.repository.DriverRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.BusinessException;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.sequence.IdSequenceService;
import com.sphwms.trip.internal.dto.requests.TripRequest;
import com.sphwms.trip.internal.dto.responses.TripResponse;
import com.sphwms.trip.internal.entity.Trip;
import com.sphwms.trip.internal.enums.TripStatus;
import com.sphwms.trip.internal.repository.TripRepository;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final WarehouseRepository warehouseRepository;
    private final IdSequenceService idSequenceService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TripResponse> getAllTrips() {
        List<Trip> trips = tripRepository.findAllByOrderByTripIdDesc();
        Map<Long, String> driverNames = driverRepository.findAll().stream()
                .collect(Collectors.toMap(Driver::getDriverId, Driver::getName, (a, b) -> a));
        Map<Long, String> whNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (a, b) -> a));

        return trips.stream()
                .map(t -> toResponse(t, driverNames, whNames))
                .collect(Collectors.toList());
    }

    @Transactional
    public TripResponse createTrip(TripRequest req) {
        Driver driver = driverRepository.findById(req.driverId())
                .orElseThrow(() -> new BusinessException("Driver not found."));
        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new BusinessException("Cannot assign inactive driver to a trip.");
        }

        if (req.warehouseId() != null) {
            Warehouse wh = warehouseRepository.findById(req.warehouseId())
                    .orElseThrow(() -> new BusinessException("Warehouse not found."));
            if (!Boolean.TRUE.equals(wh.getActive())) {
                throw new BusinessException("Cannot assign inactive warehouse to a trip.");
            }
        }

        String tripNo = idSequenceService.nextTripNo();

        Trip trip = new Trip();
        trip.setTripNo(tripNo);
        trip.setDriverId(driver.getDriverId());
        trip.setOrigin(req.origin().trim());
        trip.setDestination(req.destination().trim());
        trip.setTripDate(req.tripDate());
        trip.setStatus(req.status() != null ? req.status() : TripStatus.PLANNED);
        trip.setWarehouseId(req.warehouseId());
        trip.setCreatedAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        auditService.log("TRIP_CREATE", "TRIP", saved.getTripId(),
                "Created trip " + saved.getTripNo() + " with driver " + driver.getName());

        String whName = req.warehouseId() != null
                ? warehouseRepository.findById(req.warehouseId()).map(Warehouse::getName).orElse("")
                : "";

        return toResponse(saved, Map.of(driver.getDriverId(), driver.getName()), Map.of(saved.getWarehouseId() != null ? saved.getWarehouseId() : -1L, whName));
    }

    @Transactional
    public TripResponse updateStatus(Long id, TripStatus status) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip not found."));

        trip.setStatus(status);
        Trip saved = tripRepository.save(trip);

        auditService.log("TRIP_STATUS", "TRIP", saved.getTripId(),
                "Set trip " + saved.getTripNo() + " status to " + status);

        String driverName = driverRepository.findById(saved.getDriverId()).map(Driver::getName).orElse("");
        String whName = saved.getWarehouseId() != null
                ? warehouseRepository.findById(saved.getWarehouseId()).map(Warehouse::getName).orElse("")
                : "";

        return toResponse(saved, Map.of(saved.getDriverId(), driverName), Map.of(saved.getWarehouseId() != null ? saved.getWarehouseId() : -1L, whName));
    }

    private TripResponse toResponse(Trip t, Map<Long, String> driverNames, Map<Long, String> whNames) {
        return new TripResponse(
                t.getTripId(),
                t.getTripNo(),
                t.getDriverId(),
                driverNames.getOrDefault(t.getDriverId(), "Driver #" + t.getDriverId()),
                t.getOrigin(),
                t.getDestination(),
                t.getTripDate(),
                t.getStatus(),
                t.getWarehouseId(),
                t.getWarehouseId() != null ? whNames.getOrDefault(t.getWarehouseId(), "") : null
        );
    }
}
