package com.sphwms.arrival.internal.repository;

import com.sphwms.arrival.internal.entity.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {
    Optional<QualityCheck> findByArrivalId(Long arrivalId);
}
