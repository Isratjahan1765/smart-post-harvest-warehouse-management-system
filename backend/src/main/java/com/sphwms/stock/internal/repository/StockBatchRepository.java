package com.sphwms.stock.internal.repository;

import com.sphwms.stock.internal.entity.StockBatch;
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
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    @Query("SELECT b.warehouseId, SUM(b.remainingQuantity), SUM(b.remainingQuantity * b.unitCost) " +
           "FROM StockBatch b WHERE b.remainingQuantity > 0 GROUP BY b.warehouseId")
    List<Object[]> findStockSummaryByWarehouse();

    @Query("SELECT b.warehouseId, b.productId, SUM(b.remainingQuantity) " +
           "FROM StockBatch b WHERE b.remainingQuantity > 0 GROUP BY b.warehouseId, b.productId")
    List<Object[]> findStockBreakdown();

    @Query("SELECT b.productId, SUM(b.remainingQuantity), SUM(b.remainingQuantity * b.unitCost) " +
           "FROM StockBatch b WHERE b.remainingQuantity > 0 GROUP BY b.productId")
    List<Object[]> findStockSummaryByProduct();

    @Query("SELECT b.productId, b.warehouseId, SUM(b.remainingQuantity) " +
           "FROM StockBatch b WHERE b.remainingQuantity > 0 GROUP BY b.productId, b.warehouseId")
    List<Object[]> findProductStockByWarehouse();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBatch b WHERE b.productId = :productId AND b.warehouseId = :warehouseId " +
           "AND b.remainingQuantity > 0 AND b.expiryDate > :today ORDER BY b.expiryDate ASC, b.batchId ASC")
    List<StockBatch> findAvailableBatchesForSaleWithLock(@Param("productId") Long productId,
                                                         @Param("warehouseId") Long warehouseId,
                                                         @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBatch b WHERE b.batchId = :batchId")
    Optional<StockBatch> findByIdWithLock(@Param("batchId") Long batchId);

    List<StockBatch> findByWarehouseIdAndRemainingQuantityGreaterThan(Long warehouseId, BigDecimal minQty);

    Optional<StockBatch> findByArrivalId(Long arrivalId);
}
