package com.sphwms.donation.internal.entity;

import com.sphwms.donation.internal.enums.DonationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation")
@Getter
@Setter
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Long donationId;

    @Column(name = "donation_no", nullable = false, unique = true, length = 20)
    private String donationNo;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "total_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "condition_note", length = 255)
    private String conditionNote;

    @Column(name = "safe_confirmed", nullable = false)
    private Boolean safeConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DonationStatus status = DonationStatus.CONFIRMED;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Column(name = "donated_by", nullable = false)
    private Long donatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
