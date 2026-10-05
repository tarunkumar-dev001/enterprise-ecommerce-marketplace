package com.ecommerce.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_ledger", indexes = {
        @Index(name = "idx_vendor_ledger_vendor_status", columnList = "vendor_id,payout_status"),
        @Index(name = "idx_vendor_ledger_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class VendorLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id", nullable = false, unique = true)
    private SubOrder subOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOrderAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommissionFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netVendorPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;

    @Version
    private long version;
}
