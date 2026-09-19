# E-commerce Order Management

A Spring Boot backend for an e-commerce order-management system covering catalog management, shopping cart, multi-warehouse inventory, checkout, payment, fulfillment, returns, refunds, concurrency control, role-based access, and asynchronous downstream processing.

The project is implemented as a **modular monolith** with a strong focus on transaction correctness, inventory consistency, and clean separation of business domains.

---

## Key Features

### Admin

* Manage categories, products, and SKUs
* Activate/deactivate products and SKUs
* Manage warehouses
* Manage inventory per SKU and warehouse
* Create and manage discount codes

### Customer

* Browse product catalog
* Add/update/remove cart items
* Preview pricing with discount and tax
* Checkout and place orders
* Track order history and current status
* Return delivered orders
* Receive simulated refunds

### Warehouse Staff

* View orders waiting for fulfillment
* Move orders through the fulfillment lifecycle:

    * `CONFIRMED`
    * `PACKED`
    * `SHIPPED`
    * `DELIVERED`

### System

* Multi-warehouse inventory
* Percentage and fixed discounts
* Configurable tax calculation
* Transactional checkout
* Concurrent purchase protection
* Duplicate checkout protection
* Simulated payment/refund processing
* Async fulfillment routing
* Async notification logging
* Async audit logging
* Consistent REST error responses
* Role-based API authorization

---

## Tech Stack

| Area                  | Technology                         |
| --------------------- | ---------------------------------- |
| Language              | Java 17                            |
| Framework             | Spring Boot 4.1.1                  |
| Web                   | Spring MVC                         |
| Persistence           | Spring Data JPA / Hibernate        |
| Runtime Database      | MySQL                              |
| Test Database         | H2 with MySQL compatibility mode   |
| Security              | Spring Security + HTTP Basic       |
| Password Hashing      | BCrypt                             |
| Validation            | Jakarta Bean Validation            |
| Async Processing      | Spring `@Async`                    |
| Transaction Events    | `@TransactionalEventListener`      |
| Build Tool            | Maven                              |
| Testing               | JUnit 5, Spring Boot Test, MockMvc |
| Boilerplate Reduction | Lombok                             |

---

## Architecture

The project uses a **modular monolith**.

```text
com.noman.ecommerce_order_management
│
├── catalog
├── cart
├── warehouse
├── inventory
├── discount
├── pricing
├── order
├── payment
├── fulfillment
├── returns
├── notification
├── audit
├── security
├── exception
└── user
```

A modular monolith was chosen instead of microservices because the most important flows in this assignment—especially checkout, inventory reservation, payment state, and returns—require strong transactional consistency.

It also keeps the system easy to run and explain while preserving clear domain boundaries.

---

## Core Domain Model

```text
Category
   ↓
Product
   ↓
SKU
   ↓
Inventory ← Warehouse


Customer
   ↓
Cart → CartItems
   ↓
Checkout
   ↓
CustomerOrder
   ├── OrderItems
   ├── Payment
   ├── FulfillmentTasks
   └── OrderReturn
```

A **Product** represents the generic product.

A **SKU** represents the actual purchasable variant and contains the price.

Inventory is therefore maintained at:

```text
SKU + Warehouse
```

level.

---

## Inventory Model

Each inventory record maintains:

```text
availableQuantity
reservedQuantity
```

Example:

```text
Initial stock:

available = 10
reserved  = 0
```

Customer checks out quantity `2`:

```text
available = 8
reserved  = 2
```

When the warehouse ships the order:

```text
available = 8
reserved  = 0
```

If the delivered order is returned:

```text
available = 10
reserved  = 0
```

Inventory is **not reserved when an item is added to the cart** because abandoned carts should not block stock indefinitely.

Reservation happens during checkout.

---

## Preventing Inventory Overselling

Concurrent checkout is one of the key correctness requirements of the project.

Inventory reservation uses database-level:

```text
PESSIMISTIC_WRITE
```

locking.

Example with one unit remaining:

```text
Customer A                Customer B
    │                          │
    └────── checkout ──────────┘
               │
         inventory row lock
               │
        Customer A wins
               │
     available: 1 → 0
     reserved:  0 → 1
               │
             COMMIT
               │
     Customer B gets lock
               │
       sees available = 0
               │
          409 Conflict
```

This works across multiple application threads and application instances sharing the same database.

A Java `synchronized` block was intentionally not used because it would only coordinate requests inside one JVM.

---

## Deadlock Reduction

For operations that may lock multiple inventory rows, locks are acquired in deterministic order.

Checkout orders reservations by:

```text
SKU ID
```

Fulfillment and returns use:

```text
SKU ID → Warehouse ID
```

This reduces the chance of two transactions acquiring the same locks in opposite order.

---

## Checkout Flow

Checkout runs inside a database transaction.

```text
Validate request
      ↓
Lock customer cart
      ↓
Read cart items
      ↓
Calculate subtotal
      ↓
Apply discount
      ↓
Calculate tax
      ↓
Create order
      ↓
Lock and reserve inventory
      ↓
Create order items
      ↓
Process simulated payment
      ↓
Order → CONFIRMED
      ↓
Clear cart
      ↓
Publish order event
      ↓
COMMIT
```

If any database operation fails, the transaction rolls back.

This prevents inconsistent states such as:

```text
Order created but inventory not reserved
```

or:

```text
Inventory reserved but payment/order state missing
```

The payment provider itself is simulated for the assignment, so payment state participates in the same local transaction.

---

## Duplicate Checkout Protection

The customer's cart is pessimistically locked during checkout.

If the same customer submits checkout twice concurrently:

```text
Request A
Request B
```

one request obtains the cart lock first.

The successful transaction clears the cart.

When the second request continues, it sees an empty cart and fails instead of creating another order.

---

## Pricing

Pricing follows:

```text
Subtotal
   ↓
Discount
   ↓
Taxable Amount
   ↓
Tax
   ↓
Final Total
```

The configured tax rate is:

```text
18%
```

Discounts support:

* percentage discount
* fixed-amount discount
* minimum-order requirement
* optional maximum discount cap
* validity period
* activation/deactivation

Calculated pricing values are copied into the order so historical orders remain unchanged if pricing rules change later.

---

## Payment

Payment processing is simulated.

A successful checkout creates a payment with:

```text
PaymentStatus.SUCCESS
```

and a generated reference such as:

```text
PAY-...
```

A successful return changes it to:

```text
PaymentStatus.REFUNDED
```

with a generated:

```text
REF-...
```

reference.

### Production Consideration

For a real external payment provider, I would not keep a database transaction open while waiting for the remote service.

A production design would use concepts such as:

* idempotency keys
* Saga/compensation
* transactional outbox
* retry/reconciliation processing

---

## Order Lifecycle

Normal order flow:

```text
PLACED
   ↓
CONFIRMED
   ↓
PACKED
   ↓
SHIPPED
   ↓
DELIVERED
```

Return flow:

```text
DELIVERED
   ↓
RETURNED
```

Warehouse staff can perform only:

```text
CONFIRMED → PACKED
PACKED    → SHIPPED
SHIPPED   → DELIVERED
```

Invalid transitions such as:

```text
CONFIRMED → SHIPPED
```

are rejected.

---

## Fulfillment

When checkout succeeds, every order item contains the warehouse selected during inventory reservation.

After the order transaction commits, asynchronous fulfillment routing creates one fulfillment task for each unique warehouse involved in the order.

Example:

```text
Order
├── SKU A → Warehouse 1
├── SKU B → Warehouse 1
└── SKU C → Warehouse 2
```

creates:

```text
Task → Warehouse 1
Task → Warehouse 2
```

A database uniqueness constraint on:

```text
order + warehouse
```

helps prevent duplicate fulfillment tasks.

---

## Asynchronous Downstream Processing

Order status changes publish an `OrderStatusChangedEvent`.

Downstream handlers use:

```text
@TransactionalEventListener(AFTER_COMMIT)
+
@Async
```

Flow:

```text
Order transaction
       ↓
Publish event
       ↓
COMMIT
       ↓
Async processing
       ├── Fulfillment routing
       ├── Customer notification log
       └── Audit log
```

Using `AFTER_COMMIT` ensures a failed transaction does not create downstream work for an order that was rolled back.

Using `@Async` means these operations do not block the checkout or fulfillment request.

### Production Consideration

Spring application events are process-local rather than durable messaging.

For stronger delivery guarantees, this could evolve to:

```text
Transactional Outbox
        ↓
Kafka / Message Broker
        ↓
Consumers
```

---

## Returns and Refunds

The current scope supports **full-order returns**.

Rules:

* only a `DELIVERED` order can be returned
* the authenticated customer must own the order
* the order can only be returned once
* the full payment amount is refunded
* stock is returned to the original warehouse
* returned products are assumed to be immediately sellable

Flow:

```text
Lock order
    ↓
Validate ownership and status
    ↓
Lock payment
    ↓
SUCCESS → REFUNDED
    ↓
Lock inventory
    ↓
Restock original warehouse
    ↓
Create return record
    ↓
Order → RETURNED
    ↓
COMMIT
```

The same transaction prevents duplicate refunds and duplicate inventory restocking.

---

## Security

The application uses:

* Spring Security
* HTTP Basic
* BCrypt password hashing
* stateless sessions

API authorization:

| API                 | Role              |
| ------------------- | ----------------- |
| `/api/admin/**`     | `ADMIN`           |
| `/api/customer/**`  | `CUSTOMER`        |
| `/api/warehouse/**` | `WAREHOUSE_STAFF` |

HTTP Basic was selected because advanced authentication such as OAuth/JWT was outside the assignment scope.

---

## Local Demo Users

The application creates the following users if they do not already exist:

| Role            | Email                     | Password       |
| --------------- | ------------------------- | -------------- |
| Admin           | `admin@ecommerce.com`     | `admin123`     |
| Customer        | `customer@ecommerce.com`  | `customer123`  |
| Warehouse Staff | `warehouse@ecommerce.com` | `warehouse123` |

These credentials are intended only for local evaluation.

---

## API Overview

### Admin

| Method | Endpoint                                                        | Purpose                     |
| ------ | --------------------------------------------------------------- | --------------------------- |
| POST   | `/api/admin/catalog/categories`                                 | Create category             |
| PUT    | `/api/admin/catalog/categories/{categoryId}`                    | Update category             |
| POST   | `/api/admin/catalog/products`                                   | Create product              |
| PUT    | `/api/admin/catalog/products/{productId}`                       | Update product              |
| PATCH  | `/api/admin/catalog/products/{productId}/status?active={value}` | Activate/deactivate product |
| POST   | `/api/admin/catalog/products/{productId}/skus`                  | Create SKU                  |
| PUT    | `/api/admin/catalog/skus/{skuId}`                               | Update SKU                  |
| PATCH  | `/api/admin/catalog/skus/{skuId}/status?active={value}`         | Activate/deactivate SKU     |
| POST   | `/api/admin/warehouses`                                         | Create warehouse            |
| PUT    | `/api/admin/warehouses/{warehouseId}`                           | Update warehouse            |
| PATCH  | `/api/admin/warehouses/{warehouseId}/status?active={value}`     | Update warehouse status     |
| GET    | `/api/admin/warehouses`                                         | List warehouses             |
| POST   | `/api/admin/inventory`                                          | Create inventory            |
| PUT    | `/api/admin/inventory/{inventoryId}/stock`                      | Update stock                |
| GET    | `/api/admin/inventory`                                          | View/filter inventory       |
| POST   | `/api/admin/discounts`                                          | Create discount             |
| PUT    | `/api/admin/discounts/{discountId}`                             | Update discount             |
| PATCH  | `/api/admin/discounts/{discountId}/status?active={value}`       | Update discount status      |
| GET    | `/api/admin/discounts`                                          | List discounts              |

### Customer

| Method | Endpoint                                     | Purpose                |
| ------ | -------------------------------------------- | ---------------------- |
| GET    | `/api/customer/catalog/categories`           | Browse categories      |
| GET    | `/api/customer/catalog/products`             | Browse/filter products |
| GET    | `/api/customer/catalog/products/{productId}` | Product details        |
| GET    | `/api/customer/cart`                         | View cart              |
| POST   | `/api/customer/cart/items`                   | Add item               |
| PUT    | `/api/customer/cart/items/{itemId}`          | Update quantity        |
| DELETE | `/api/customer/cart/items/{itemId}`          | Remove item            |
| DELETE | `/api/customer/cart`                         | Clear cart             |
| GET    | `/api/customer/pricing/preview`              | Preview pricing        |
| POST   | `/api/customer/orders/checkout`              | Checkout               |
| GET    | `/api/customer/orders`                       | Order history          |
| GET    | `/api/customer/orders/{orderId}`             | Order details          |
| POST   | `/api/customer/orders/{orderId}/return`      | Return delivered order |
| GET    | `/api/customer/orders/{orderId}/return`      | Return details         |

Pricing preview optionally accepts:

```text
?discountCode={code}
```

### Warehouse Staff

| Method | Endpoint                                 | Purpose                   |
| ------ | ---------------------------------------- | ------------------------- |
| GET    | `/api/warehouse/orders`                  | View fulfillment queue    |
| PATCH  | `/api/warehouse/orders/{orderId}/status` | Update fulfillment status |

---

## Error Handling

The API uses a common error structure.

Example:

```json
{
  "timestamp": "2026-09-19T21:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/customer/cart/items",
  "validationErrors": {
    "skuId": "SKU id is required",
    "quantity": "Quantity must be at least 1"
  }
}
```

The same structure is used for security failures such as `401` and `403`.

Unexpected server errors return a safe generic response while the actual exception is logged server-side.

---

## Database Configuration

Runtime database:

```text
MySQL
```

Default application configuration:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/ecommerce_order_management?createDatabaseIfNotExist=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
```

The following environment variables can override the defaults:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Tests use an isolated in-memory H2 database in MySQL compatibility mode.

---

## Running Locally

### Requirements

* Java 17+
* MySQL
* Maven Wrapper included in the repository

Start MySQL on port `3306`.

The application can create the `ecommerce_order_management` database automatically if the configured MySQL user has permission.

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

## Running Tests

### Windows

```powershell
.\mvnw.cmd test
```

### Linux/macOS

```bash
./mvnw test
```

Current suite:

```text
18 tests
```

covering:

* application startup
* catalog
* cart
* inventory
* pricing
* checkout
* payment
* concurrent last-unit purchase
* duplicate concurrent checkout
* fulfillment transitions
* async downstream processing
* returns/refunds
* inventory restoration
* role-based security
* REST error handling

The concurrency test starts separate threads competing for the same stock rather than mocking the behavior.

---

## Important Scope Decisions

To keep the take-home focused and explainable:

1. Payment and refund providers are simulated.
2. Authentication uses HTTP Basic instead of JWT/OAuth.
3. Returns are full-order returns.
4. Returned inventory is considered immediately sellable.
5. Warehouse staff authorization is role-based rather than tied to an individual warehouse.
6. Spring application events are used instead of Kafka.
7. Hibernate schema management is used instead of Flyway/Liquibase.
8. The implementation is a modular monolith rather than microservices.

These are deliberate scope decisions rather than production recommendations.

---

## What I Would Improve for Production

With a larger scope, I would consider:

* Flyway or Liquibase migrations
* OAuth2/OIDC authentication
* idempotency keys for checkout/payment/refund requests
* transactional outbox
* Kafka or another durable broker
* payment-provider reconciliation
* retry/dead-letter handling
* partial-item returns
* returned-stock inspection/quarantine
* pagination
* observability and distributed tracing
* structured application metrics

---

## Engineering Areas Worth Reviewing

If reviewing the repository quickly, these are the most important parts:

### Concurrency and inventory correctness

```text
InventoryRepository
InventoryService
CheckoutService
ConcurrentCheckoutIntegrationTest
```

### Checkout atomicity

```text
CheckoutService
PaymentService
CustomerOrder
OrderItem
```

### Fulfillment

```text
FulfillmentService
FulfillmentRoutingService
FulfillmentTask
```

### Async processing

```text
AsyncConfig
OrderStatusChangedEvent
OrderStatusAsyncListener
```

### Returns/refunds

```text
ReturnService
PaymentService
InventoryService
```

### API/security handling

```text
SecurityConfig
GlobalExceptionHandler
RestAuthenticationEntryPoint
RestAccessDeniedHandler
```

---

## AI-Assisted Development

AI assistance was used during development for architecture discussion, implementation drafts, edge-case analysis, test scenarios, and code review.

AI-generated suggestions were treated as drafts rather than automatically accepted code. Changes were manually integrated and validated through compilation, the full Maven test suite, Git diff/status review, and incremental commits.

See [`AGENTS.md`](AGENTS.md) for the detailed AI-assisted development workflow and constraints.
