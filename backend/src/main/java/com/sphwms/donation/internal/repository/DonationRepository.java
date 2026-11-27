package com.sphwms.donation.internal.repository;

import com.sphwms.donation.internal.entity.Donation;
import com.sphwms.donation.internal.enums.DonationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findAllByOrderByDonationIdDesc();

    List<Donation> findByReceiverIdOrderByDonationIdDesc(Long receiverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.donationId = :id")
    Optional<Donation> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(d.totalValue), 0) FROM Donation d WHERE d.status = 'CONFIRMED' AND d.donationDate >= :start AND d.donationDate <= :end")
    BigDecimal sumDonatedValueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(d.totalValue), 0) FROM Donation d WHERE d.status = 'CONFIRMED' AND d.warehouseId = :whId AND d.donationDate >= :start AND d.donationDate <= :end")
    BigDecimal sumDonatedValueBetweenForWarehouse(@Param("whId") Long whId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Donation> findByStatus(DonationStatus status);
}
