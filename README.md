# 🌉 ponteWire: High-Performance Reactive Webhook Bridge

![Java 25](https://img.shields.io/badge/Java-25-orange?logo=java&logoColor=white)
![Spring Boot 4.0.3](https://img.shields.io/badge/Spring_Boot-4.0.3-green?logo=spring&logoColor=white)
![Kafka 4.2.0](https://img.shields.io/badge/Kafka-4.2.0-black?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

**ponteWire** is a resilient, event-driven bridge designed to handle high-velocity webhook traffic from global providers like Stripe or Shopify. Built for **scalability**, **fault tolerance**, and **data integrity** using the cutting-edge 2026 tech stack.

---

## 🏗 Architecture

The system follows a decoupled, reactive microservices pattern to ensure **Zero Data Loss** even during massive traffic spikes.

![Architecture Diagram](./images/image_da4f34.png)

### Core Components:
* **Spring WebFlux Gateway**: A non-blocking entry point that validates and normalizes incoming events.
* **Apache Kafka (KRaft)**: Acts as a high-throughput message broker and reliable persistence buffer.
* **Worker Service**: An asynchronous consumer that processes events and persists them to a reactive database.
* **Resilience Layer**: Global error handling featuring **Dead Letter Queues (DLQ)** and smart retry policies.

---

## 🚀 Key Features

| Feature | Description |
| :--- | :--- |
| **Zero-Loss Policy** | Advanced error handling with DLQ and smart retry mechanisms using `DeadLetterPublishingRecoverer`. |
| **Reactive Pipeline** | Fully non-blocking I/O from ingestion (WebFlux) to persistence (R2DBC). |
| **Architectural Contracts** | Shared DTOs and validation rules (Java 25 Records) to ensure consistency. |
| **Optimized Persistence** | Native **PostgreSQL JSONB** storage with **GIN indexing** for sub-millisecond querying. |

---

## 🛠 Tech Stack

* **Runtime:** Java 25 (Immutable Records & Pattern Matching)
* **Framework:** Spring Boot 4.0.3 (Reactive Stack)
* **Messaging:** Apache Kafka 4.2.0 (KRaft mode, No Zookeeper)
* **Persistence:** PostgreSQL 17 + R2DBC (Reactive Driver)
* **Build Tool:** Maven 3.9+

---

## 📦 Project Structure

* `pw-common`: Shared architectural contracts and utility DTOs.
* `pw-gateway`: High-throughput ingestion service (Kafka Producer).
* `pw-worker`: Event processing and auditing service (Kafka Consumer).

---

## 🚦 Getting Started

### Prerequisites
* **Docker & Docker Compose**
* **JDK 25**

### Quick Start
```bash
# Clone the repository
git clone [https://github.com/zeld1n/ponte-wire.git](https://github.com/zeld1n/ponte-wire.git)

# Start the infrastructure (Kafka, Postgres)
docker compose up -d

# Build and run the services
mvn clean install
