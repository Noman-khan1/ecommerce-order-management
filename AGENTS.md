# AGENTS.md

## Purpose

This file documents how AI assistance was used while developing the E-commerce Order Management take-home assignment.

The AI assistant was used as a development aid for design discussion, implementation drafting, edge-case analysis, testing ideas, code review, and documentation.

It was not treated as the source of truth for the final implementation.

---

## AI Agent Used

**Primary AI assistant:** ChatGPT by OpenAI.

Development itself was performed locally using:

* IntelliJ IDEA
* Maven
* Git
* GitHub

ChatGPT did not directly own or automatically push changes to the repository.

Repository snapshots, source files, test output, and Git status information were reviewed during the development process, and changes were manually applied and committed locally.

---

## How I Used the Agent

The project was implemented incrementally rather than asking the AI to generate the complete application at once.

For each feature, the workflow was approximately:

```text
Requirement
    ↓
Discuss design / trade-offs
    ↓
Define one focused commit
    ↓
Generate/review implementation
    ↓
Apply code locally
    ↓
Run complete Maven test suite
    ↓
Review staged files
    ↓
Fix issues if required
    ↓
Commit and push
```

Examples of areas where AI assistance was used:

* domain modeling
* checkout transaction design
* inventory concurrency discussion
* pessimistic-locking strategy
* deterministic lock ordering
* order-state transition rules
* asynchronous event-processing design
* return/refund flow
* integration-test scenarios
* API error handling
* documentation review

---

## Development Constraints Given to the Agent

The implementation was intentionally kept inside the assignment scope.

The main constraints used during development were:

### Architecture

Use a **modular monolith**.

Do not introduce microservices merely for architectural complexity.

Keep business areas separated by feature packages such as:

```text
catalog
cart
inventory
order
payment
fulfillment
returns
notification
audit
```

---

### Technology

Use:

```text
Java 17
Spring Boot
Spring MVC
Spring Data JPA
Spring Security
MySQL
H2 for tests
Maven
```

Avoid infrastructure that was not required by the assignment.

---

### Security

Use HTTP Basic authentication with:

```text
ADMIN
CUSTOMER
WAREHOUSE_STAFF
```

Authorization:

```text
/api/admin/**     → ADMIN
/api/customer/**  → CUSTOMER
/api/warehouse/** → WAREHOUSE_STAFF
```

JWT/OAuth was intentionally not added because advanced authentication was outside the required scope.

---

### Inventory

Inventory belongs to:

```text
SKU + Warehouse
```

and maintains:

```text
availableQuantity
reservedQuantity
```

Cart operations must not reserve inventory.

Stock is reserved during checkout.

---

### Concurrency

The assignment explicitly requires avoiding overselling.

The chosen approach uses database pessimistic locking rather than Java `synchronized`.

Reason:

```text
synchronized
```

protects only one JVM, while a database lock coordinates all application instances using the same database.

For multiple inventory locks, deterministic ordering is used to reduce deadlock risk.

---

### Checkout

Checkout should atomically coordinate local database state for:

```text
cart
inventory reservation
order
order items
payment state
cart clearing
```

If a step fails, database changes must roll back.

Payment is simulated for this assignment.

---

### Order Lifecycle

Normal flow:

```text
PLACED
→ CONFIRMED
→ PACKED
→ SHIPPED
→ DELIVERED
```

Return flow:

```text
DELIVERED
→ RETURNED
```

Skipped or backward fulfillment transitions should be rejected.

---

### Async Processing

Downstream work should not block checkout.

The selected approach uses:

```text
ApplicationEventPublisher
@TransactionalEventListener(AFTER_COMMIT)
@Async
```

for:

* fulfillment routing
* notification logging
* audit logging

Event payloads use stable values such as IDs/status rather than passing managed JPA entities to async threads.

---

### Returns

The assignment implementation supports a **full-order return**.

A valid return must:

```text
lock order
validate ownership
validate DELIVERED status
refund payment
restore inventory
create return record
set order to RETURNED
```

Duplicate refunds and duplicate restocking must be prevented.

---

## Human Review and Verification

AI output was treated as a draft.

Before accepting a feature, I verified it through the actual project.

The normal validation process was:

1. apply the change locally
2. compile the project
3. run the complete Maven test suite
4. inspect failures rather than assuming generated code was correct
5. inspect the exact files staged for the commit
6. review Git status/diff
7. commit only after the project was passing

Command used repeatedly:

```powershell
.\mvnw.cmd test
```

The final test suite contains:

```text
18 tests
```

with coverage for the core business flows, concurrency, async processing, returns/refunds, security, and API error handling.

---

## Repository-Snapshot Review

During development, updated repository snapshots and test output were provided to the AI assistant for review.

This was useful for checking:

* whether the intended files were staged
* whether a proposed change matched the current codebase
* whether compilation/test errors came from missing or incompatible code
* whether new code unintentionally changed previous behavior

The local repository remained under manual control.

---

## Examples of Engineering Decisions Discussed with AI

### Pessimistic vs JVM Locking

The inventory requirement needed coordination across potentially multiple application servers.

Database pessimistic locking was selected instead of `synchronized`.

---

### Cart vs Checkout Reservation

Inventory is not reserved when an item is added to a cart.

Reservation occurs during checkout to avoid abandoned carts holding inventory.

---

### Async AFTER_COMMIT Processing

Notification, audit, and fulfillment-routing work is started only after the order transaction commits.

This prevents downstream records for rolled-back orders.

---

### Spring Events vs Kafka

Spring application events were chosen for the take-home because they keep the architecture small and explainable.

The limitation is acknowledged: application events are not durable messaging.

A production evolution could use:

```text
Transactional Outbox
→ Kafka / Message Broker
→ Consumers
```

---

### Simulated Payment

A simulated local payment flow was used because integrating an external payment provider was outside the assignment scope.

For a real provider, the design would require stronger idempotency, retries, reconciliation, and distributed transaction/compensation handling.

---

### Full vs Partial Returns

Full-order return was selected for the assignment.

Partial-item and quantity-level returns would require additional return-item modeling and more complex refund calculations.

That complexity was intentionally kept outside the current scope.

---

## AI Usage Boundaries

The AI assistant was not relied upon to:

* determine whether code compiled without running it
* determine whether concurrency worked without tests
* directly modify the local Git repository
* automatically push commits
* provide production credentials
* hide known limitations or trade-offs

When generated code produced an issue, the implementation was corrected and the test suite was run again before committing.

---

## Testing Philosophy

Important business behavior is covered using integration tests rather than only mocked unit tests.

This is particularly important for:

* JPA mappings
* transaction rollback
* database constraints
* pessimistic locking
* concurrent checkout
* AFTER_COMMIT event behavior

The concurrency test uses actual separate threads so both checkout attempts compete for shared inventory state.

MockMvc is used for REST security and error-contract testing.

---

## Current Scope Decisions

The following are deliberate assignment-level choices:

| Area                    | Current Choice      | Possible Production Evolution      |
| ----------------------- | ------------------- | ---------------------------------- |
| Architecture            | Modular monolith    | Split services only when justified |
| Authentication          | HTTP Basic          | OAuth2/OIDC/JWT                    |
| Payment                 | Simulated           | External payment provider          |
| Messaging               | Spring async events | Outbox + Kafka                     |
| Returns                 | Full-order          | Partial/quantity returns           |
| Schema management       | Hibernate           | Flyway/Liquibase                   |
| Warehouse authorization | Role based          | Per-warehouse assignment           |

These choices keep the solution focused on the requirements while leaving clear paths for production evolution.

---

## Final Principle

The AI assistant was used to accelerate development and challenge design decisions, but the final code was accepted only after it was integrated into the actual repository and validated with the project's tests.

The goal was not to maximize the amount of generated code.

The goal was to produce a solution that is **correct, explainable, testable, and aligned with the assignment scope**.
