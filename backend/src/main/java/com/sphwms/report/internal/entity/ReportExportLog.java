package com.sphwms.report.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_export_log")
@Getter
@Setter
public class ReportExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "export_id")
    private Long exportId;

    @Column(name = "report_type", nullable = false, length = 40)
    private String reportType;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "exported_by", nullable = false)
    private Long exportedBy;

    @Column(name = "exported_at", nullable = false, updatable = false)
    private LocalDateTime exportedAt = LocalDateTime.now();
}
