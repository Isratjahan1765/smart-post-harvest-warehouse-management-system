package com.sphwms.driver.internal.entity;

import com.sphwms.driver.internal.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver")
@Getter
@Setter
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "license_no", nullable = false, unique = true, length = 30)
    private String licenseNo;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "phone", nullable = false, unique = true, length = 11)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "vehicle_no", nullable = false, length = 30)
    private String vehicleNo;

    @Column(name = "picture_path", length = 255)
    private String picturePath;

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
