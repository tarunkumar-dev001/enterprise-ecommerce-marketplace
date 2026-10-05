# Enterprise E-Commerce & Multi-Vendor Marketplace

A Java and React portfolio marketplace demonstrating role-based access, vendor-as-customer shopping, multi-vendor checkout, inventory-safe ordering, reviews, analytics, and an admin portal.

> **Demo payment only:** payments are simulated for demonstration. No real money is charged and no payment provider is connected.

## Live Demo

Frontend: https://YOUR-NETLIFY-URL  
Backend: https://YOUR-RAILWAY-URL

This is a demonstration project. Payment is simulated and no real money is charged.

### Demo Accounts

These are public development/demo accounts. The login form accepts the username shown below; email is listed for account identification. All use the shared demo password `Demo@12345`.

| Role | Email | Login username |
|---|---|---|
| Admin | `admin@marketplace.demo` | `admin` |
| Vendor | `technova@marketplace.demo` | `TechNova Store` |
| Customer | `rahul@marketplace.demo` | `rahul` |

The accounts and their data are disposable demonstration data. Do not use these credentials for any other service.

### Deployment configuration

Netlify uses `frontend` as its base directory. The included `frontend/netlify.toml` builds with `npm run build`, publishes `dist`, and rewrites client-side routes to `index.html`. Set the Netlify build environment variable `VITE_API_BASE_URL` to `https://YOUR-RAILWAY-URL/api` (replace the placeholder with the Railway public backend URL). Vite variables are embedded in the public frontend bundle; never put credentials or secrets in `VITE_*` variables.

For Railway, deploy the repository's `marketplace` directory with the included Java 17 `Dockerfile`, add a Railway MySQL service, then set these variables on the backend service:

| Variable | Value |
|---|---|
| `PORT` | Railway-provided port (Railway normally injects this) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` (replace `MySQL` with the actual database service name) |
| `SPRING_DATASOURCE_USERNAME` | `${{MySQL.MYSQLUSER}}` reference variable |
| `SPRING_DATASOURCE_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` reference variable |
| `JWT_SECRET` | A private Base64-encoded random key containing at least 32 bytes; generate a different value for deployment |
| `FRONTEND_URL` | The exact Netlify origin, such as `https://your-site.netlify.app` (no path or trailing slash) |
| `SPRING_PROFILES_ACTIVE` | `dev` only when the public demo data is wanted |
| `APP_DEMO_DATA_ENABLED` | `true` only when using the `dev` profile and demo data is wanted; otherwise `false` |

Railway MySQL provides `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, and `MYSQLDATABASE`; use Railway reference variables so database credentials are not copied into source files. Keep `SPRING_JPA_DDL_AUTO` at its default only for this resume/demo deployment; the project does not yet include a production schema migration tool. Configure a Railway health check for `/api/health`. After setting variables and generating the two public service domains, replace the placeholders in this README. No services are deployed by these instructions.

## Features

- Browse and search products, manage a cart and wishlist, checkout, track orders, and review purchased products.
- Vendors keep all customer shopping capabilities alongside store, product, order, analytics, earnings, and review tools.
- Multi-vendor carts split into vendor suborders and record vendor commission ledger entries.
- Admin dashboard for users, vendors, products, orders, ledger payouts, and reviews.
- Development-only, idempotent demo data seeding.

## Architecture

```text
React + TypeScript + Vite
        │ REST / JSON + JWT
        ▼
Spring Boot controllers → services → Spring Data JPA repositories
        │
        ▼
      MySQL
```

The backend uses DTOs at the API boundary, transaction-scoped checkout and inventory locks, and method-level Spring Security authorization. The frontend has a shared Axios client, role-aware routes, and separate customer, vendor, and admin pages.

## Tech stack

- Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA, Hibernate, Maven
- MySQL 8
- React, TypeScript, Vite, React Router, Tailwind CSS, Three.js / React Three Fiber

## Roles

| Role | Capabilities |
|---|---|
| Customer (`ROLE_USER`) | Product browsing, cart, wishlist, checkout, orders, and reviews for purchased products |
| Vendor (`ROLE_VENDOR`) | All customer shopping features plus vendor dashboard and store management |
| Admin (`ROLE_ADMIN`) | Admin portal and platform-wide read and payout operations |

The backend remains authoritative for access checks; frontend route guards only shape navigation.

## Customer features

- Product catalog, filters, product details, ratings, and verified-purchase reviews
- Cart, coupons, wishlist, multi-vendor shipping checkout, and demo payment
- Order list, order detail and status timeline, cancellation before shipment, and paid demo invoices

## Vendor features

- Store profile and product management
- Vendor order fulfillment statuses, analytics, reviews, earnings, and payout history
- The same marketplace browsing, cart, wishlist, checkout, order, and review abilities as customers

## Admin features

- Dashboard, users, vendors, products, orders, payouts, and reviews
- Admin access is restricted by Spring Security roles and method authorization

## Multi-vendor checkout

Checkout captures one shipping address for the full cart. The server snapshots the address and creates a parent order plus vendor suborders. It locks products in deterministic ID order, checks available stock, and decrements inventory in the checkout transaction. The demo payment action is separate from order placement and is clearly labeled as a test transaction.

The simple demo order lifecycle is `PLACED → PROCESSING → SHIPPED → DELIVERED`. Orders may be cancelled before shipping; cancellation restores reserved stock. Payment refunds are not automated. Vendor ledger payouts are only released for paid, non-cancelled orders.

## Security

- JWT bearer authentication with database-loaded authorities
- BCrypt password encoding
- Role-based authorization for admin and vendor APIs
- Ownership checks for customer orders and vendor-managed records
- No payment card data is collected or stored
- Development demo seeding is guarded by the `dev` profile and its enablement property

## Database design

Core relationships include users to products, carts, wishlists, and orders; orders to order items and vendor suborders; suborders to vendor ledger records; and products to reviews. Order items retain the purchase-time unit price. Hibernate schema update is intended for local/demo convenience; no production migration framework is configured.

## API overview

| Area | Routes |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login` |
| Catalog and reviews | `/api/products`, `/api/products/{id}`, `/api/products/{id}/reviews` |
| Customer | `/api/cart/**`, `/api/orders/**`, `/api/wishlist/**`, `/api/users/me` |
| Vendor | `/api/vendor/products`, `/api/vendor/orders`, `/api/vendor/analytics`, `/api/vendor/ledger`, `/api/vendor/reviews` |
| Admin | `/api/admin/**`, `GET /api/admin/ledger/all`, `POST /api/admin/payouts/{vendorId}` |

## Screenshots

Add screenshots of the marketplace homepage, checkout/order detail, vendor dashboard, and admin dashboard here.

## Demo credentials

Enable the `dev` profile and demo seeder to create these development-only users. The shared demo password is `Demo@12345`.

| Role | Username |
|---|---|
| Admin | `admin` |
| Vendor | `TechNova Store`, `UrbanStyle`, `HomeNest` |
| Customer | `rahul`, `priya`, `ananya`, `rohan`, `sneha` |

These accounts are disposable development/demo records; the deployment section above explains when to enable their seeder.

## How to run

### Start MySQL and Mailpit with Docker Compose

1. Copy `.env.example` to `.env` and replace every placeholder with local development values.
2. Create a Base64 encoded JWT key with at least 32 random bytes. For example, in PowerShell: `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))`.
3. Start services with `docker compose up --build`.
4. Open the frontend separately from the `frontend` directory with the steps below. The API listens on port `8080`; Mailpit’s web UI listens on `8025`.

### Run backend and frontend locally

1. Start a local MySQL 8 database named `marketplace`.
2. Set `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` in the environment. Set `SPRING_PROFILES_ACTIVE=dev` to load demo data; the dev profile enables the idempotent seeder by default.
3. In the project directory, run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
4. In `frontend`, run `npm install` and `npm run dev`.

The frontend `.env.example` points to the local backend at `http://localhost:8080/api`. To use Vite's same-origin proxy instead, leave `VITE_API_BASE_URL` unset (or set it to `/api`); `VITE_PROXY_TARGET` defaults to `http://localhost:8080`.

## Environment variables

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Local MySQL credentials |
| `SPRING_DATASOURCE_URL` | Optional JDBC URL override |
| `JWT_SECRET` | Base64 encoded JWT signing key; required by the backend |
| `SPRING_PROFILES_ACTIVE` | Set to `dev` to enable development settings |
| `APP_DEMO_DATA_ENABLED` | Enable or disable demo seeding within the dev profile |
| `MARKETPLACE_COMMISSION_RATE` | Platform commission rate |
| `VITE_API_BASE_URL`, `VITE_PROXY_TARGET` | Frontend API base and local Vite proxy target |
| `FRONTEND_URL` | Exact allowed browser origin for backend CORS; defaults to `http://localhost:5173` |
| `PORT` | Backend listen port; defaults to `8080` |

## Project structure

```text
src/main/java/.../controller   REST endpoints
src/main/java/.../service      Marketplace workflows and business rules
src/main/java/.../entity       JPA entities
src/main/java/.../repository   Spring Data JPA repositories
src/main/java/.../security     JWT and security configuration
src/test/java                  Backend integration and seeder tests
frontend/src                   React application, API client, pages, and components
```

## Testing

```powershell
.\mvnw.cmd -q test
cd frontend
npm run lint
npx tsc -b
npm run build
```

## Future improvements

- Add frontend component and end-to-end tests for the customer, vendor, and admin journeys.
- Add pagination to growing admin and vendor lists.
- Add deployment-specific schema management and external payment integration only if the project moves beyond a demo.
- Add project screenshots and a short checkout walkthrough video.
