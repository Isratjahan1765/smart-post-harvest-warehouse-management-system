package com.sphwms.arrival.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quality_check")
@Getter
@Setter
public class QualityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Long checkId;

    @Column(name = "arrival_id", nullable = false, unique = true)
    private Long arrivalId;

    @Column(name = "packaging_ok", nullable = false)
    private Boolean packagingOk;

    @Column(name = "no_damage", nullable = false)
    private Boolean noDamage;

    @Column(name = "quantity_match", nullable = false)
    private Boolean quantityMatch;

    @Column(name = "expiry_valid", nullable = false)
    private Boolean expiryValid;

    @Column(name = "grade_match", nullable = false)
    private Boolean gradeMatch;

    @Column(name = "checked_by", nullable = false)
    private Long checkedBy;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();
}
