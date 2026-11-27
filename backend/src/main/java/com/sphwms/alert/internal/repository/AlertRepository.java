package com.sphwms.alert.internal.repository;

import com.sphwms.alert.internal.entity.Alert;
import com.sphwms.alert.internal.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByDedupeKey(String dedupeKey);

    List<Alert> findByResolvedAtIsNullOrderByIsReadAscAlertIdDesc();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.resolvedAt IS NULL AND a.isRead = false")
    long countUnreadActiveAlerts();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.resolvedAt IS NULL AND a.type = :type")
    long countActiveByType(@Param("type") AlertType type);

    @Query("SELECT a FROM Alert a WHERE a.resolvedAt IS NULL")
    List<Alert> findAllActive();
}
