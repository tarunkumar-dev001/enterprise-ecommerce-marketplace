package com.ecommerce.marketplace;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs only when MYSQL_MIGRATION_TEST_URL, MYSQL_MIGRATION_TEST_USERNAME, and
 * MYSQL_MIGRATION_TEST_PASSWORD explicitly point at a local MySQL server URL
 * without a database name, for example jdbc:mysql://localhost:3306/?useSSL=false.
 */
class MySqlModerationMigrationIntegrationTest {

    @Test
    void migratesEmptyAndLegacyProductsSchemasWithoutLosingExistingStatuses() throws Exception {
        String serverUrl = System.getenv("MYSQL_MIGRATION_TEST_URL");
        String username = System.getenv("MYSQL_MIGRATION_TEST_USERNAME");
        String password = System.getenv("MYSQL_MIGRATION_TEST_PASSWORD");
        Assumptions.assumeTrue(serverUrl != null && username != null && password != null,
                "Set MYSQL_MIGRATION_TEST_URL, MYSQL_MIGRATION_TEST_USERNAME, and MYSQL_MIGRATION_TEST_PASSWORD to run MySQL migration tests");
        assertSafeLocalServerUrl(serverUrl);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String emptySchema = "marketplace_migration_empty_" + suffix;
        String legacySchema = "marketplace_migration_legacy_" + suffix;
        String moderatedSchema = "marketplace_migration_moderated_" + suffix;
        try (Connection server = DriverManager.getConnection(serverUrl, username, password); Statement statement = server.createStatement()) {
            statement.execute("CREATE DATABASE `" + emptySchema + "`");
            statement.execute("CREATE DATABASE `" + legacySchema + "`");
            statement.execute("CREATE DATABASE `" + moderatedSchema + "`");
            statement.execute("CREATE TABLE `" + emptySchema + "`.products (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE `" + legacySchema + "`.products (id BIGINT PRIMARY KEY, name VARCHAR(255))");
            statement.execute("CREATE TABLE `" + moderatedSchema + "`.products (id BIGINT PRIMARY KEY, moderation_status VARCHAR(32) NULL, moderation_reason VARCHAR(1000) NULL)");
            statement.execute("INSERT INTO `" + legacySchema + "`.products (id, name) VALUES (1, 'Legacy product')");
            statement.execute("INSERT INTO `" + moderatedSchema + "`.products (id, moderation_status, moderation_reason) VALUES (1, 'SUSPENDED', 'Policy investigation')");

            migrate(schemaUrl(serverUrl, emptySchema), username, password);
            migrate(schemaUrl(serverUrl, legacySchema), username, password);
            migrate(schemaUrl(serverUrl, moderatedSchema), username, password);
            // A second run must be a no-op once Flyway records V1.
            migrate(schemaUrl(serverUrl, moderatedSchema), username, password);

            assertEquals("PENDING_REVIEW", columnDefault(serverUrl, username, password, emptySchema));
            assertEquals("APPROVED", legacyStatus(serverUrl, username, password, legacySchema));
            assertEquals("SUSPENDED", existingStatus(serverUrl, username, password, moderatedSchema));
            assertEquals("Policy investigation", existingReason(serverUrl, username, password, moderatedSchema));
        } finally {
            try (Connection server = DriverManager.getConnection(serverUrl, username, password); Statement statement = server.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + emptySchema + "`");
                statement.execute("DROP DATABASE IF EXISTS `" + legacySchema + "`");
                statement.execute("DROP DATABASE IF EXISTS `" + moderatedSchema + "`");
            }
        }
    }

    private void migrate(String url, String username, String password) {
        Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                // The schemas deliberately contain a legacy products table but no Flyway
                // history. Baselining at 0 lets V1 run; baselining at 1 would skip it.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }

    private void assertSafeLocalServerUrl(String serverUrl) throws Exception {
        if (!serverUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("MYSQL_MIGRATION_TEST_URL must be a jdbc:mysql server URL");
        }
        URI uri = URI.create(serverUrl.substring("jdbc:".length()));
        String host = uri.getHost();
        if (!("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host))) {
            throw new IllegalArgumentException("MYSQL_MIGRATION_TEST_URL must target local MySQL");
        }
        if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
            throw new IllegalArgumentException("MYSQL_MIGRATION_TEST_URL must not select a database");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("MYSQL_MIGRATION_TEST_URL must not contain credentials");
        }
    }

    private String schemaUrl(String serverUrl, String schema) {
        int query = serverUrl.indexOf('?');
        String base = query < 0 ? serverUrl : serverUrl.substring(0, query);
        String parameters = query < 0 ? "" : serverUrl.substring(query);
        int pathStart = base.indexOf('/', "jdbc:mysql://".length());
        String server = pathStart < 0 ? base : base.substring(0, pathStart);
        return server + "/" + schema + parameters;
    }

    private String columnDefault(String url, String username, String password, String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(schemaUrl(url, schema), username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT column_default FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'moderation_status'")) {
            result.next();
            return result.getString(1);
        }
    }

    private String legacyStatus(String url, String username, String password, String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(schemaUrl(url, schema), username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT moderation_status FROM products WHERE id = 1")) {
            result.next();
            return result.getString(1);
        }
    }

    private String existingStatus(String url, String username, String password, String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(schemaUrl(url, schema), username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT moderation_status FROM products WHERE id = 1")) {
            result.next();
            return result.getString(1);
        }
    }

    private String existingReason(String url, String username, String password, String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(schemaUrl(url, schema), username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT moderation_reason FROM products WHERE id = 1")) {
            result.next();
            return result.getString(1);
        }
    }
}
