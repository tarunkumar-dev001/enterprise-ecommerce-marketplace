package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.AdminProductResponse;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;
import com.ecommerce.marketplace.exception.BusinessException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Service
public class ProductModerationService {

    private static final Logger auditLog = LoggerFactory.getLogger("com.ecommerce.marketplace.audit.ProductModeration");

    private final ProductRepository productRepository;

    public ProductModerationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public AdminProductResponse approve(Long productId, String administrator) {
        return transition(productId, ProductModerationStatus.APPROVED, null,
                Set.of(ProductModerationStatus.PENDING_REVIEW, ProductModerationStatus.REJECTED), administrator);
    }

    @Transactional
    public AdminProductResponse reject(Long productId, String reason, String administrator) {
        return transition(productId, ProductModerationStatus.REJECTED, reason,
                Set.of(ProductModerationStatus.PENDING_REVIEW), administrator);
    }

    @Transactional
    public AdminProductResponse suspend(Long productId, String reason, String administrator) {
        return transition(productId, ProductModerationStatus.SUSPENDED, reason,
                Set.of(ProductModerationStatus.APPROVED), administrator);
    }

    /** A suspension is reversible only by returning the listing to normal review. */
    @Transactional
    public AdminProductResponse reinstate(Long productId, String reason, String administrator) {
        return transition(productId, ProductModerationStatus.PENDING_REVIEW, reason,
                Set.of(ProductModerationStatus.SUSPENDED), administrator);
    }

    private AdminProductResponse transition(
            Long productId,
            ProductModerationStatus targetStatus,
            String reason,
            Set<ProductModerationStatus> allowedSources,
            String administrator) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductModerationStatus currentStatus = product.getModerationStatus();
        if (currentStatus != null && !allowedSources.contains(currentStatus)) {
            throw new BusinessException(
                    "Product cannot move from " + currentStatus + " to " + targetStatus,
                    HttpStatus.CONFLICT);
        }
        if (currentStatus == null && targetStatus != ProductModerationStatus.APPROVED) {
            throw new BusinessException("A product without a moderation status must be approved before moderation", HttpStatus.CONFLICT);
        }

        product.setModerationStatus(targetStatus);
        product.setModerationReason(reason == null ? null : reason.trim());
        Product saved = productRepository.save(product);
        auditLog.info("product_moderation_transition administrator={} productId={} from={} to={} reason={}",
                administrator, saved.getId(), currentStatus, targetStatus, saved.getModerationReason());
        return AdminProductResponse.from(saved);
    }
}
