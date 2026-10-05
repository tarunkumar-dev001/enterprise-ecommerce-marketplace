package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByVendor(User vendor);
    List<Product> findByVendorOrderByIdDesc(User vendor);
    java.util.Optional<Product> findByVendorAndName(User vendor, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :productId")
    java.util.Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

}
