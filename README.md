# Idempotency Gateway – Pay Once Protocol

## Overview

This project implements an **Idempotency Layer** for payment processing. It ensures that a payment request is processed **exactly once**, even if the client retries due to network failures, timeouts, or duplicate submissions.

The system also addresses **race conditions** that can occur when multiple identical requests arrive at the same time. Without proper coordination, concurrent requests could be processed in parallel, which may result in duplicate transactions.

To prevent this, the system uses an idempotency key, request hashing, and atomic operations in Redis. These mechanisms ensure that only one request is allowed to execute the payment operation.Subsequent requests either wait for the operation to complete or receive the previously computed response.

---

## Architecture

The system is fully containerized using Docker and orchestrated with Docker Compose for easy setup and reproducible environments.

The services include:

* **Spring Boot Application** – Implements the API, idempotency logic, and payment processing simulation.
* **PostgreSQL** – Persists idempotency records, request hashes, and stored responses.
* **Redis** – Provides fast lookups and assists with concurrency control during in-flight requests.
* **Docker Compose** – Orchestrates all services, allowing the entire system to be started with a single command.

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
docker compose up -d --build 
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

PostgreSQL was chosen as the primary database to **store computed transactions permanently**. Unlike Redis, which is used for temporary caching, PostgreSQL provides durable and reliable long-term storage.

It serves several important purposes:
- **Permanent persistence** of transaction records, ensuring data is not lost after application restarts or cache expiration.
- **Data integrity and reliability**, supported by PostgreSQL’s ACID-compliant transactional guarantees.
- **Auditing and reporting**, allowing historical transaction data to be queried and analyzed when needed.

By separating responsibilities, Redis handles short-lived idempotency and performance optimization, while PostgreSQL ensures that all finalized transactions are safely stored for long-term use.

---

### Why Redis?

Redis is used as a complementary in-memory store to improve performance and handle concurrency in the idempotency layer.

Each idempotency request is cached in Redis under a unique key and stores important values such as:
- **Request hash** – used to verify that repeated requests have the same payload.
- **Processing status** (e.g., IN_PROGRESS or COMPLETED) – indicates the current state of the request.
- **HTTP status code** – the original response status returned to the client.
- **Response body** – the previously computed response used for replay.

This enables:
- Fast detection of duplicate requests.
- Replay of previously computed responses without reprocessing.
- Reduced database queries and lower system load.

Redis was chosen because of:
- Its **fast, in-memory architecture**, providing very low latency reads and writes.
- Support for **atomic operations**, which help prevent multiple threads from processing the same request simultaneously.

A **TTL (Time-To-Live)** is applied to each record to define how long the idempotency entry should remain cached. Once the TTL expires, Redis automatically removes the record, preventing stale data and unnecessary memory usage.

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
