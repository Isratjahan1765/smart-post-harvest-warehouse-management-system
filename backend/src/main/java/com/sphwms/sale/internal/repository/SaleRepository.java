package com.sphwms.sale.internal.repository;

import com.sphwms.sale.internal.entity.Sale;
import com.sphwms.sale.internal.enums.PaymentStatus;
import com.sphwms.sale.internal.enums.SaleStatus;
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
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByOrderBySaleIdDesc();

    List<Sale> findByBuyerIdOrderBySaleIdDesc(Long buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.saleId = :id")
    Optional<Sale> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.buyerId = :buyerId AND s.paymentStatus = 'DUE' AND s.status = 'CONFIRMED' ORDER BY s.saleDate ASC, s.saleId ASC")
    List<Sale> findDueSalesByBuyerWithLock(@Param("buyerId") Long buyerId);

    @Query("SELECT s FROM Sale s WHERE s.paymentStatus = 'DUE' AND s.status = 'CONFIRMED' AND s.dueDate < :today")
    List<Sale> findOverdueSales(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(s.totalAmount - s.paidAmount), 0) FROM Sale s WHERE s.status = 'CONFIRMED' AND s.paymentStatus = 'DUE'")
    BigDecimal sumTotalDue();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.status = 'CONFIRMED' AND s.saleDate >= :start AND s.saleDate <= :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(sa.quantity * s.rate), 0) FROM SaleAllocation sa JOIN Sale s ON s.saleId = sa.saleId " +
           "WHERE s.status = 'CONFIRMED' AND sa.warehouseId = :whId AND s.saleDate >= :start AND s.saleDate <= :end")
    BigDecimal sumRevenueBetweenForWarehouse(@Param("whId") Long whId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
