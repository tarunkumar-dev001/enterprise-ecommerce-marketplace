package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.dto.VendorBestSellingProductResponse;
import com.ecommerce.marketplace.entity.OrderItem;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    interface VendorSalesMetricsProjection {
        BigDecimal getTotalSalesRevenue();

        Long getOrderCount();

        Long getUnitsSold();
    }

    @Query("select coalesce(sum(item.unitPrice * item.quantity), 0) as totalSalesRevenue, "
            + "count(distinct item.order.id) as orderCount, coalesce(sum(item.quantity), 0) as unitsSold "
            + "from OrderItem item where item.product.vendor.id = :vendorId "
            + "and item.order.paymentStatus = :paymentStatus and item.order.status <> :excludedStatus")
    VendorSalesMetricsProjection getVendorSalesMetrics(
            @Param("vendorId") Long vendorId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("excludedStatus") OrderStatus excludedStatus);

    @Query("select new com.ecommerce.marketplace.dto.VendorBestSellingProductResponse("
            + "item.product.id, item.product.name, sum(item.quantity), sum(item.unitPrice * item.quantity)) "
            + "from OrderItem item where item.product.vendor.id = :vendorId "
            + "and item.order.paymentStatus = :paymentStatus and item.order.status <> :excludedStatus "
            + "group by item.product.id, item.product.name "
            + "order by sum(item.quantity) desc, sum(item.unitPrice * item.quantity) desc, item.product.name asc")
    List<VendorBestSellingProductResponse> findBestSellingProducts(
            @Param("vendorId") Long vendorId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("excludedStatus") OrderStatus excludedStatus,
            Pageable pageable);
}