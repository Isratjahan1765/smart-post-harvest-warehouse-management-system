package com.sphwms.warehouse.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "code_number", nullable = false, unique = true)
    private Integer codeNumber;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Column(name = "in_charge", nullable = false, length = 100)
    private String inCharge;

    @Column(name = "capacity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
