package com.sphwms.sale.internal.repository;

import com.sphwms.sale.internal.entity.SaleAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleAllocationRepository extends JpaRepository<SaleAllocation, Long> {
    List<SaleAllocation> findBySaleId(Long saleId);
}
