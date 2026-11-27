package com.sphwms.buyer.internal.repository;

import com.sphwms.buyer.internal.entity.DuePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DuePaymentRepository extends JpaRepository<DuePayment, Long> {
    List<DuePayment> findByBuyerIdOrderByPaymentDateDesc(Long buyerId);
}
