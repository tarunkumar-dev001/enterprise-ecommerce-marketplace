# Development demo data

Demo data is disabled by default. To seed a local development database, activate the `dev` Spring profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

For Docker Compose, set `SPRING_PROFILES_ACTIVE=dev` in the environment before running `docker compose up --build`.
Use a non-demo profile and keep `app.demo-data.enabled=false` for production. You can also disable seeding in development with `APP_DEMO_DATA_ENABLED=false`.

The seed uses the existing entities and relationships. It reuses accounts by email, products by vendor/name, orders by their stable `DEMO-ORDER-*` payment-reference marker, reviews by purchased product/customer, and existing cart/wishlist entries. It never deletes or overwrites account, product, order, review, or payout data. A mismatched existing demo email/role fails clearly rather than changing that account.

The development profile uses the shared password `Demo@12345`. The password is encoded through the application's BCrypt password encoder. The application prints demo login names, emails, and the password only when the seed is enabled.

The vendor accounts use the existing `ROLE_VENDOR` role and therefore retain both shopping and vendor capabilities. Store names are represented by vendor usernames because the current model has no separate store entity or store-name field.
