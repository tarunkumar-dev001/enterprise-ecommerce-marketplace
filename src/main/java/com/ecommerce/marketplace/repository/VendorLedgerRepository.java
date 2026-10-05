package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.PayoutStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.entity.VendorLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorLedgerRepository extends JpaRepository<VendorLedger, Long> {

    @EntityGraph(attributePaths = {"vendor", "subOrder", "subOrder.order"})
    List<VendorLedger> findByVendorOrderByCreatedAtDesc(User vendor);

    @EntityGraph(attributePaths = {"vendor", "subOrder", "subOrder.order"})
    List<VendorLedger> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ledger from VendorLedger ledger where ledger.vendor.id = :vendorId "
            + "and ledger.payoutStatus = :status")
    List<VendorLedger> findVendorRecordsForPayout(
            @Param("vendorId") Long vendorId,
            @Param("status") PayoutStatus status);
}
