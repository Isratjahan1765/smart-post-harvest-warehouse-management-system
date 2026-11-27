package com.sphwms.donation.internal.repository;

import com.sphwms.donation.internal.entity.Receiver;
import com.sphwms.donation.internal.enums.ReceiverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiverRepository extends JpaRepository<Receiver, Long> {
    List<Receiver> findByStatusOrderByNameAsc(ReceiverStatus status);
}
