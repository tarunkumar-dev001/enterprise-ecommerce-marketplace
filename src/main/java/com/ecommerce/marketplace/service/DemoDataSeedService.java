package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.dto.ProductReviewRequest;
import com.ecommerce.marketplace.dto.CartItemRequest;
import com.ecommerce.marketplace.dto.WishlistItemRequest;
import com.ecommerce.marketplace.entity.*;
import com.ecommerce.marketplace.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Adds a fixed, repeatable interview/demo marketplace dataset when the dev profile is enabled. */
@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeedService.class);
    private static final String ORDER_MARKER = "DEMO-ORDER-";
    private static final List<String> REVIEW_TEXTS = List.of(
            "Excellent build quality and fast delivery. Works exactly as described.",
            "Good product for the price. I have been using it every day.",
            "The material feels premium and the finish is very neat.",
            "Looks great at home and is straightforward to use.",
            "Solid value. Packaging was secure and delivery was on time.",
            "A few small issues, but overall it is a useful product.",
            "Comfortable, well made, and true to the product description.",
            "Good quality and thoughtful details. Would recommend it.",
            "It does the job well, though the instructions could be clearer.",
            "Very happy with the purchase and the quality for this price.");
    private static final int[] REVIEW_RATINGS = {5, 4, 5, 3, 4, 2, 5, 4, 3, 5};

    private static final List<ProductSeed> PRODUCT_SEEDS = List.of(
            new ProductSeed("Nova X Pro Smartphone", "A bright OLED display, all-day battery and a versatile triple camera system.", "49999", 18, "Smartphones", 0, "technova-smartphone.svg"),
            new ProductSeed("TechNova AirBook 14", "A lightweight 14-inch work laptop with a sharp display and fast solid-state storage.", "64999", 12, "Laptops", 0, "technova-laptop.svg"),
            new ProductSeed("Wireless Noise Cancelling Headphones", "Comfortable over-ear audio with adaptive noise reduction and long battery life.", "7999", 34, "Audio", 0, "technova-headphones.svg"),
            new ProductSeed("Mechanical RGB Keyboard", "A compact mechanical keyboard with tactile switches and adjustable backlighting.", "3499", 8, "Computer accessories", 0, "technova-keyboard.svg"),
            new ProductSeed("Wireless Gaming Mouse", "An ergonomic wireless mouse with adjustable sensitivity and a rechargeable battery.", "1999", 42, "Computer accessories", 0, "technova-mouse.svg"),
            new ProductSeed("USB-C Hub 7-in-1", "A travel-friendly USB-C hub with HDMI, card readers and multiple USB ports.", "1299", 7, "Computer accessories", 0, "technova-usb-hub.svg"),
            new ProductSeed("Smart Fitness Band", "A water-resistant fitness band that tracks daily activity, sleep and heart rate.", "2999", 23, "Smart devices", 0, "technova-fitness-band.svg"),
            new ProductSeed("27-inch 4K Monitor", "A color-rich 4K monitor with an adjustable stand for work and entertainment.", "27999", 11, "Computer accessories", 0, "technova-monitor.svg"),
            new ProductSeed("Classic Cotton T-Shirt", "A soft, breathable cotton crew-neck tee with a clean everyday fit.", "799", 46, "T-shirts", 1, "urbanstyle-tshirt.svg"),
            new ProductSeed("Premium Denim Jeans", "Durable stretch denim in a versatile straight fit for daily wear.", "1999", 16, "Jeans", 1, "urbanstyle-jeans.svg"),
            new ProductSeed("Casual Running Shoes", "Lightweight running shoes with a cushioned footbed and grippy outsole.", "2499", 9, "Shoes", 1, "urbanstyle-running-shoes.svg"),
            new ProductSeed("Urban Hoodie", "A mid-weight fleece hoodie with a relaxed fit and reinforced seams.", "1799", 28, "Jackets", 1, "urbanstyle-hoodie.svg"),
            new ProductSeed("Leather Finish Backpack", "A roomy, structured backpack with padded laptop storage and secure pockets.", "2199", 6, "Bags", 1, "urbanstyle-backpack.svg"),
            new ProductSeed("Minimal Analog Watch", "A refined analog watch with a clear dial and an adjustable everyday strap.", "3499", 14, "Watches and accessories", 1, "urbanstyle-watch.svg"),
            new ProductSeed("Casual Bomber Jacket", "A versatile lightweight bomber jacket with a smooth lining and secure pockets.", "2999", 19, "Jackets", 1, "urbanstyle-bomber-jacket.svg"),
            new ProductSeed("Slim Fit Shirt", "A crisp cotton-blend shirt with a modern slim silhouette and button cuffs.", "1499", 5, "Shirts", 1, "urbanstyle-shirt.svg"),
            new ProductSeed("Ceramic Dinner Set", "A durable glazed ceramic dinner set for everyday meals and relaxed hosting.", "2499", 13, "Kitchen", 2, "homenest-dinnerware.svg"),
            new ProductSeed("Smart LED Table Lamp", "A dimmable LED table lamp with warm and cool light settings.", "1899", 31, "Lighting", 2, "homenest-table-lamp.svg"),
            new ProductSeed("Non-Stick Cookware Set", "A practical cookware set with even heat distribution and easy-clean coating.", "3999", 10, "Kitchen", 2, "homenest-cookware.svg"),
            new ProductSeed("Wooden Storage Organizer", "A compact wood-finish organizer for keeping shelves and worktops tidy.", "999", 37, "Storage", 2, "homenest-storage-organizer.svg"),
            new ProductSeed("Electric Kettle", "A quick-boil stainless steel kettle with automatic shut-off and a cool-touch handle.", "1299", 7, "Small appliances", 2, "homenest-kettle.svg"),
            new ProductSeed("Decorative Wall Clock", "A quiet wall clock with a minimal face and a warm natural finish.", "1199", 18, "Home decor", 2, "homenest-wall-clock.svg"),
            new ProductSeed("Modern Cushion Set", "A pair of textured cushion covers with soft inserts for living spaces.", "899", 25, "Home decor", 2, "homenest-cushions.svg"),
            new ProductSeed("Kitchen Spice Rack", "A space-saving spice rack with labeled jars for an organized kitchen.", "799", 4, "Kitchen", 2, "homenest-spice-rack.svg"));

    private static final List<String> CUSTOMER_EMAILS = List.of(
            "rahul@marketplace.demo", "priya@marketplace.demo", "ananya@marketplace.demo",
            "rohan@marketplace.demo", "sneha@marketplace.demo");

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewRepository reviewRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final VendorLedgerService ledgerService;
    private final ProductReviewService productReviewService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataSeedService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            ProductReviewRepository reviewRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            VendorLedgerService ledgerService,
            ProductReviewService productReviewService,
            CartService cartService,
            WishlistService wishlistService,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-data.password}") String demoPassword) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.ledgerService = ledgerService;
        this.productReviewService = productReviewService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Transactional
    public SeedSummary seed() {
        Map<String, User> users = new LinkedHashMap<>();
        users.put("admin", user("admin", "admin@marketplace.demo", "ROLE_ADMIN"));
        users.put("technova", user("TechNova Store", "technova@marketplace.demo", "ROLE_VENDOR"));
        users.put("urbanstyle", user("UrbanStyle", "urbanstyle@marketplace.demo", "ROLE_VENDOR"));
        users.put("homenest", user("HomeNest", "homenest@marketplace.demo", "ROLE_VENDOR"));
        users.put("rahul", user("rahul", CUSTOMER_EMAILS.get(0), "ROLE_USER"));
        users.put("priya", user("priya", CUSTOMER_EMAILS.get(1), "ROLE_USER"));
        users.put("ananya", user("ananya", CUSTOMER_EMAILS.get(2), "ROLE_USER"));
        users.put("rohan", user("rohan", CUSTOMER_EMAILS.get(3), "ROLE_USER"));
        users.put("sneha", user("sneha", CUSTOMER_EMAILS.get(4), "ROLE_USER"));

        List<User> vendors = List.of(users.get("technova"), users.get("urbanstyle"), users.get("homenest"));
        List<Product> products = seedProducts(vendors);
        List<Order> orders = seedOrders(users, vendors, products);
        seedReviews(products, orders);
        seedCartAndWishlist(users, products);

        SeedSummary summary = summarize(users, vendors, products, orders);
        log.info("Development demo data enabled. {}", summary);
        log.info("Demo logins (username / email): admin / admin@marketplace.demo; "
                + "TechNova Store / technova@marketplace.demo; UrbanStyle / urbanstyle@marketplace.demo; "
                + "HomeNest / homenest@marketplace.demo; rahul / rahul@marketplace.demo; "
                + "priya / priya@marketplace.demo; ananya / ananya@marketplace.demo; "
                + "rohan / rohan@marketplace.demo; sneha / sneha@marketplace.demo.");
        return summary;
    }

    private User user(String username, String email, String role) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.getUsername().equals(username)) {
                throw new IllegalStateException("Demo email " + email + " belongs to username "
                        + user.getUsername() + "; refusing to change an account outside the demo account list.");
            }
            if (!user.getRoles().contains(role)) {
                throw new IllegalStateException("Demo account " + email + " exists without expected role " + role
                        + "; refusing to change existing account data.");
            }
            if (!passwordEncoder.matches(demoPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(demoPassword));
                return userRepository.save(user);
            }
            return user;
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Demo username " + username + " is already used by another account.");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private List<Product> seedProducts(List<User> vendors) {
        List<Product> products = new ArrayList<>();
        for (ProductSeed seed : PRODUCT_SEEDS) {
            User vendor = vendors.get(seed.vendorIndex());
            Product product = productRepository.findByVendorAndName(vendor, seed.name()).orElseGet(() -> {
                Product created = new Product();
                created.setName(seed.name());
                created.setDescription(seed.description());
                created.setPrice(new BigDecimal(seed.price()));
                created.setStock(seed.stock());
                created.setCategory(seed.category());
                created.setVendor(vendor);
                created.setImageUrl("/assets/products/" + seed.imageFile());
                created.setModerationStatus(ProductModerationStatus.APPROVED);
                return productRepository.save(created);
            });
            if (product.getModerationStatus() != ProductModerationStatus.APPROVED) {
                product.setModerationStatus(ProductModerationStatus.APPROVED);
                product.setModerationReason(null);
                product = productRepository.save(product);
            }
            String expectedImageUrl = "/assets/products/" + seed.imageFile();
            if (!hasUsableImageReference(product.getImageUrl())
                    || isMismatchedDemoAsset(product.getImageUrl(), expectedImageUrl)) {
                product.setImageUrl(expectedImageUrl);
                product = productRepository.save(product);
            }
            products.add(product);
        }
        return products;
    }

    private List<Order> seedOrders(Map<String, User> users, List<User> vendors, List<Product> products) {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < 25; index++) {
            String marker = ORDER_MARKER + String.format(Locale.ROOT, "%03d", index + 1);
            Optional<Order> existing = orderRepository.findByPaymentReference(marker);
            if (existing.isPresent()) {
                orders.add(existing.get());
                continue;
            }

            Order order = new Order();
            order.setUser(orderCustomer(index, users, vendors));
            order.setPlacedAt(now.minusHours((long) ((60 - index * 2.5) * 24)));
            order.setPaymentReference(marker);
            order.setStatus(orderStatus(index));
            order.setPaymentStatus(order.getStatus() == OrderStatus.CANCELLED
                    ? PaymentStatus.FAILED
                    : order.getStatus() == OrderStatus.PLACED ? PaymentStatus.PENDING : PaymentStatus.PAID);

            Map<Long, SubOrder> subOrdersByVendor = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            List<LineSeed> lines = orderLines(index);
            for (LineSeed line : lines) {
                Product product = products.get(line.productIndex());
                if (product.getStock() < line.quantity()) {
                    throw new IllegalStateException("Insufficient seed stock for " + product.getName());
                }
                product.setStock(product.getStock() - line.quantity());
                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(line.quantity()));
                SubOrder subOrder = subOrdersByVendor.computeIfAbsent(product.getVendor().getId(), ignored -> {
                    SubOrder created = new SubOrder();
                    created.setOrder(order);
                    created.setVendor(product.getVendor());
                    created.setStatus(order.getStatus());
                    order.getSubOrders().add(created);
                    return created;
                });
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setSubOrder(subOrder);
                item.setProduct(product);
                item.setQuantity(line.quantity());
                item.setUnitPrice(product.getPrice());
                order.getItems().add(item);
                subOrder.getItems().add(item);
                subOrder.setTotalAmount(subOrder.getTotalAmount().add(lineTotal));
                total = total.add(lineTotal);
            }
            order.setSubtotalAmount(total);
            order.setDiscountAmount(BigDecimal.ZERO.setScale(2));
            order.setTotalAmount(total);
            for (SubOrder subOrder : subOrdersByVendor.values()) {
                ledgerService.attachPendingRecord(subOrder);
                VendorLedger ledger = subOrder.getLedger();
                ledger.setCreatedAt(order.getPlacedAt());
                if (order.getPaymentStatus() == PaymentStatus.PAID && index % 2 == 0) {
                    ledger.setPayoutStatus(PayoutStatus.PAID);
                    ledger.setPaidAt(order.getPlacedAt().plusDays(2).isAfter(now)
                            ? now : order.getPlacedAt().plusDays(2));
                }
            }
            orders.add(orderRepository.saveAndFlush(order));
        }
        return orders;
    }

    private User orderCustomer(int index, Map<String, User> users, List<User> vendors) {
        if (index < 1) return users.get("rahul");
        if (index < 4) return users.get("priya");
        if (index < 8) return users.get("ananya");
        if (index < 15) return users.get("rohan");
        if (index < 22) return users.get("sneha");
        return vendors.get(index - 22);
    }

    private OrderStatus orderStatus(int index) {
        return switch (index % 5) {
            case 0 -> OrderStatus.DELIVERED;
            case 1 -> OrderStatus.SHIPPED;
            case 2 -> OrderStatus.PROCESSING;
            case 3 -> OrderStatus.PLACED;
            default -> OrderStatus.CANCELLED;
        };
    }

    private List<LineSeed> orderLines(int index) {
        int qty = index % 7 == 0 ? 2 : 1;
        List<LineSeed> lines = new ArrayList<>();
        switch (index % 3) {
            case 0 -> {
                lines.add(new LineSeed(index % 8, qty));
                lines.add(new LineSeed(8 + ((index + 3) % 8), 1));
                lines.add(new LineSeed(16 + ((index + 5) % 8), 1));
            }
            case 1 -> {
                lines.add(new LineSeed(index % 8, qty));
                lines.add(new LineSeed(8 + ((index * 2) % 8), 1));
            }
            default -> {
                int vendorBase = ((index / 3) % 3) * 8;
                lines.add(new LineSeed(vendorBase + ((index * 5) % 8), qty));
            }
        }
        if (index % 4 == 0) {
            int firstProduct = lines.get(0).productIndex();
            int sameVendorProduct = (firstProduct / 8) * 8 + ((firstProduct + 1) % 8);
            lines.add(new LineSeed(sameVendorProduct, 1));
        }
        return lines;
    }

    private void seedReviews(List<Product> products, List<Order> orders) {
        int created = 0;
        long existingForDemoProducts = products.stream()
                .mapToLong(product -> reviewRepository.findByProduct_IdOrderByCreatedAtDescIdDesc(product.getId()).size())
                .sum();
        for (Order order : orders) {
            if (existingForDemoProducts + created >= 20) break;
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (reviewRepository.existsByProductAndCustomer(product, order.getUser())) continue;
                int reviewIndex = created % REVIEW_TEXTS.size();
                productReviewService.createReview(order.getUser().getUsername(), product.getId(),
                        new ProductReviewRequest(REVIEW_RATINGS[reviewIndex], REVIEW_TEXTS.get(reviewIndex)));
                created++;
                if (existingForDemoProducts + created >= 20) break;
            }
        }
        if (existingForDemoProducts + created < 15) {
            throw new IllegalStateException("Could only seed " + (existingForDemoProducts + created)
                    + " verified reviews for demo products.");
        }
    }

    private void seedCartAndWishlist(Map<String, User> users, List<Product> products) {
        addCartItem(users.get("rohan"), products.get(4), 1);
        addCartItem(users.get("sneha"), products.get(16), 1);
        addWishlistItem(users.get("priya"), products.get(2));
        addWishlistItem(users.get("ananya"), products.get(10));
    }

    private void addCartItem(User user, Product product, int quantity) {
        Optional<Cart> existingCart = cartRepository.findByUser(user);
        if (existingCart.isPresent() && cartItemRepository.findByCartAndProduct(existingCart.get(), product).isPresent()) {
            return;
        }
        cartService.addItemToCart(user.getUsername(), new CartItemRequest(product.getId(), quantity));
    }

    private void addWishlistItem(User user, Product product) {
        Wishlist wishlist = wishlistRepository.findByUser(user).orElse(null);
        if (wishlist != null && wishlistItemRepository.existsByWishlistAndProduct_Id(wishlist, product.getId())) {
            return;
        }
        wishlistService.addItem(user.getUsername(), new WishlistItemRequest(product.getId()));
    }

    private SeedSummary summarize(Map<String, User> users, List<User> vendors, List<Product> products, List<Order> orders) {
        long subOrders = orders.stream().mapToLong(order -> order.getSubOrders().size()).sum();
        long orderItems = orders.stream().mapToLong(order -> order.getItems().size()).sum();
        long ledgerEntries = orders.stream().flatMap(order -> order.getSubOrders().stream())
                .filter(subOrder -> subOrder.getLedger() != null).count();
        long paidPayouts = orders.stream().flatMap(order -> order.getSubOrders().stream())
                .filter(subOrder -> subOrder.getLedger() != null
                        && subOrder.getLedger().getPayoutStatus() == PayoutStatus.PAID).count();
        long reviewCount = products.stream()
                .mapToLong(product -> reviewRepository.findByProduct_IdOrderByCreatedAtDescIdDesc(product.getId()).size())
                .sum();
        return new SeedSummary(users.size(), vendors.size(), CUSTOMER_EMAILS.size(), vendors.size(), products.size(),
                orders.size(), subOrders, orderItems, reviewCount, ledgerEntries, paidPayouts, ledgerEntries - paidPayouts);
    }

    public record SeedSummary(long users, long vendors, long customers, long stores, long products, long orders,
                              long subOrders, long orderItems, long reviews, long ledgerEntries,
                              long paidPayouts, long pendingPayouts) { }

    private static boolean hasUsableImageReference(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;
        String value = imageUrl.trim();
        if (value.startsWith("/")) return true;
        try {
            java.net.URI uri = java.net.URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isMismatchedDemoAsset(String imageUrl, String expectedImageUrl) {
        return imageUrl != null
                && imageUrl.trim().startsWith("/assets/products/")
                && !expectedImageUrl.equals(imageUrl.trim());
    }

    private record ProductSeed(String name, String description, String price, int stock, String category, int vendorIndex, String imageFile) { }
    private record LineSeed(int productIndex, int quantity) { }
}
