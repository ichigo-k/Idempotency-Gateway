# Idempotency Gateway – Pay Once Protocol

## Overview

This project implements an **Idempotency Layer** for payment processing.
It ensures that a payment request is processed **exactly once**, even if the client retries due to network failures or timeouts.


---

## Architecture

The system is fully containerized using Docker and orchestrated with Docker Compose for easy setup and reproducible environments.

The services include:

* **Spring Boot Application** – Implements the API, idempotency logic, and payment processing simulation.
* **PostgreSQL** – Persists idempotency records, request hashes, and stored responses.
* **Redis** – Provides fast lookups and assists with concurrency control during in-flight requests.
* **Docker Compose** – Orchestrates all services, allowing the entire system to be started with a single command.


### High-Level Flow

1. Client sends request with:

  * Idempotency-Key
  * Client-Id
2. System checks if record exists
3. If not:
  * Create record with status `IN_PROGRESS`
  * Process payment
  * Save response
4. If duplicate:
  * Return stored response immediately
5. If same key but different body:
  * Return `409 Conflict`

---

## Sequence Flow 



---

## Setup Instructions

### Prerequisites

* Docker
* Docker Compose
* Git

### Steps

Clone the repository:

```bash
git clone https://github.com/ichigo-k/Idempotency-Gateway.git
cd idempotency-gateway
```

Start the system:

```bash
docker compose up --build
```

The API will be available at:

```
http://localhost:8080/api/v1/process-payment
```

---

## API Documentation

### Endpoint

```
POST /api/v1/process-payment
```

### Headers Required

```
Idempotency-Key: unique-string
Client-Id: clientA
Content-Type: application/json
```

### Request Body Example

```json
{
  "amount": 100,
  "currency": "GHS"
}
```

---

## Sample cURL Request

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
-H "Idempotency-Key: test123" \
-H "Client-Id: clientA" \
-H "Content-Type: application/json" \
-d '{
  "amount": 100,
  "currency": "GHS"
}'
```

---

## Example Responses

### First Request

```
201 Created
{
  "message": "Charged 100 GHS"
}
```

---

### Duplicate Request

```
200 OK
X-Cache-Hit: true
{
  "message": "Charged 100 GHS"
}
```

---

### Same Key Different Payload

```
409 Conflict
{
  "message": "Idempotency key already used for a different request body."
}
```

---

## Design Decisions

### Why PostgreSQL?

PostgreSQL is used as the primary datastore to guarantee **durability and correctness** of idempotency records.

Key reasons:
- **Strong consistency** ensures that once a payment response is stored, it can be safely returned for all subsequent duplicate requests.
- **Transactional guarantees** allow atomic creation and updates of idempotency records, which is critical for preventing double execution.
- **Persistence across restarts** ensures that idempotency is preserved even if the application crashes or restarts.

PostgreSQL stores:
- Idempotency keys (scoped by client)
- Request hashes
- Processing status (e.g., `IN_PROGRESS`, `COMPLETED`)
- Final response payload and HTTP status

---

### Why Redis?

Redis is used as a complementary in-memory store to improve performance and handle concurrency.

It serves multiple purposes:
- **Fast access** for frequently checked idempotency keys, reducing latency compared to database-only lookups.
- **Concurrency coordination** during in-flight requests, helping prevent multiple threads from processing the same payment simultaneously.
- **Database load reduction**, as repeated duplicate requests can be resolved without hitting PostgreSQL.

Redis is particularly useful during high traffic scenarios where multiple retries may arrive within a short time window.

---

### Why PostgreSQL + Redis Together?

Using both systems allows the design to balance **performance and reliability**:

- Redis handles speed and concurrency
- PostgreSQL guarantees durability and correctness

If Redis data is lost, PostgreSQL remains the source of truth.
If PostgreSQL is under heavy load, Redis absorbs frequent duplicate reads.

This layered approach reflects patterns commonly used in real-world payment systems.

---

### Why Docker Compose?

Docker Compose is used to fully containerize the system and simplify setup and execution.

Benefits include:
- **Single-command startup** of all required services (API, Redis, PostgreSQL).
- **Consistent environments** across development and review, eliminating “works on my machine” issues.
- **Clear service isolation**, making it easy to understand and manage system dependencies.
- **Improved reproducibility**, ensuring reviewers can run the system exactly as intended.

Docker Compose also mirrors production-style deployments, making the project easier to extend or deploy in real environments.


---
## Bonus Feature – Client-Scoped Idempotency Keys

### Overview

This system implements **client-scoped idempotency keys**, meaning that an idempotency record is uniquely identified by the combination:

```
(Client-Id, Idempotency-Key)
```
rather than by the idempotency key alone.

---
### Why This Design Was Chosen

In real payment systems, the API is used by **multiple independent clients (merchants or services)**.
If idempotency keys were stored globally, two different clients could unknowingly use the same key value, leading to incorrect behavior.

For example:

Client A:

```
Idempotency-Key: abc123
Amount: 100
```

Client B:

```
Idempotency-Key: abc123
Amount: 500
```

If the system keyed only on `Idempotency-Key`, the second request could:

* receive an incorrect cached response, or
* be rejected incorrectly due to a hash mismatch.

By including `Client-Id` in the lookup and storage logic, each client operates within its own isolated idempotency space.

---
### Benefits

This approach provides several practical advantages:

* **Isolation between clients**
  Each client’s requests are handled independently, preventing interference across tenants.

* **Correctness in multi-tenant systems**
  Identical keys from different clients are treated as separate transactions.

* **Improved security and data integrity**
  Responses cannot be accidentally shared across clients.

* **Closer alignment with real payment gateway design**
  Production payment systems typically scope idempotency keys to an account, API key, or merchant identifier.

---
### Implementation Detail

In this project, idempotency records are stored and queried using:

```
(client_id, idempotency_key)
```

This composite key ensures that:

1. Duplicate requests from the **same client** are handled safely.
2. Requests from **different clients** using the same key remain independent.
---
