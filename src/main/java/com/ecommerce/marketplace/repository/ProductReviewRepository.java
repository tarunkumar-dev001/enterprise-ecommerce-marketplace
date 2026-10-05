package com.ecommerce.marketplace.repository;

import com.ecommerce.marketplace.entity.ProductReview;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByProductAndCustomer(Product product, User customer);

    List<ProductReview> findByProduct_IdOrderByCreatedAtDescIdDesc(Long productId);

    long countByProduct_Id(Long productId);

    @Query("select avg(review.rating) from ProductReview review where review.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("select review from ProductReview review join fetch review.product product "
            + "join fetch review.customer where product.vendor.id = :vendorId "
            + "order by review.createdAt desc, review.id desc")
    List<ProductReview> findAllForVendor(@Param("vendorId") Long vendorId);
}
