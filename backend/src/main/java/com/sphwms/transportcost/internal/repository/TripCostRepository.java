package com.sphwms.transportcost.internal.repository;

import com.sphwms.transportcost.internal.entity.TripCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripCostRepository extends JpaRepository<TripCost, Long> {
    Optional<TripCost> findByTripId(Long tripId);
}
