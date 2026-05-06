# 📊 Performance Test Reporting Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)]()
[![JPA](https://img.shields.io/badge/JPA-Hibernate-blueviolet)]()
[![Swagger](https://img.shields.io/badge/Swagger-API-red)]()

---

## 📝 Project Overview

This is a **sanitised public demo** of a production‑grade load testing framework.  
It executes performance tests with precise **RPS (requests per second) control**, stores results asynchronously, and provides a **rich historical report API** (paginated, filterable).  
Built with **Spring WebFlux, Java HTTP Client, JPA, Hibernate, Flyway, and H2/PostgreSQL**.

> ⚠️ **Note:** All internal endpoints, credentials, and business logic have been removed. Public placeholders (httpbin.org, JSONPlaceholder) are used instead. The architecture and implementation patterns are fully preserved to demonstrate engineering skills.

---

## ⚡️ Features

- **Accurate RPS Limiting** – `Semaphore` + scheduled executor to sustain target load.
- **Asynchronous Reporting** – Test reports saved to database without blocking test execution (`@Async` + custom thread pool).
- **Rich Metrics** – Total requests, success/failure counts, mean, max, 95th percentile response times.
- **Historical Report API** – Paginated, filterable (by success/failure) access to past test runs and per‑request details.
- **Database Versioning** – Flyway manages schema (H2 in‑memory for demo, PostgreSQL ready).
- **OpenAPI Documentation** – Interactive Swagger UI.

---

## 🧱 Architecture (Simplified)
``` 
Controller (WebFlux Mono) → Service (Rate Limiting + HTTP Client) → Target API (httpbin.org)
│
▼
Async Report Persistence
│
▼
JPA Repository → H2/PostgreSQL
```

- **Reactive controllers** return `Mono` for non‑blocking request handling.
- **Custom load engine** – pure Java `HttpClient`, no external tool.
- **Decoupled persistence** – runs in separate `report-saver‑` thread pool.

---

## 🛠️ Tech Stack

| Area               | Technologies |
|--------------------|--------------|
| Framework          | Spring Boot 3.2.5 (WebFlux, Data JPA) |
| Build              | Maven |
| Database           | H2 (demo), PostgreSQL (production) |
| Migration          | Flyway |
| HTTP Client        | Java `java.net.http.HttpClient` |
| Asynchronous       | `@Async`, `CompletableFuture`, custom thread pool |
| Documentation      | SpringDoc OpenAPI (Swagger UI) |
| Language           | Java 17, Lombok |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Installation & Running

```bash
# Clone the repository
git clone https://github.com/shkBostan/performance-test-reporting-platform.git
cd performance-test-reporting-platform

# Build the project
mvn clean package

# Run the application
java -jar target/perf-test-reporting-*.jar
```
### Swagger UI
```
http://localhost:8090/swagger-ui.html
```

📁 Project Structure (simplified)
```
src/main/java/io/github/shkBostan/perftest/reporting/
├── controller/        # REST endpoints (WebFlux)
├── service/           # Load test engine, async persistence
├── repository/        # Spring Data JPA
├── entity/            # TestRun, RequestResult
├── model/             # DTOs (CustomMetricsReport, etc.)
├── config/            # Async thread pool
└── PerfTestReportingApplication.java
```


📄 License
This project is licensed under the Apache License 2.0 – see the LICENSE(LICENSE.txt) file for details.


## Author

**s Bostan**  
Created on: Apr, 2026
