package com.sphwms.transportcost.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_cost")
@Getter
@Setter
public class TripCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_cost_id")
    private Long tripCostId;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "fuel_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal fuelCost = BigDecimal.ZERO;

    @Column(name = "driver_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal driverCost = BigDecimal.ZERO;

    @Column(name = "other_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
