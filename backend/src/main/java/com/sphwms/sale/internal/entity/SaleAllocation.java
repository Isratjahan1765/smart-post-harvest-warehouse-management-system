package com.sphwms.sale.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_allocation")
@Getter
@Setter
public class SaleAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alloc_id")
    private Long allocId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
}
