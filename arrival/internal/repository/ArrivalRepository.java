package com.sphwms.arrival.internal.repository;

import com.sphwms.arrival.internal.entity.Arrival;
import com.sphwms.arrival.internal.enums.ArrivalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArrivalRepository extends JpaRepository<Arrival, Long> {

    List<Arrival> findAllByOrderByArrivalIdDesc();

    List<Arrival> findByStatusOrderByArrivalIdDesc(ArrivalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Arrival a WHERE a.arrivalId = :id")
    Optional<Arrival> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(a) FROM Arrival a WHERE a.status = :status")
    long countByStatus(@Param("status") ArrivalStatus status);

    @Query("SELECT COUNT(a) FROM Arrival a WHERE a.status = 'CONFIRMED' AND a.confirmedAt >= :start AND a.confirmedAt < :end")
    long countConfirmedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM Arrival a WHERE a.status = 'CONFIRMED' AND a.confirmedAt >= :start AND a.confirmedAt < :end")
    java.math.BigDecimal sumConfirmedCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM Arrival a WHERE a.status = 'CONFIRMED' AND a.warehouseId = :whId AND a.confirmedAt >= :start AND a.confirmedAt < :end")
    java.math.BigDecimal sumConfirmedCostBetweenForWarehouse(@Param("whId") Long whId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
