package com.sphwms.trip.internal.repository;

import com.sphwms.trip.internal.entity.Trip;
import com.sphwms.trip.internal.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByOrderByTripIdDesc();

    List<Trip> findByStatus(TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.status = 'DELIVERED' AND t.tripId NOT IN (SELECT tc.tripId FROM TripCost tc) ORDER BY t.tripDate DESC, t.tripId DESC")
    List<Trip> findDeliveredTripsWithoutCost();
}
