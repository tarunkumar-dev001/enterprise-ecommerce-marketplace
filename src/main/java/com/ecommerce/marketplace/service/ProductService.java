package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.ProductRequest;
import com.ecommerce.marketplace.dto.ProductPageResponse;
import com.ecommerce.marketplace.dto.ProductDetailsResponse;
import com.ecommerce.marketplace.dto.ProductResponse;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.persistence.criteria.Predicate;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getModerationStatus() != ProductModerationStatus.APPROVED) {
            throw new ResourceNotFoundException("Product not found");
        }
        return ProductDetailsResponse.from(product);
    }

    @Transactional
    public ProductResponse createProduct(String username, ProductRequest request) {
        User vendor = findVendor(username);
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        product.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        product.setVendor(vendor);
        product.setModerationStatus(ProductModerationStatus.PENDING_REVIEW);
        product.setModerationReason(null);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(String username, Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getVendor().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only update your own products");
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        product.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        if (product.getModerationStatus() == ProductModerationStatus.APPROVED) {
            product.setModerationStatus(ProductModerationStatus.PENDING_REVIEW);
            product.setModerationReason(null);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String username) {
        User vendor = findVendor(username);
        return productRepository.findByVendorOrderByIdDesc(vendor)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductPageResponse browseProducts(
            String search,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("Price filters must be zero or greater");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price");
        }

        Set<String> sortableFields = Set.of("id", "name", "price", "category", "stock");
        String sortField = sortBy == null ? "id" : sortBy.trim().toLowerCase(Locale.ROOT);
        if (!sortableFields.contains(sortField)) {
            throw new IllegalArgumentException("sortBy must be one of: id, name, price, category, stock");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction == null ? "desc" : direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("direction must be 'asc' or 'desc'");
        }

        Specification<Product> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("moderationStatus"), ProductModerationStatus.APPROVED));
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                criteriaBuilder.coalesce(root.get("description"), "")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), searchPattern)));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT)));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<ProductResponse> products = productRepository
                .findAll(specification, PageRequest.of(page, size, Sort.by(sortDirection, sortField)))
                .map(ProductResponse::from);
        return new ProductPageResponse(
                products.getContent(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast(),
                sortField,
                sortDirection.name());
    }

    private User findVendor(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getRoles().contains("ROLE_VENDOR"))
                .orElseThrow(() -> new AccessDeniedException("Only vendors can manage products"));
    }
}

