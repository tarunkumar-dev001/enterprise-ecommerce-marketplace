package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.AdminLedgerSummaryResponse;
import com.ecommerce.marketplace.dto.VendorLedgerRecordResponse;
import com.ecommerce.marketplace.dto.VendorLedgerSummaryResponse;
import com.ecommerce.marketplace.entity.PayoutStatus;
import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.SubOrder;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.entity.VendorLedger;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.UserRepository;
import com.ecommerce.marketplace.repository.VendorLedgerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Service
public class VendorLedgerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final VendorLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final BigDecimal commissionRate;

    public VendorLedgerService(
            VendorLedgerRepository ledgerRepository,
            UserRepository userRepository,
            @Value("${marketplace.commission.rate:0.10}") BigDecimal commissionRate) {
        if (commissionRate.signum() < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("marketplace.commission.rate must be between 0 and 1");
        }
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.commissionRate = commissionRate;
    }

    public void attachPendingRecord(SubOrder subOrder) {
        BigDecimal total = subOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal commission = total.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        VendorLedger record = new VendorLedger();
        record.setSubOrder(subOrder);
        record.setVendor(subOrder.getVendor());
        record.setTotalOrderAmount(total);
        record.setPlatformCommissionFee(commission);
        record.setNetVendorPayout(total.subtract(commission));
        record.setPayoutStatus(PayoutStatus.PENDING);
        subOrder.setLedger(record);
    }

    @Transactional(readOnly = true)
    public VendorLedgerSummaryResponse getVendorLedger(String username) {
        User vendor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return summarizeVendor(vendor, ledgerRepository.findByVendorOrderByCreatedAtDesc(vendor));
    }

    @Transactional(readOnly = true)
    public AdminLedgerSummaryResponse getAllLedgerRecords() {
        List<VendorLedger> records = ledgerRepository.findAllByOrderByCreatedAtDesc();
        return new AdminLedgerSummaryResponse(
                sum(records, VendorLedger::getTotalOrderAmount),
                sum(records, VendorLedger::getPlatformCommissionFee),
                sum(records, VendorLedger::getNetVendorPayout),
                sum(records.stream().filter(this::isEligiblePending).toList(),
                        VendorLedger::getNetVendorPayout),
                sum(records.stream().filter(record -> record.getPayoutStatus() == PayoutStatus.PAID).toList(),
                        VendorLedger::getNetVendorPayout),
                records.stream().map(VendorLedgerRecordResponse::from).toList());
    }

    @Transactional
    public VendorLedgerSummaryResponse payPendingRecords(Long vendorId) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        if (!vendor.getRoles().contains("ROLE_VENDOR")) {
            throw new ResourceNotFoundException("Vendor not found");
        }

        List<VendorLedger> eligible = ledgerRepository.findVendorRecordsForPayout(vendorId, PayoutStatus.PENDING)
                .stream().filter(this::isEligiblePending).toList();
        LocalDateTime paidAt = LocalDateTime.now();
        eligible.forEach(record -> {
            record.setPayoutStatus(PayoutStatus.PAID);
            record.setPaidAt(paidAt);
        });
        ledgerRepository.saveAll(eligible);
        return summarizeVendor(vendor, ledgerRepository.findByVendorOrderByCreatedAtDesc(vendor));
    }

    private VendorLedgerSummaryResponse summarizeVendor(User vendor, List<VendorLedger> records) {
        return new VendorLedgerSummaryResponse(
                vendor.getId(),
                vendor.getUsername(),
                sum(records, VendorLedger::getTotalOrderAmount),
                sum(records, VendorLedger::getPlatformCommissionFee),
                sum(records, VendorLedger::getNetVendorPayout),
                sum(records.stream().filter(this::isEligiblePending).toList(),
                        VendorLedger::getNetVendorPayout),
                sum(records.stream().filter(record -> record.getPayoutStatus() == PayoutStatus.PAID).toList(),
                        VendorLedger::getNetVendorPayout),
                records.stream().map(VendorLedgerRecordResponse::from).toList());
    }

    private boolean isEligiblePending(VendorLedger record) {
        if (record.getPayoutStatus() != PayoutStatus.PENDING) return false;
        Order order = record.getSubOrder().getOrder();
        return record.getSubOrder().getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.CANCELLED
                && order.getPaymentStatus() == PaymentStatus.PAID;
    }

    private BigDecimal sum(List<VendorLedger> records, Function<VendorLedger, BigDecimal> value) {
        return records.stream().map(value).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
