-- Baseline schema for a clean MySQL 8+ marketplace installation.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    version BIGINT NOT NULL,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    review_count BIGINT NOT NULL DEFAULT 0,
    category VARCHAR(255) NOT NULL,
    image_url VARCHAR(2048),
    moderation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    moderation_reason VARCHAR(1000),
    vendor_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FIXED') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    vendor_id BIGINT NULL,
    expires_at DATETIME(6) NULL,
    usage_limit INT NULL,
    usage_count INT NOT NULL,
    active BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupon_code UNIQUE (code),
    CONSTRAINT fk_coupons_vendor FOREIGN KEY (vendor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_carts_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    subtotal_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    coupon_code VARCHAR(32) NULL,
    status ENUM('PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_reference VARCHAR(80) NULL,
    full_name VARCHAR(255) NULL, phone VARCHAR(255) NULL, address_line VARCHAR(255) NULL,
    city VARCHAR(255) NULL, state VARCHAR(255) NULL, postal_code VARCHAR(255) NULL, country VARCHAR(255) NULL,
    placed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sub_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL, vendor_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sub_orders_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_sub_orders_vendor FOREIGN KEY (vendor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL, product_id BIGINT NOT NULL, sub_order_id BIGINT NULL,
    quantity INT NOT NULL, unit_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_sub_order FOREIGN KEY (sub_order_id) REFERENCES sub_orders(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vendor_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sub_order_id BIGINT NOT NULL, vendor_id BIGINT NOT NULL,
    total_order_amount DECIMAL(12,2) NOT NULL, platform_commission_fee DECIMAL(12,2) NOT NULL,
    net_vendor_payout DECIMAL(12,2) NOT NULL,
    payout_status ENUM('PENDING', 'PAID') NOT NULL,
    created_at DATETIME(6) NOT NULL, paid_at DATETIME(6) NULL, version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vendor_ledger_sub_order UNIQUE (sub_order_id),
    CONSTRAINT fk_vendor_ledger_sub_order FOREIGN KEY (sub_order_id) REFERENCES sub_orders(id),
    CONSTRAINT fk_vendor_ledger_vendor FOREIGN KEY (vendor_id) REFERENCES users(id),
    INDEX idx_vendor_ledger_vendor_status (vendor_id, payout_status),
    INDEX idx_vendor_ledger_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, rating INT NOT NULL,
    review_text VARCHAR(2000) NOT NULL, created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_review_customer UNIQUE (product_id, customer_id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL, product_id BIGINT NOT NULL, quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL,
    PRIMARY KEY (id), CONSTRAINT uk_wishlists_user UNIQUE (user_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wishlist_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wishlist_id BIGINT NOT NULL, product_id BIGINT NOT NULL, added_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wishlist_product UNIQUE (wishlist_id, product_id),
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists(id),
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;
