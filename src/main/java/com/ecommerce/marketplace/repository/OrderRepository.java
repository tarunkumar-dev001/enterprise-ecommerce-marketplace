package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<Order> findByUserOrderByPlacedAtDesc(User user);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<Order> findByIdAndUser(Long id, User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from Order order where order.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from Order order where order.id = :orderId and order.user = :customer")
    Optional<Order> findByIdAndUserForUpdate(
            @Param("orderId") Long orderId,
            @Param("customer") User customer);

    Optional<Order> findByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from Order order where order.id = :orderId and order.user = :customer")
    Optional<Order> findByIdAndUserForPayment(
            @Param("orderId") Long orderId,
            @Param("customer") User customer);

        @Query("select case when count(item) > 0 then true else false end "
            + "from Order o join o.items item "
            + "where o.user.id = :customerId and item.product.id = :productId and o.status <> :excludedStatus")
        boolean hasPurchasedProduct(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId,
            @Param("excludedStatus") OrderStatus excludedStatus);
}
