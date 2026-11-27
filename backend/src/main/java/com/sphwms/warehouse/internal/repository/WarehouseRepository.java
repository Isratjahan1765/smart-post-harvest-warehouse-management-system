package com.sphwms.warehouse.internal.repository;

import com.sphwms.warehouse.internal.entity.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOrderByCodeNumberAsc();

    Optional<Warehouse> findByNameIgnoreCase(String name);

    @Query("SELECT w FROM Warehouse w WHERE LOWER(w.name) = LOWER(:name) AND w.warehouseId <> :id")
    Optional<Warehouse> findByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Warehouse w WHERE w.warehouseId = :id")
    Optional<Warehouse> findByIdWithLock(@Param("id") Long id);

    List<Warehouse> findByActiveTrueOrderByCodeNumberAsc();
}
