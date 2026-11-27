package com.sphwms.buyer.internal.repository;

import com.sphwms.buyer.internal.entity.Buyer;
import com.sphwms.buyer.internal.enums.BuyerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, Long> {

    List<Buyer> findByStatusOrderByBuyerIdAsc(BuyerStatus status);

    Optional<Buyer> findByPhone(String phone);

    @Query("SELECT b FROM Buyer b WHERE b.phone = :phone AND b.buyerId <> :id")
    Optional<Buyer> findByPhoneAndIdNot(@Param("phone") String phone, @Param("id") Long id);
}
