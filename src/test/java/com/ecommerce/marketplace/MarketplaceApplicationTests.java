package com.ecommerce.marketplace;

import com.ecommerce.marketplace.entity.Product;
import com.ecommerce.marketplace.entity.ProductModerationStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.repository.ProductRepository;
import com.ecommerce.marketplace.repository.ProductReviewRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:marketplace;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"jwt.secret=bG9jYWwtdGVzdC1zaWduaW5nLWtleS1tdXN0LWJlLTMyaGFyYWN0ZXJz",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class MarketplaceApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductReviewRepository productReviewRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private JavaMailSender mailSender;

	@Test
	void healthEndpointIsPublicAndReturnsUpStatus() throws Exception {
		mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(options("/api/health")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void registerThenLoginReturnsJwtToken() throws Exception {
		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"auth-loop-test\",\"email\":\"auth-loop-test@example.com\",\"password\":\"test-password\"}"))
			.andExpect(status().isOk());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"auth-loop-test\",\"password\":\"test-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.user.username").value("auth-loop-test"))
			.andReturn();

		String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(get("/api/users/me")
					.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("auth-loop-test"))
			.andExpect(jsonPath("$.email").value("auth-loop-test@example.com"));
	}

	@Test
	void vendorCanCreateAndListProductsWithJwt() throws Exception {
		User vendor = new User();
		vendor.setUsername("vendor-product-owner");
		vendor.setEmail("vendor-product-owner@example.com");
		vendor.setPassword(passwordEncoder.encode("vendor-password"));
		vendor.setRoles(Set.of("ROLE_VENDOR"));
		userRepository.save(vendor);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"vendor-product-owner\",\"password\":\"vendor-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andReturn();

		String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(post("/api/products")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Gaming Keyboard\",\"description\":\"Mechanical keyboard\",\"price\":129.99,\"stock\":20,\"category\":\"Electronics\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Gaming Keyboard"));

		mockMvc.perform(get("/api/vendor/products")
					.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Gaming Keyboard"));
	}

	@Test
	void customerCanAddToCartAndPlaceOrder() throws Exception {
		User customer = new User();
		customer.setUsername("customer-cart-user");
		customer.setEmail("customer-cart-user@example.com");
		customer.setPassword(passwordEncoder.encode("customer-password"));
		customer.setRoles(Set.of("ROLE_CUSTOMER"));
		userRepository.save(customer);

		User vendor = new User();
		vendor.setUsername("customer-order-vendor");
		vendor.setEmail("customer-order-vendor@example.com");
		vendor.setPassword(passwordEncoder.encode("vendor-password"));
		vendor.setRoles(Set.of("ROLE_VENDOR"));
		userRepository.save(vendor);

		Product product = new Product();
		product.setName("Wireless Mouse");
		product.setDescription("Ergonomic wireless mouse");
		product.setPrice(new java.math.BigDecimal("49.99"));
		product.setStock(10);
		product.setCategory("Electronics");
		product.setVendor(vendor);
		product.setModerationStatus(ProductModerationStatus.APPROVED);
		productRepository.save(product);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"customer-cart-user\",\"password\":\"customer-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andReturn();

		String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"productId\":" + product.getId() + ",\"quantity\":2}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].productId").value(product.getId().intValue()))
			.andExpect(jsonPath("$.items[0].quantity").value(2));

		mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalAmount").value(99.98))
			.andExpect(jsonPath("$.status").value("PLACED"));
	}

	@Test
	void vendorCanListAndUpdateOrderStatusForOwnProducts() throws Exception {
		User customer = new User();
		customer.setUsername("vendor-order-customer");
		customer.setEmail("vendor-order-customer@example.com");
		customer.setPassword(passwordEncoder.encode("customer-password"));
		customer.setRoles(Set.of("ROLE_CUSTOMER"));
		userRepository.save(customer);

		User vendor = new User();
		vendor.setUsername("vendor-order-manager");
		vendor.setEmail("vendor-order-manager@example.com");
		vendor.setPassword(passwordEncoder.encode("vendor-password"));
		vendor.setRoles(Set.of("ROLE_VENDOR"));
		userRepository.save(vendor);

		Product product = new Product();
		product.setName("Noise Cancelling Headphones");
		product.setDescription("Premium audio headset");
		product.setPrice(new java.math.BigDecimal("199.99"));
		product.setStock(15);
		product.setCategory("Electronics");
		product.setVendor(vendor);
		product.setModerationStatus(ProductModerationStatus.APPROVED);
		productRepository.save(product);

		MvcResult customerLogin = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"vendor-order-customer\",\"password\":\"customer-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andReturn();
		String customerToken = JsonPath.read(customerLogin.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
			.andExpect(status().isOk());

		MvcResult orderResult = mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PLACED"))
			.andReturn();
		Long orderId = ((Number) JsonPath.read(orderResult.getResponse().getContentAsString(), "$.id")).longValue();

		MvcResult vendorLogin = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"vendor-order-manager\",\"password\":\"vendor-password\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andReturn();
		String vendorToken = JsonPath.read(vendorLogin.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(get("/api/vendor/orders")
					.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(orderId.intValue()))
			.andExpect(jsonPath("$[0].status").value("PLACED"));

		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PROCESSING\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PROCESSING"));
	}

	@Test
	void productManagementEnforcesVendorOwnershipAndValidatesRequests() throws Exception {
		User firstVendor = saveUser("product-flow-vendor-one", "ROLE_VENDOR");
		saveUser("product-flow-vendor-two", "ROLE_VENDOR");
		saveUser("product-flow-customer", "ROLE_CUSTOMER");
		String firstVendorToken = login("product-flow-vendor-one", "test-password");
		String secondVendorToken = login("product-flow-vendor-two", "test-password");
		String customerToken = login("product-flow-customer", "test-password");

		MvcResult created = mockMvc.perform(post("/api/products")
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Desk Lamp", "Adjustable LED desk lamp", "34.50", 8)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Desk Lamp"))
			.andExpect(jsonPath("$.price").value(34.50))
			.andExpect(jsonPath("$.stock").value(8))
			.andExpect(jsonPath("$.vendorId").value(firstVendor.getId().intValue()))
			.andReturn();
		Long productId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
		assertEquals(ProductModerationStatus.PENDING_REVIEW,
				productRepository.findById(productId).orElseThrow().getModerationStatus());

		mockMvc.perform(get("/api/vendor/products").header("Authorization", "Bearer " + firstVendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(productId.intValue()));
		mockMvc.perform(get("/api/vendor/products").header("Authorization", "Bearer " + secondVendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

		mockMvc.perform(put("/api/products/{id}", productId)
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Desk Lamp Pro", "Updated LED desk lamp", "44.50", 6)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Desk Lamp Pro"))
			.andExpect(jsonPath("$.price").value(44.50))
			.andExpect(jsonPath("$.stock").value(6));
		assertEquals(ProductModerationStatus.PENDING_REVIEW,
				productRepository.findById(productId).orElseThrow().getModerationStatus());

		Product approvedProduct = productRepository.findById(productId).orElseThrow();
		approvedProduct.setModerationStatus(ProductModerationStatus.APPROVED);
		approvedProduct.setModerationReason("Previously approved");
		productRepository.saveAndFlush(approvedProduct);
		mockMvc.perform(put("/api/products/{id}", productId)
				.header("Authorization", "Bearer " + firstVendorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Desk Lamp Revised", "Revised LED desk lamp", "45.50", 6)))
			.andExpect(status().isOk());
		Product resubmittedProduct = productRepository.findById(productId).orElseThrow();
		assertEquals(ProductModerationStatus.PENDING_REVIEW, resubmittedProduct.getModerationStatus());
		assertNull(resubmittedProduct.getModerationReason());

		mockMvc.perform(put("/api/products/{id}", productId)
					.header("Authorization", "Bearer " + secondVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Stolen Listing", "Should not update", "1.00", 1)))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/products/{id}", Long.MAX_VALUE)
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Missing Product", "Does not exist", "1.00", 1)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value("Product not found"));
		mockMvc.perform(post("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Customer Listing", "Not allowed", "1.00", 1)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/products")
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\",\"description\":\"Missing price\",\"stock\":-1,\"category\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product name is required")))
			.andExpect(jsonPath("$.path").value("/api/products"));
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(productRequest("Anonymous Listing", "No token", "1.00", 1)))
			.andExpect(status().isForbidden());
	}

	@Test
	void customersCanBrowseProductsWithPaginationSortingAndFilters() throws Exception {
		User customer = saveUser("product-browse-customer", "ROLE_CUSTOMER");
		User firstVendor = saveUser("product-browse-vendor-one", "ROLE_VENDOR");
		User secondVendor = saveUser("product-browse-vendor-two", "ROLE_VENDOR");
		String customerToken = login(customer.getUsername(), "test-password");

		Product notebook = saveProduct("Catalog Notebook", new BigDecimal("15.00"), 6, firstVendor);
		Product organizer = saveProduct("Catalog Organizer", new BigDecimal("45.00"), 4, secondVendor);
		Product stand = saveProduct("Catalog Stand", new BigDecimal("75.00"), 3, firstVendor);
		for (Product product : List.of(notebook, organizer, stand)) {
			product.setCategory("Catalog Test");
			productRepository.save(product);
		}

		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("category", "catalog test")
					.param("page", "0")
					.param("size", "2")
					.param("sortBy", "price")
					.param("direction", "asc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
			.andExpect(jsonPath("$.content[0].name").value("Catalog Notebook"))
			.andExpect(jsonPath("$.content[1].name").value("Catalog Organizer"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(false));

		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("category", "Catalog Test")
					.param("page", "1")
					.param("size", "2")
					.param("sortBy", "price")
					.param("direction", "desc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("Catalog Notebook"))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.last").value(true));

		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("category", "Catalog Test")
					.param("minPrice", "20")
					.param("maxPrice", "60"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("Catalog Organizer"))
			.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("search", "notebook"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("Catalog Notebook"));

		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("sortBy", "vendorUsername"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("minPrice", "80")
					.param("maxPrice", "20"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("page", "-1"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("size", "101"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer " + customerToken)
					.param("direction", "sideways"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/products"))
			.andExpect(status().isForbidden());
	}

	@Test
	void customerCheckoutCalculatesTotalsUpdatesStockAndClearsCart() throws Exception {
		User customer = saveUser("checkout-flow-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("checkout-flow-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Travel Mug", new BigDecimal("12.50"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 2)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(2));
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(3))
			.andExpect(jsonPath("$.totalAmount").value(37.50));

		MvcResult checkout = mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalAmount").value(37.50))
			.andExpect(jsonPath("$.status").value("PLACED"))
			.andExpect(jsonPath("$.paymentStatus").value("PENDING"))
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(product.getId().intValue()))
			.andExpect(jsonPath("$.items[0].quantity").value(3))
			.andExpect(jsonPath("$.items[0].unitPrice").value(12.50))
			.andExpect(jsonPath("$.items[0].subtotal").value(37.50))
			.andReturn();
		Long orderId = ((Number) JsonPath.read(checkout.getResponse().getContentAsString(), "$.id")).longValue();

		mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)))
			.andExpect(jsonPath("$.totalAmount").value(0));
		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(orderId.intValue()));
		org.junit.jupiter.api.Assertions.assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getStock());

		mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Cart is empty"))
			.andExpect(jsonPath("$.path").value("/api/orders"));
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 0)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void checkoutRejectsStaleStockWithoutChangingCartOrInventory() throws Exception {
		User customer = saveUser("checkout-stock-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("checkout-stock-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Portable Charger", new BigDecimal("25.00"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 3)))
			.andExpect(status().isOk());
		product.setStock(2);
		productRepository.save(product);

		mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.error").value("Conflict"))
			.andExpect(jsonPath("$.message").value("Insufficient stock for product: Portable Charger"))
			.andExpect(jsonPath("$.path").value("/api/orders"));
		mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(3));
		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
		org.junit.jupiter.api.Assertions.assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getStock());
		verifyNoInteractions(mailSender);
	}

	@Test
	void concurrentCheckoutsCannotOversellTheLastUnit() throws Exception {
		User firstCustomer = saveUser("concurrent-checkout-customer-one", "ROLE_CUSTOMER");
		User secondCustomer = saveUser("concurrent-checkout-customer-two", "ROLE_CUSTOMER");
		User vendor = saveUser("concurrent-checkout-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Last Unit Item", new BigDecimal("30.00"), 1, vendor);
		String firstToken = login(firstCustomer.getUsername(), "test-password");
		String secondToken = login(secondCustomer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + firstToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + secondToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk());

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CompletableFuture<MvcResult> firstCheckout = checkoutConcurrently(firstToken, ready, start, executor);
			CompletableFuture<MvcResult> secondCheckout = checkoutConcurrently(secondToken, ready, start, executor);
			org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();

			MvcResult firstResponse = firstCheckout.get(20, TimeUnit.SECONDS);
			MvcResult secondResponse = secondCheckout.get(20, TimeUnit.SECONDS);
			int firstStatus = firstResponse.getResponse().getStatus();
			int secondStatus = secondResponse.getResponse().getStatus();
			org.junit.jupiter.api.Assertions.assertTrue(
					(firstStatus == 200 && secondStatus == 409) || (firstStatus == 409 && secondStatus == 200),
					"Exactly one checkout should succeed and the other should report insufficient stock");
			MvcResult conflictResponse = firstStatus == 409 ? firstResponse : secondResponse;
			org.junit.jupiter.api.Assertions.assertEquals(409,
					((Number) JsonPath.read(conflictResponse.getResponse().getContentAsString(), "$.status")).intValue());
			org.junit.jupiter.api.Assertions.assertEquals("Conflict",
					JsonPath.read(conflictResponse.getResponse().getContentAsString(), "$.error"));
		} finally {
			start.countDown();
			executor.shutdownNow();
		}

		org.junit.jupiter.api.Assertions.assertEquals(0,
				productRepository.findById(product.getId()).orElseThrow().getStock());
		MvcResult firstHistory = mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + firstToken))
			.andExpect(status().isOk())
			.andReturn();
		MvcResult secondHistory = mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + secondToken))
			.andExpect(status().isOk())
			.andReturn();
		int firstOrderCount = JsonPath.read(firstHistory.getResponse().getContentAsString(), "$.length()");
		int secondOrderCount = JsonPath.read(secondHistory.getResponse().getContentAsString(), "$.length()");
		org.junit.jupiter.api.Assertions.assertEquals(1, firstOrderCount + secondOrderCount);
		MvcResult firstCart = mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + firstToken))
			.andExpect(status().isOk())
			.andReturn();
		MvcResult secondCart = mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + secondToken))
			.andExpect(status().isOk())
			.andReturn();
		int firstCartCount = JsonPath.read(firstCart.getResponse().getContentAsString(), "$.items.length()");
		int secondCartCount = JsonPath.read(secondCart.getResponse().getContentAsString(), "$.items.length()");
		org.junit.jupiter.api.Assertions.assertEquals(1, firstCartCount + secondCartCount);
	}

	@Test
	void customerCanSimulatePaymentFailureRetryAndSuccess() throws Exception {
		User customer = saveUser("payment-flow-customer", "ROLE_CUSTOMER");
		User otherCustomer = saveUser("payment-flow-other-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("payment-flow-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Payment Test Item", new BigDecimal("42.50"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String otherCustomerToken = login(otherCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		Long orderId = placeOrder(customerToken, product.getId());

		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":null}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + otherCustomerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isNotFound());

		MvcResult failedPayment = mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderId").value(orderId.intValue()))
			.andExpect(jsonPath("$.amount").value(42.50))
			.andExpect(jsonPath("$.paymentStatus").value("FAILED"))
			.andExpect(jsonPath("$.transactionReference").isNotEmpty())
			.andReturn();
		String failedReference = JsonPath.read(failedPayment.getResponse().getContentAsString(), "$.transactionReference");
		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paymentStatus").value("FAILED"))
			.andExpect(jsonPath("$.paymentReference").value(failedReference));

		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false,\"amount\":0.01}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.amount").value(42.50))
			.andExpect(jsonPath("$.paymentStatus").value("PAID"))
			.andExpect(jsonPath("$.transactionReference").isNotEmpty());
		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));

		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void vendorAndAdminCouponsApplyDiscountsToCheckout() throws Exception {
		User customer = saveUser("coupon-flow-customer", "ROLE_CUSTOMER");
		User secondCustomer = saveUser("coupon-flow-second-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("coupon-flow-vendor", "ROLE_VENDOR");
		User otherVendor = saveUser("coupon-flow-other-vendor", "ROLE_VENDOR");
		User admin = saveUser("coupon-flow-admin", "ROLE_ADMIN");
		Product eligibleProduct = saveProduct("Vendor Promo Product", new BigDecimal("100.00"), 10, vendor);
		Product otherProduct = saveProduct("Other Vendor Product", new BigDecimal("50.00"), 10, otherVendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String secondCustomerToken = login(secondCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String adminToken = login(admin.getUsername(), "test-password");

		mockMvc.perform(post("/api/coupons")
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"vendor10\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10,"
							+ "\"expiresAt\":\"2030-12-31T23:59:59\",\"usageLimit\":1}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("VENDOR10"))
			.andExpect(jsonPath("$.vendorUsername").value(vendor.getUsername()));

		mockMvc.perform(post("/api/coupons")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"adminfixed\",\"discountType\":\"FIXED\",\"discountValue\":20}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.vendorUsername").doesNotExist());
		mockMvc.perform(post("/api/coupons")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"NOTALLOWED\",\"discountType\":\"FIXED\",\"discountValue\":5}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(eligibleProduct.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(otherProduct.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/cart/coupon")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"vendor10\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subtotal").value(150.00))
			.andExpect(jsonPath("$.discountAmount").value(10.00))
			.andExpect(jsonPath("$.totalAmount").value(140.00))
			.andExpect(jsonPath("$.couponCode").value("VENDOR10"));

		MvcResult placedOrder = mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subtotalAmount").value(150.00))
			.andExpect(jsonPath("$.discountAmount").value(10.00))
			.andExpect(jsonPath("$.totalAmount").value(140.00))
			.andExpect(jsonPath("$.couponCode").value("VENDOR10"))
			.andReturn();
		Long orderId = ((Number) JsonPath.read(placedOrder.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subtotalAmount").value(150.00))
			.andExpect(jsonPath("$.discountAmount").value(10.00))
			.andExpect(jsonPath("$.totalAmount").value(140.00))
			.andExpect(jsonPath("$.couponCode").value("VENDOR10"));

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + secondCustomerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(eligibleProduct.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/cart/coupon")
					.header("Authorization", "Bearer " + secondCustomerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"VENDOR10\"}"))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/api/cart/coupon")
					.header("Authorization", "Bearer " + secondCustomerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"code\":\"adminfixed\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subtotal").value(100.00))
			.andExpect(jsonPath("$.discountAmount").value(20.00))
			.andExpect(jsonPath("$.totalAmount").value(80.00));
		mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + secondCustomerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.subtotalAmount").value(100.00))
			.andExpect(jsonPath("$.discountAmount").value(20.00))
			.andExpect(jsonPath("$.totalAmount").value(80.00))
			.andExpect(jsonPath("$.couponCode").value("ADMINFIXED"));
	}

	@Test
	void onlyOwnerCanDownloadInvoiceForPaidOrder() throws Exception {
		User customer = saveUser("invoice-flow-customer", "ROLE_CUSTOMER");
		User otherCustomer = saveUser("invoice-flow-other-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("invoice-flow-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Invoice Test Product", new BigDecimal("65.25"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String otherCustomerToken = login(otherCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		Long orderId = placeOrder(customerToken, product.getId());

		mockMvc.perform(get("/api/orders/{id}/invoice", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("An invoice is available only for paid orders"));
		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));

		MvcResult invoiceResponse = mockMvc.perform(get("/api/orders/{id}/invoice", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
			.andReturn();
		byte[] pdfBytes = invoiceResponse.getResponse().getContentAsByteArray();
		org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 100);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			String invoiceText = new PDFTextStripper().getText(document);
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("INVOICE"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("Invoice Test Product"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("65.25"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("DEMO PAYMENT - no real money was charged"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("Subtotal: INR 65.25"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("Discount: -INR 0.00"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains("Total paid: INR 65.25"));
			org.junit.jupiter.api.Assertions.assertTrue(invoiceText.contains(orderId.toString()));
		}

		mockMvc.perform(get("/api/orders/{id}/invoice", orderId)
					.header("Authorization", "Bearer " + otherCustomerToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/orders/{id}/invoice", orderId)
					.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/orders/{id}/invoice", orderId))
			.andExpect(status().isForbidden());
	}

	@Test
	void customersCanManagePrivateWishlistsWithoutDuplicateFavorites() throws Exception {
		User customer = saveUser("wishlist-flow-customer", "ROLE_CUSTOMER");
		User otherCustomer = saveUser("wishlist-flow-other-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("wishlist-flow-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Wishlist Product", new BigDecimal("37.50"), 4, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String otherCustomerToken = login(otherCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");

		mockMvc.perform(post("/api/wishlist/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"productId\":" + product.getId() + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(product.getId().intValue()))
			.andExpect(jsonPath("$.items[0].productName").value("Wishlist Product"))
			.andExpect(jsonPath("$.items[0].price").value(37.50));

		mockMvc.perform(post("/api/wishlist/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"productId\":" + product.getId() + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].category").value("Test Category"));
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + otherCustomerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));

		mockMvc.perform(delete("/api/wishlist/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
		mockMvc.perform(delete("/api/wishlist/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/wishlist/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"productId\":9223372036854775807}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
		mockMvc.perform(get("/api/wishlist"))
			.andExpect(status().isForbidden());
	}

	@Test
	void customersCanAtomicallyMoveWishlistItemsIntoCart() throws Exception {
		User customer = saveUser("wishlist-move-customer", "ROLE_CUSTOMER");
		User otherCustomer = saveUser("wishlist-move-other-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("wishlist-move-vendor", "ROLE_VENDOR");
		Product availableProduct = saveProduct("Available Wishlist Item", new BigDecimal("24.00"), 5, vendor);
		Product unavailableProduct = saveProduct("Unavailable Wishlist Item", new BigDecimal("49.00"), 1, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String otherCustomerToken = login(otherCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");

		for (Product product : List.of(availableProduct, unavailableProduct)) {
			mockMvc.perform(post("/api/wishlist/items")
						.header("Authorization", "Bearer " + customerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":" + product.getId() + "}"))
				.andExpect(status().isOk());
		}

		mockMvc.perform(post("/api/wishlist/items/{productId}/move-to-cart", availableProduct.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(availableProduct.getId().intValue()))
			.andExpect(jsonPath("$.items[0].quantity").value(1))
			.andExpect(jsonPath("$.totalAmount").value(24.00));
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(unavailableProduct.getId().intValue()));

		unavailableProduct.setStock(0);
		productRepository.save(unavailableProduct);
		mockMvc.perform(post("/api/wishlist/items/{productId}/move-to-cart", unavailableProduct.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isConflict());
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(unavailableProduct.getId().intValue()));
		mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.items[0].productId").value(availableProduct.getId().intValue()));

		mockMvc.perform(post("/api/wishlist/items/{productId}/move-to-cart", availableProduct.getId())
					.header("Authorization", "Bearer " + otherCustomerToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/wishlist/items/{productId}/move-to-cart", availableProduct.getId())
					.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/wishlist/items/{productId}/move-to-cart", availableProduct.getId()))
			.andExpect(status().isForbidden());
	}

	@Test
	void vendorAnalyticsAggregatePaidSalesAndRankProducts() throws Exception {
		User vendor = saveUser("analytics-flow-vendor", "ROLE_VENDOR");
		User otherVendor = saveUser("analytics-flow-other-vendor", "ROLE_VENDOR");
		User firstCustomer = saveUser("analytics-flow-customer-one", "ROLE_CUSTOMER");
		User secondCustomer = saveUser("analytics-flow-customer-two", "ROLE_CUSTOMER");
		User unpaidCustomer = saveUser("analytics-flow-unpaid-customer", "ROLE_CUSTOMER");
		Product bestSeller = saveProduct("Analytics Best Seller", new BigDecimal("10.00"), 20, vendor);
		Product secondBestSeller = saveProduct("Analytics Second Seller", new BigDecimal("5.00"), 20, vendor);
		Product otherVendorProduct = saveProduct("Other Vendor Analytics Product", new BigDecimal("100.00"), 20, otherVendor);
		String firstCustomerToken = login(firstCustomer.getUsername(), "test-password");
		String secondCustomerToken = login(secondCustomer.getUsername(), "test-password");
		String unpaidCustomerToken = login(unpaidCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String otherVendorToken = login(otherVendor.getUsername(), "test-password");

		Long firstPaidOrder = addCartItemsAndPlaceOrder(firstCustomerToken,
				new ProductQuantity(bestSeller.getId(), 2));
		payOrder(firstCustomerToken, firstPaidOrder);
		Long secondPaidOrder = addCartItemsAndPlaceOrder(secondCustomerToken,
				new ProductQuantity(bestSeller.getId(), 1),
				new ProductQuantity(secondBestSeller.getId(), 1),
				new ProductQuantity(otherVendorProduct.getId(), 1));
		payOrder(secondCustomerToken, secondPaidOrder);
		addCartItemsAndPlaceOrder(unpaidCustomerToken, new ProductQuantity(secondBestSeller.getId(), 3));

		mockMvc.perform(get("/api/vendor/analytics").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalSalesRevenue").value(35.00))
			.andExpect(jsonPath("$.orderCount").value(2))
			.andExpect(jsonPath("$.unitsSold").value(4))
			.andExpect(jsonPath("$.bestSellingProducts", org.hamcrest.Matchers.hasSize(2)))
			.andExpect(jsonPath("$.bestSellingProducts[0].productId").value(bestSeller.getId().intValue()))
			.andExpect(jsonPath("$.bestSellingProducts[0].unitsSold").value(3))
			.andExpect(jsonPath("$.bestSellingProducts[0].salesRevenue").value(30.00))
			.andExpect(jsonPath("$.bestSellingProducts[1].productId").value(secondBestSeller.getId().intValue()))
			.andExpect(jsonPath("$.bestSellingProducts[1].unitsSold").value(1));
		mockMvc.perform(get("/api/vendor/analytics").header("Authorization", "Bearer " + otherVendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalSalesRevenue").value(100.00))
			.andExpect(jsonPath("$.orderCount").value(1))
			.andExpect(jsonPath("$.unitsSold").value(1))
			.andExpect(jsonPath("$.bestSellingProducts", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.bestSellingProducts[0].productId").value(otherVendorProduct.getId().intValue()));
		mockMvc.perform(get("/api/vendor/analytics").header("Authorization", "Bearer " + firstCustomerToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/vendor/analytics"))
			.andExpect(status().isForbidden());
	}

	@Test
	void customersCanUpdateAndRemoveCartItemsWithStockValidation() throws Exception {
		User customer = saveUser("cart-edit-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("cart-edit-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Cart Editable Item", new BigDecimal("14.25"), 3, vendor);
		String customerToken = login(customer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(put("/api/cart/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"quantity\":3}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(3))
			.andExpect(jsonPath("$.totalAmount").value(42.75));
		mockMvc.perform(put("/api/cart/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"quantity\":4}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
		mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(3));

		mockMvc.perform(delete("/api/cart/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)))
			.andExpect(jsonPath("$.totalAmount").value(0));
		mockMvc.perform(delete("/api/cart/items/{productId}", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isNotFound());
	}

	@Test
	void committedOrderAndFulfillmentChangesSendCustomerEmails() throws Exception {
		User customer = saveUser("email-flow-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("email-flow-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Email Test Product", new BigDecimal("52.75"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		clearInvocations(mailSender);

		Long orderId = placeOrder(customerToken, product.getId());
		ArgumentCaptor<SimpleMailMessage> confirmationCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(confirmationCaptor.capture());
		SimpleMailMessage confirmation = confirmationCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals(customer.getEmail(), confirmation.getTo()[0]);
		org.junit.jupiter.api.Assertions.assertEquals("Order confirmation #" + orderId, confirmation.getSubject());
		org.junit.jupiter.api.Assertions.assertTrue(confirmation.getText().contains("Total: 52.75"));

		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PROCESSING\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"SHIPPED\"}"))
			.andExpect(status().isOk());
		ArgumentCaptor<SimpleMailMessage> statusCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(3)).send(statusCaptor.capture());
		SimpleMailMessage statusMessage = statusCaptor.getAllValues().stream()
				.filter(message -> message.getSubject().equals("Order #" + orderId + " status update")
						&& message.getText().contains("changed to SHIPPED"))
				.findFirst()
				.orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("Order #" + orderId + " status update", statusMessage.getSubject());
		org.junit.jupiter.api.Assertions.assertTrue(statusMessage.getText().contains("changed to SHIPPED"));
		org.junit.jupiter.api.Assertions.assertEquals(customer.getEmail(), statusMessage.getTo()[0]);
	}

	@Test
	void vendorOrderManagementFiltersByOwnershipAndRejectsUnauthorizedUpdates() throws Exception {
		User customer = saveUser("vendor-order-flow-customer", "ROLE_CUSTOMER");
		User firstVendor = saveUser("vendor-order-flow-one", "ROLE_VENDOR");
		User secondVendor = saveUser("vendor-order-flow-two", "ROLE_VENDOR");
		Product firstProduct = saveProduct("Vendor One Item", new BigDecimal("18.00"), 10, firstVendor);
		Product secondProduct = saveProduct("Vendor Two Item", new BigDecimal("22.00"), 10, secondVendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String firstVendorToken = login(firstVendor.getUsername(), "test-password");
		String secondVendorToken = login(secondVendor.getUsername(), "test-password");

		Long firstOrderId = placeOrder(customerToken, firstProduct.getId());
		Long secondOrderId = placeOrder(customerToken, secondProduct.getId());

		mockMvc.perform(get("/api/vendor/orders").header("Authorization", "Bearer " + firstVendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(firstOrderId.intValue()))
			.andExpect(jsonPath("$[0].items[0].productId").value(firstProduct.getId().intValue()));
		mockMvc.perform(get("/api/vendor/orders").header("Authorization", "Bearer " + secondVendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(secondOrderId.intValue()));

		mockMvc.perform(patch("/api/vendor/orders/{id}/status", firstOrderId)
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PROCESSING\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PROCESSING"));
		mockMvc.perform(patch("/api/vendor/orders/{id}/status", firstOrderId)
					.header("Authorization", "Bearer " + secondVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"SHIPPED\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/vendor/orders/{id}/status", firstOrderId)
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":null}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/vendor/orders/{id}/status", firstOrderId)
					.header("Authorization", "Bearer " + firstVendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"UNKNOWN\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.message").value("Malformed or invalid request body"))
			.andExpect(jsonPath("$.path").value("/api/vendor/orders/" + firstOrderId + "/status"));
		mockMvc.perform(get("/api/vendor/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/vendor/orders"))
			.andExpect(status().isForbidden());
	}

	@Test
	void customerOrderHistoryAndDetailsShowCompleteItemsAndCurrentStatus() throws Exception {
		User customer = saveUser("order-tracking-customer", "ROLE_CUSTOMER");
		User otherCustomer = saveUser("order-tracking-other-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("order-tracking-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Bluetooth Speaker", new BigDecimal("79.95"), 8, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String otherCustomerToken = login(otherCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		Long orderId = placeOrder(customerToken, product.getId());
		Long secondOrderId = placeOrder(customerToken, product.getId());

		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
			.andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(orderId.intValue(), secondOrderId.intValue())))
			.andExpect(jsonPath("$[0].status").value("PLACED"))
			.andExpect(jsonPath("$[0].items[0].productId").value(product.getId().intValue()))
			.andExpect(jsonPath("$[0].items[0].productName").value("Bluetooth Speaker"))
			.andExpect(jsonPath("$[0].items[0].quantity").value(1))
			.andExpect(jsonPath("$[0].items[0].unitPrice").value(79.95))
			.andExpect(jsonPath("$[0].items[0].subtotal").value(79.95));
		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + otherCustomerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(orderId.intValue()))
			.andExpect(jsonPath("$.status").value("PLACED"))
			.andExpect(jsonPath("$.items[0].productName").value("Bluetooth Speaker"))
			.andExpect(jsonPath("$.items[0].subtotal").value(79.95));

		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"PROCESSING\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"SHIPPED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SHIPPED"));
		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SHIPPED"));

		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + otherCustomerToken))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Order not found"));
		mockMvc.perform(get("/api/orders/{id}", orderId)
					.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/orders/{id}", orderId))
			.andExpect(status().isForbidden());
	}

	@Test
	void vendorsCanShopAndRetainVendorOnlyAndAdminBoundaries() throws Exception {
		User vendor = saveUser("dual-capability-vendor", "ROLE_VENDOR");
		User otherVendor = saveUser("dual-capability-supplier", "ROLE_VENDOR");
		Product product = saveProduct("Vendor Shopping Product", new BigDecimal("24.95"), 5, otherVendor);
		String vendorToken = login(vendor.getUsername(), "test-password");

		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].name").value("Vendor Shopping Product"));
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].productName").value("Vendor Shopping Product"));

		MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":1}]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(vendor.getUsername()))
			.andReturn();
		int orderId = ((Number) JsonPath.read(checkout.getResponse().getContentAsString(), "$.id")).intValue();

		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(orderId));
		mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(orderId));
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":5,\"text\":\"Bought this item for my own use\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/vendor/analytics").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/ledger/all").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminPortalApisAreRestrictedToAdmins() throws Exception {
		User admin = saveUser("portal-test-admin", "ROLE_ADMIN");
		User vendor = saveUser("portal-test-vendor", "ROLE_VENDOR");
		User customer = saveUser("portal-test-customer", "ROLE_CUSTOMER");
		String adminToken = login(admin.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String customerToken = login(customer.getUsername(), "test-password");
		List<String> adminEndpoints = List.of("/api/admin/dashboard", "/api/admin/users", "/api/admin/vendors",
				"/api/admin/products", "/api/admin/orders", "/api/admin/reviews", "/api/admin/ledger/all");
		for (String endpoint : adminEndpoints) {
			mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken))
					.andExpect(status().isOk());
			mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + customerToken))
					.andExpect(status().isForbidden());
			mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + vendorToken))
					.andExpect(status().isForbidden());
		}
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId())
					.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId())
					.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void purchasedCustomersCanReviewProductsAndProductDetailsShowAggregates() throws Exception {
		User customer = saveUser("review-flow-customer", "ROLE_CUSTOMER");
		User secondCustomer = saveUser("review-flow-customer-two", "ROLE_CUSTOMER");
		User vendor = saveUser("review-flow-vendor", "ROLE_VENDOR");
		User admin = saveUser("review-flow-admin", "ROLE_ADMIN");
		Product product = saveProduct("Reviewable Headphones", new BigDecimal("129.00"), 10, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String secondCustomerToken = login(secondCustomer.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String adminToken = login(admin.getUsername(), "test-password");

		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":5,\"text\":\"Excellent sound and fit\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("You can only review products you have purchased"));

		placeOrder(customerToken, product.getId());
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":5,\"text\":\"Excellent sound and fit\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(5))
			.andExpect(jsonPath("$.text").value("Excellent sound and fit"))
			.andExpect(jsonPath("$.customerUsername").value(customer.getUsername()));

		placeOrder(secondCustomerToken, product.getId());
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + secondCustomerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":3,\"text\":\"Comfortable but average battery\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(3));

		mockMvc.perform(get("/api/products/{id}", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(product.getId().intValue()))
			.andExpect(jsonPath("$.averageRating").value(4.0))
			.andExpect(jsonPath("$.reviewCount").value(2));
		mockMvc.perform(get("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
			.andExpect(jsonPath("$[0].text").value("Comfortable but average battery"));

		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":4,\"text\":\"Second review\"}"))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":6,\"text\":\"Invalid rating\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + vendorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":5,\"text\":\"Vendor review\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("You can only review products you have purchased"));
		mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rating\":5,\"text\":\"Admin review\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/products/{id}", product.getId()))
			.andExpect(status().isForbidden());
	}

	@Test
	void checkoutSnapshotsShippingAndCancellationRestoresStockWithoutPayingUnpaidLedger() throws Exception {
		User customer = saveUser("shipping-cancel-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("shipping-cancel-vendor", "ROLE_VENDOR");
		User admin = saveUser("shipping-cancel-admin", "ROLE_ADMIN");
		Product product = saveProduct("Shipping Snapshot Product", new BigDecimal("1200.00"), 4, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String adminToken = login(admin.getUsername(), "test-password");

		MvcResult created = mockMvc.perform(post("/api/orders/checkout")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":2}],"
							+ "\"shippingAddress\":{\"fullName\":\"Interview Demo\",\"phone\":\"+919876543210\","
							+ "\"addressLine\":\"12 Market Road\",\"city\":\"Pune\",\"state\":\"Maharashtra\","
							+ "\"postalCode\":\"411001\",\"country\":\"India\"}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shippingAddress.fullName").value("Interview Demo"))
			.andExpect(jsonPath("$.shippingAddress.city").value("Pune"))
			.andReturn();
		Long orderId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
		org.junit.jupiter.api.Assertions.assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getStock());

		mockMvc.perform(patch("/api/vendor/orders/{id}/status", orderId)
					.header("Authorization", "Bearer " + login(vendor.getUsername(), "test-password"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"DELIVERED\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/ledger/all").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records[0].payoutStatus").value("PENDING"));

		mockMvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		org.junit.jupiter.api.Assertions.assertEquals(4, productRepository.findById(product.getId()).orElseThrow().getStock());
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/ledger/all").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records[0].payoutStatus").value("PENDING"));
	}

	@Test
	void onlyPaidNonCancelledLedgerEntriesCanBePaidOut() throws Exception {
		User customer = saveUser("payout-eligibility-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("payout-eligibility-vendor", "ROLE_VENDOR");
		User admin = saveUser("payout-eligibility-admin", "ROLE_ADMIN");
		Product product = saveProduct("Payout Eligibility Product", new BigDecimal("400.00"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");
		String adminToken = login(admin.getUsername(), "test-password");
		Long orderId = placeOrder(customerToken, product.getId());
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/ledger/all").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records[0].payoutStatus").value("PENDING"));
		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));
		mockMvc.perform(post("/api/admin/payouts/{vendorId}", vendor.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/ledger/all").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records[0].payoutStatus").value("PAID"));
	}

	@Test
	void adminsModerateProductsAndOnlyApprovedProductsAreCustomerAccessible() throws Exception {
		User admin = saveUser("moderation-admin", "ROLE_ADMIN");
		User vendor = saveUser("moderation-vendor", "ROLE_VENDOR");
		User customer = saveUser("moderation-customer", "ROLE_CUSTOMER");
		String adminToken = login(admin.getUsername(), "test-password");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String customerToken = login(customer.getUsername(), "test-password");

		MvcResult created = mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + vendorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Moderated Lamp", "A listing awaiting review", "30.00", 4)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moderationStatus").value("PENDING_REVIEW"))
			.andReturn();
		Long pendingProductId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

		mockMvc.perform(get("/api/admin/products").header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + pendingProductId + ")].moderationStatus")
				.value(org.hamcrest.Matchers.contains("PENDING_REVIEW")))
			.andExpect(jsonPath("$[?(@.id == " + pendingProductId + ")].stockStatus")
				.value(org.hamcrest.Matchers.contains("ACTIVE")));
		mockMvc.perform(get("/api/products")
				.header("Authorization", "Bearer " + customerToken)
				.param("search", "Moderated Lamp"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(0)));
		mockMvc.perform(get("/api/products/{id}", pendingProductId).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/cart/items")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cartItemRequest(pendingProductId, 1)))
			.andExpect(status().isConflict());
		mockMvc.perform(patch("/api/admin/products/{id}/approve", pendingProductId)
				.header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/admin/products/{id}/approve", pendingProductId)
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moderationStatus").value("APPROVED"))
			.andExpect(jsonPath("$.moderationReason").isEmpty());
		mockMvc.perform(get("/api/products/{id}", pendingProductId).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk());

		mockMvc.perform(patch("/api/admin/products/{id}/suspend", pendingProductId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"   \"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/admin/products/{id}/suspend", pendingProductId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Policy violation\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moderationStatus").value("SUSPENDED"))
			.andExpect(jsonPath("$.moderationReason").value("Policy violation"));
		mockMvc.perform(patch("/api/admin/products/{id}/suspend", pendingProductId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Again\"}"))
			.andExpect(status().isConflict());

		MvcResult rejected = mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + vendorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequest("Rejected Lamp", "A second listing awaiting review", "40.00", 3)))
			.andExpect(status().isOk())
			.andReturn();
		Long rejectedProductId = ((Number) JsonPath.read(rejected.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(patch("/api/admin/products/{id}/reject", rejectedProductId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/admin/products/{id}/reject", rejectedProductId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Missing compliance information\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moderationStatus").value("REJECTED"))
			.andExpect(jsonPath("$.moderationReason").value("Missing compliance information"));
		mockMvc.perform(get("/api/vendor/products").header("Authorization", "Bearer " + vendorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + rejectedProductId + ")].moderationReason")
				.value(org.hamcrest.Matchers.contains("Missing compliance information")));
	}

	@Test
	void checkoutRejectsAProductThatBecomesUnapprovedAfterItIsAddedToCart() throws Exception {
		User customer = saveUser("moderation-checkout-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("moderation-checkout-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Checkout Moderation Product", new BigDecimal("20.00"), 3, vendor);
		String customerToken = login(customer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isOk());
		product.setModerationStatus(ProductModerationStatus.SUSPENDED);
		product.setModerationReason("Removed before checkout");
		productRepository.saveAndFlush(product);

		mockMvc.perform(post("/api/orders/checkout")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":1}]}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("Product is not currently available for purchase."));
		assertEquals(3, productRepository.findById(product.getId()).orElseThrow().getStock());
		mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
	}

	@Test
	void moderationActionsRejectCustomerAndUnauthenticatedRequests() throws Exception {
		User admin = saveUser("moderation-auth-admin", "ROLE_ADMIN");
		User customer = saveUser("moderation-auth-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("moderation-auth-vendor", "ROLE_VENDOR");
		Product pending = saveProduct("Moderation Auth Pending", new BigDecimal("15.00"), 2, vendor);
		pending.setModerationStatus(ProductModerationStatus.PENDING_REVIEW);
		productRepository.saveAndFlush(pending);
		Product approved = saveProduct("Moderation Auth Approved", new BigDecimal("16.00"), 2, vendor);
		String adminToken = login(admin.getUsername(), "test-password");
		String customerToken = login(customer.getUsername(), "test-password");

		for (String endpoint : List.of(
				"/api/admin/products/" + pending.getId() + "/approve",
				"/api/admin/products/" + pending.getId() + "/reject",
				"/api/admin/products/" + approved.getId() + "/suspend")) {
			mockMvc.perform(patch(endpoint)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"reason\":\"Policy violation\"}"))
				.andExpect(status().isForbidden());
			mockMvc.perform(patch(endpoint)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"reason\":\"Policy violation\"}"))
				.andExpect(status().isForbidden());
		}

		mockMvc.perform(patch("/api/admin/products/{id}/approve", pending.getId())
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk());
	}

	@Test
	void vendorProductRequestsCannotMassAssignModerationFields() throws Exception {
		User vendor = saveUser("moderation-mass-assignment-vendor", "ROLE_VENDOR");
		String vendorToken = login(vendor.getUsername(), "test-password");
		String createRequest = productRequest("Mass Assignment Product", "Vendor supplied fields must be ignored", "22.00", 4)
				.replace("}", ",\"moderationStatus\":\"APPROVED\",\"moderationReason\":\"attacker supplied\"}");
		MvcResult created = mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + vendorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moderationStatus").value("PENDING_REVIEW"))
			.andReturn();
		Long productId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
		Product product = productRepository.findById(productId).orElseThrow();
		assertEquals(ProductModerationStatus.PENDING_REVIEW, product.getModerationStatus());
		assertNull(product.getModerationReason());

		product.setModerationStatus(ProductModerationStatus.REJECTED);
		product.setModerationReason("Existing moderator reason");
		productRepository.saveAndFlush(product);
		String updateRequest = productRequest("Mass Assignment Updated", "Still cannot set moderation", "23.00", 4)
				.replace("}", ",\"moderationStatus\":\"APPROVED\",\"moderationReason\":\"replace reason\"}");
		mockMvc.perform(put("/api/products/{id}", productId)
				.header("Authorization", "Bearer " + vendorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
			.andExpect(status().isOk());
		Product afterUpdate = productRepository.findById(productId).orElseThrow();
		assertEquals(ProductModerationStatus.REJECTED, afterUpdate.getModerationStatus());
		assertEquals("Existing moderator reason", afterUpdate.getModerationReason());
	}

	@Test
	void everyUnapprovedModerationStatusIsHiddenAndCannotBePurchased() throws Exception {
		User customer = saveUser("moderation-visibility-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("moderation-visibility-vendor", "ROLE_VENDOR");
		String customerToken = login(customer.getUsername(), "test-password");
		List<Product> unavailableProducts = List.of(
				productWithStatus("Pending Visibility Product", ProductModerationStatus.PENDING_REVIEW, vendor),
				productWithStatus("Rejected Visibility Product", ProductModerationStatus.REJECTED, vendor),
				productWithStatus("Suspended Visibility Product", ProductModerationStatus.SUSPENDED, vendor));
		Product legacyNullProduct = saveProduct("Legacy Null Visibility Product", new BigDecimal("18.00"), 3, vendor);
		jdbcTemplate.execute("ALTER TABLE products ALTER COLUMN moderation_status DROP NOT NULL");
		jdbcTemplate.update("UPDATE products SET moderation_status = NULL WHERE id = ?", legacyNullProduct.getId());

		for (Product product : unavailableProducts) {
			assertProductIsUnavailableToCustomer(customerToken, product);
		}
		assertProductIsUnavailableToCustomer(customerToken, legacyNullProduct);

		Product cartProduct = saveProduct("Suspended Cart Quantity Product", new BigDecimal("19.00"), 3, vendor);
		mockMvc.perform(post("/api/cart/items")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cartItemRequest(cartProduct.getId(), 1)))
			.andExpect(status().isOk());
		cartProduct.setModerationStatus(ProductModerationStatus.SUSPENDED);
		productRepository.saveAndFlush(cartProduct);
		mockMvc.perform(put("/api/cart/items/{productId}", cartProduct.getId())
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":2}"))
			.andExpect(status().isConflict());
	}

	@Test
	void moderationTransitionsValidateReasonsAndClearReasonsOnApproval() throws Exception {
		User admin = saveUser("moderation-transition-admin", "ROLE_ADMIN");
		User vendor = saveUser("moderation-transition-vendor", "ROLE_VENDOR");
		String adminToken = login(admin.getUsername(), "test-password");
		Product product = saveProduct("Moderation Transition Product", new BigDecimal("25.00"), 3, vendor);
		product.setModerationStatus(ProductModerationStatus.PENDING_REVIEW);
		productRepository.saveAndFlush(product);

		mockMvc.perform(patch("/api/admin/products/{id}/reject", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/admin/products/{id}/reject", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Missing documentation\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.moderationStatus").value("REJECTED"));
		mockMvc.perform(patch("/api/admin/products/{id}/approve", product.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.moderationStatus").value("APPROVED"))
			.andExpect(jsonPath("$.moderationReason").isEmpty());
		mockMvc.perform(patch("/api/admin/products/{id}/reject", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Not allowed from approved\"}"))
			.andExpect(status().isConflict());
		mockMvc.perform(patch("/api/admin/products/{id}/suspend", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Policy violation\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/admin/products/{id}/approve", product.getId()).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isConflict());
		mockMvc.perform(patch("/api/admin/products/{id}/reinstate", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Corrective action verified\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.moderationStatus").value("PENDING_REVIEW"))
			.andExpect(jsonPath("$.moderationReason").value("Corrective action verified"));
	}

	@Test
	void cartAndWishlistHideProductsThatBecomeUnavailableButStillAllowRemoval() throws Exception {
		User customer = saveUser("moderation-saved-items-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("moderation-saved-items-vendor", "ROLE_VENDOR");
		Product cartProduct = saveProduct("Hidden Cart Product", new BigDecimal("29.99"), 4, vendor);
		Product wishlistProduct = saveProduct("Hidden Wishlist Product", new BigDecimal("39.99"), 5, vendor);
		String customerToken = login(customer.getUsername(), "test-password");

		mockMvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON).content(cartItemRequest(cartProduct.getId(), 1)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/wishlist/items").header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + wishlistProduct.getId() + "}"))
			.andExpect(status().isOk());

		cartProduct.setModerationStatus(ProductModerationStatus.SUSPENDED);
		cartProduct.setModerationReason("Policy investigation");
		wishlistProduct.setModerationStatus(ProductModerationStatus.REJECTED);
		wishlistProduct.setModerationReason("Listing no longer permitted");
		productRepository.saveAndFlush(cartProduct);
		productRepository.saveAndFlush(wishlistProduct);

		mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)))
			.andExpect(jsonPath("$.subtotal").value(0));
		mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));

		mockMvc.perform(delete("/api/cart/items/{productId}", cartProduct.getId()).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
		mockMvc.perform(delete("/api/wishlist/items/{productId}", wishlistProduct.getId()).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
	}

	@Test
	void suspensionDoesNotBreakHistoricalOrdersPaymentsRestockingOrStoredReviews() throws Exception {
		User admin = saveUser("moderation-history-admin", "ROLE_ADMIN");
		User customer = saveUser("moderation-history-customer", "ROLE_CUSTOMER");
		User vendor = saveUser("moderation-history-vendor", "ROLE_VENDOR");
		Product product = saveProduct("Moderation History Product", new BigDecimal("31.00"), 2, vendor);
		String adminToken = login(admin.getUsername(), "test-password");
		String customerToken = login(customer.getUsername(), "test-password");
		Long orderId = placeOrder(customerToken, product.getId());

		mockMvc.perform(post("/api/products/{id}/reviews", product.getId()).header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"text\":\"Bought before suspension\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/api/admin/products/{id}/suspend", product.getId()).header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Policy violation\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(orderId.intValue()));
		mockMvc.perform(post("/api/orders/{id}/payments", orderId).header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"simulateFailure\":false}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PAID"));
		mockMvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
		assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getStock());
		assertEquals(1, productReviewRepository.countByProduct_Id(product.getId()));
	}

	private User saveUser(String username, String role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setPassword(passwordEncoder.encode("test-password"));
		user.setRoles(Set.of(role));
		return userRepository.save(user);
	}

	private Product saveProduct(String name, BigDecimal price, int stock, User vendor) {
		Product product = new Product();
		product.setName(name);
		product.setDescription(name + " description");
		product.setPrice(price);
		product.setStock(stock);
		product.setCategory("Test Category");
		product.setVendor(vendor);
		product.setModerationStatus(ProductModerationStatus.APPROVED);
		return productRepository.save(product);
	}

	private Product productWithStatus(String name, ProductModerationStatus moderationStatus, User vendor) {
		Product product = saveProduct(name, new BigDecimal("18.00"), 3, vendor);
		product.setModerationStatus(moderationStatus);
		return productRepository.saveAndFlush(product);
	}

	private void assertProductIsUnavailableToCustomer(String customerToken, Product product) throws Exception {
		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + customerToken)
				.param("search", product.getName()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/products/{id}", product.getId()).header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/products/{id}/reviews", product.getId()))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON).content(cartItemRequest(product.getId(), 1)))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/api/orders/checkout").header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":1}]}"))
			.andExpect(status().isConflict());
		assertEquals(3, productRepository.findById(product.getId()).orElseThrow().getStock());
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

	private String productRequest(String name, String description, String price, int stock) {
		return "{\"name\":\"" + name + "\",\"description\":\"" + description
				+ "\",\"price\":" + price + ",\"stock\":" + stock + ",\"category\":\"Electronics\"}";
	}

	private String cartItemRequest(Long productId, int quantity) {
		return "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
	}

	private Long placeOrder(String customerToken, Long productId) throws Exception {
		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(cartItemRequest(productId, 1)))
			.andExpect(status().isOk());
		MvcResult result = mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private Long addCartItemsAndPlaceOrder(String customerToken, ProductQuantity... products) throws Exception {
		for (ProductQuantity product : products) {
			mockMvc.perform(post("/api/cart/items")
						.header("Authorization", "Bearer " + customerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cartItemRequest(product.productId(), product.quantity())))
				.andExpect(status().isOk());
		}
		MvcResult result = mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + customerToken))
			.andExpect(status().isOk())
			.andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private void payOrder(String customerToken, Long orderId) throws Exception {
		mockMvc.perform(post("/api/orders/{id}/payments", orderId)
					.header("Authorization", "Bearer " + customerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"simulateFailure\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));
	}

	private record ProductQuantity(Long productId, int quantity) {
	}

	private CompletableFuture<MvcResult> checkoutConcurrently(
			String token,
			CountDownLatch ready,
			CountDownLatch start,
			ExecutorService executor) {
		return CompletableFuture.supplyAsync(() -> {
			ready.countDown();
			try {
				if (!start.await(10, TimeUnit.SECONDS)) {
					throw new IllegalStateException("Checkout start signal timed out");
				}
				return mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + token))
					.andReturn();
			} catch (Exception exception) {
				throw new java.util.concurrent.CompletionException(exception);
			}
		}, executor);
	}
}
