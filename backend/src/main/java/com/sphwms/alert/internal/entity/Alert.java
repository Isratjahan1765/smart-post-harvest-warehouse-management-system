package com.sphwms.alert.internal.entity;

import com.sphwms.alert.internal.enums.AlertEntityType;
import com.sphwms.alert.internal.enums.AlertType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AlertEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "details", nullable = false, length = 255)
    private String details;

    @Column(name = "dedupe_key", nullable = false, unique = true, length = 100)
    private String dedupeKey;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "read_by")
    private Long readBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
