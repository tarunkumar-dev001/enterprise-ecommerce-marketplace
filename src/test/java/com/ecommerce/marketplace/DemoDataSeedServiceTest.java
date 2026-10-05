package com.ecommerce.marketplace;

import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.PayoutStatus;
import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.repository.OrderRepository;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.ProductReviewRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import com.ecommerce.marketplace.repository.VendorLedgerRepository;
import com.ecommerce.marketplace.service.DemoDataSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.RoundingMode;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seed;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=bG9jYWwtdGVzdC1zaWduaW5nLWtleS1tdXN0LWJlLTMyaGFyYWN0ZXJz",
        "app.demo-data.enabled=true",
        "app.demo-data.password=Demo@12345"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DemoDataSeedServiceTest {

    @Autowired private DemoDataSeedService seedService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductReviewRepository reviewRepository;
    @Autowired private VendorLedgerRepository ledgerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mockMvc;

    @Test
    @Transactional
    void seedsUsersResetsOnlyKnownDemoPasswordsAndDoesNotDuplicateData() throws Exception {
        assertEquals(9, userRepository.count(), "The first application seed creates the demo accounts");

        User realUser = new User();
        realUser.setUsername("unrelated-real-user");
        realUser.setEmail("unrelated-real-user@example.test");
        realUser.setPassword(passwordEncoder.encode("Unchanged@123"));
        realUser.setRoles(Set.of("ROLE_USER"));
        userRepository.saveAndFlush(realUser);
        Map<String, Set<String>> rolesBefore = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUsername, user -> Set.copyOf(user.getRoles())));

        User techNova = userRepository.findByEmail("technova@marketplace.demo").orElseThrow();
        assertTrue(passwordEncoder.matches("Demo@12345", techNova.getPassword()));
        techNova.setPassword(passwordEncoder.encode("OldManualPassword@123"));
        userRepository.saveAndFlush(techNova);
        Product productWithExistingImage = productRepository.findByVendorOrderByIdDesc(techNova).get(0);
        productWithExistingImage.setImageUrl("https://images.example.test/real-product-photo.jpg");
        productRepository.saveAndFlush(productWithExistingImage);
        User homeNest = userRepository.findByEmail("homenest@marketplace.demo").orElseThrow();
        Product productWithMismatchedDemoImage = productRepository.findByVendorAndName(homeNest, "Kitchen Spice Rack")
                .orElseThrow();
        productWithMismatchedDemoImage.setImageUrl("/assets/products/homenest-table-lamp.svg");
        productRepository.saveAndFlush(productWithMismatchedDemoImage);

        DemoDataSeedService.SeedSummary secondRun = seedService.seed();

        assertEquals(10, userRepository.count());
        assertEquals(9, userRepository.findAll().stream()
                .filter(user -> user.getEmail().endsWith("@marketplace.demo")).count());
        assertEquals(rolesBefore, userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUsername, user -> Set.copyOf(user.getRoles()))));
        assertEquals(24, productRepository.count());
        assertTrue(productRepository.findAll().stream().allMatch(product ->
                product.getImageUrl() != null && !product.getImageUrl().isBlank()));
        assertEquals("https://images.example.test/real-product-photo.jpg",
                productRepository.findById(productWithExistingImage.getId()).orElseThrow().getImageUrl(),
                "A valid existing image URL is preserved by the demo seed");
        assertEquals("/assets/products/homenest-spice-rack.svg",
                productRepository.findById(productWithMismatchedDemoImage.getId()).orElseThrow().getImageUrl(),
                "A demo product pointing at another demo illustration is repaired");
        assertTrue(productRepository.findAll().stream()
                .filter(product -> !product.getId().equals(productWithExistingImage.getId()))
                .allMatch(product -> product.getImageUrl().startsWith("/assets/products/")));
        assertEquals(3, userRepository.findAll().stream().filter(user -> user.getRoles().contains("ROLE_VENDOR")).count());
        assertEquals(5, userRepository.findAll().stream()
                .filter(user -> user.getEmail().endsWith("@marketplace.demo") && user.getRoles().contains("ROLE_USER"))
                .count());
        assertTrue(userRepository.findByEmail("admin@marketplace.demo").orElseThrow()
                .getRoles().contains("ROLE_ADMIN"));
        List<Order> demoOrders = orderRepository.findAll().stream()
                .filter(order -> order.getPaymentReference() != null
                        && order.getPaymentReference().startsWith("DEMO-ORDER-"))
                .toList();
        assertEquals(25, demoOrders.size());
        assertEquals(51, demoOrders.stream().mapToLong(order -> order.getSubOrders().size()).sum());
        assertEquals(58, demoOrders.stream().mapToLong(order -> order.getItems().size()).sum());
        assertEquals(20, reviewRepository.count());
        assertEquals(51, ledgerRepository.count());
        assertEquals(17, ledgerRepository.findAll().stream().filter(ledger -> ledger.getPayoutStatus() == PayoutStatus.PAID).count());
        assertEquals(34, ledgerRepository.findAll().stream().filter(ledger -> ledger.getPayoutStatus() == PayoutStatus.PENDING).count());
        assertTrue(ledgerRepository.findAll().stream().anyMatch(ledger -> ledger.getPayoutStatus() == PayoutStatus.PAID));
        assertTrue(ledgerRepository.findAll().stream().anyMatch(ledger -> ledger.getPayoutStatus() == PayoutStatus.PENDING));

        assertTrue(demoOrders.stream().anyMatch(order -> order.getSubOrders().size() == 3));
        assertTrue(demoOrders.stream().allMatch(order -> order.getTotalAmount().compareTo(order.getSubtotalAmount()) == 0
                && order.getItems().stream().allMatch(item -> item.getQuantity() > 0 && item.getProduct().getStock() >= 0)));
        assertEquals(Set.of("DELIVERED", "SHIPPED", "PROCESSING", "PLACED", "CANCELLED"),
                demoOrders.stream().map(order -> order.getStatus().name()).collect(Collectors.toSet()));
        assertEquals(Set.of("PAID", "PENDING", "FAILED"),
                demoOrders.stream().map(order -> order.getPaymentStatus().name()).collect(Collectors.toSet()));
        for (Order order : demoOrders) {
            for (var subOrder : order.getSubOrders()) {
                assertTrue(subOrder.getItems().stream().allMatch(item ->
                        item.getProduct().getVendor().getId().equals(subOrder.getVendor().getId())));
                assertEquals(subOrder.getTotalAmount(), subOrder.getItems().stream()
                        .map(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            }
        }
        for (Product product : productRepository.findAll()) {
            assertTrue(product.getStock() >= 0);
            assertEquals(reviewRepository.countByProduct_Id(product.getId()), product.getReviewCount());
            assertEquals(reviewRepository.findAverageRatingByProductId(product.getId()) == null ? 0.0
                            : reviewRepository.findAverageRatingByProductId(product.getId()),
                    product.getAverageRating().doubleValue(), 0.01);
            reviewRepository.findByProduct_IdOrderByCreatedAtDescIdDesc(product.getId()).forEach(review ->
                    assertTrue(orderRepository.hasPurchasedProduct(review.getCustomer().getId(), product.getId(),
                            com.ecommerce.marketplace.entity.OrderStatus.CANCELLED)));
        }
        for (var ledger : ledgerRepository.findAll()) {
            assertEquals(ledger.getSubOrder().getTotalAmount(), ledger.getTotalOrderAmount());
            assertEquals(ledger.getTotalOrderAmount().subtract(ledger.getPlatformCommissionFee()), ledger.getNetVendorPayout());
            assertEquals(ledger.getTotalOrderAmount().multiply(new java.math.BigDecimal("0.10"))
                            .setScale(2, RoundingMode.HALF_UP), ledger.getPlatformCommissionFee());
        }

        User vendor = userRepository.findByEmail("technova@marketplace.demo").orElseThrow();
        assertTrue(vendor.getRoles().contains("ROLE_VENDOR"));
        assertFalse(vendor.getRoles().contains("ROLE_ADMIN"));
        assertTrue(passwordEncoder.matches("Demo@12345", vendor.getPassword()));
        assertFalse(passwordEncoder.matches("OldManualPassword@123", vendor.getPassword()));
        assertFalse(vendor.getPassword().equals("Demo@12345"), "Demo passwords remain BCrypt hashes in storage");
        userRepository.findAll().stream().filter(user -> user.getEmail().endsWith("@marketplace.demo"))
                .forEach(user -> {
                    assertTrue(passwordEncoder.matches("Demo@12345", user.getPassword()), user.getUsername());
                    assertFalse(user.getPassword().equals("Demo@12345"), user.getUsername());
                });
        assertFalse(userRepository.findByEmail("admin@marketplace.demo").orElseThrow()
                .getRoles().contains("ROLE_VENDOR"));
        assertEquals(1, orderRepository.findByUserOrderByPlacedAtDesc(vendor).size(),
                "A vendor account also has a customer purchase history");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"TechNova Store\",\"password\":\"Demo@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles").isNotEmpty());
        User unchangedRealUser = userRepository.findByEmail("unrelated-real-user@example.test").orElseThrow();
        assertTrue(passwordEncoder.matches("Unchanged@123", unchangedRealUser.getPassword()));
        assertEquals(Set.of("ROLE_USER"), unchangedRealUser.getRoles());
        for (Product product : productRepository.findByVendorOrderByIdDesc(vendor)) {
            assertTrue(product.getStock() >= 0);
        }
        assertEquals(25, secondRun.orders());
        assertEquals(51, secondRun.subOrders());
        assertEquals(58, secondRun.orderItems());
        assertEquals(20, secondRun.reviews());
        assertEquals(51, secondRun.ledgerEntries());
    }
}
