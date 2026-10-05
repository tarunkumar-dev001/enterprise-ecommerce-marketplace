package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.ProductReviewRequest;
import com.ecommerce.marketplace.dto.ProductReviewResponse;
import com.ecommerce.marketplace.entity.OrderStatus;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductReview;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.BusinessException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.OrderRepository;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.ProductReviewRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ProductReviewService(
            ProductReviewRepository productReviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderRepository orderRepository) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ProductReviewResponse createReview(String username, Long productId, ProductReviewRequest request) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!orderRepository.hasPurchasedProduct(customer.getId(), productId, OrderStatus.CANCELLED)) {
            throw new BusinessException("You can only review products you have purchased", HttpStatus.FORBIDDEN);
        }
        if (productReviewRepository.existsByProductAndCustomer(product, customer)) {
            throw new BusinessException("You have already reviewed this product", HttpStatus.CONFLICT);
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(request.rating());
        review.setText(request.text().trim());
        ProductReview savedReview = productReviewRepository.saveAndFlush(review);
        Double averageRating = productReviewRepository.findAverageRatingByProductId(productId);
        product.setAverageRating(BigDecimal.valueOf(averageRating == null ? 0.0 : averageRating)
                .setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount(productReviewRepository.countByProduct_Id(productId));
        productRepository.save(product);
        return ProductReviewResponse.from(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getProductReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return productReviewRepository.findByProduct_IdOrderByCreatedAtDescIdDesc(productId).stream()
                .map(ProductReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getVendorReviews(String username) {
        User vendor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        if (!vendor.getRoles().contains("ROLE_VENDOR")) {
            throw new AccessDeniedException("Only vendors can view vendor reviews");
        }
        return productReviewRepository.findAllForVendor(vendor.getId()).stream()
                .map(ProductReviewResponse::from)
                .toList();
    }
}
