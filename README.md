# Crypto Wallet Management

[![CI](https://github.com/ric-ram/cryptowallet/actions/workflows/ci.yml/badge.svg)](https://github.com/ric-ram/cryptowallet>/actions/workflows/ci.yml)


> **Elevator pitch:**  
> Crypto Wallet Management is a Spring Boot microservice that lets users  
> create and manage a wallet of cryptocurrency holdings, automatically fetch  
> live prices via the CoinCap API, compute real-time valuation, and simulate  
> historical profit/loss — either as of today or any past date .

---

## Table of Contents

- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Postman Collection](#postman-collection)
- [Future Improvements](#future-improvements)
- [License](#license)

---

## Key Features

1. **Get the Latest Prices**
    - Periodically fetch live token prices from CoinCap, at a configurable interval,  
      using up to 3 concurrent threads.

2. **Save to SQL Database**
    - Persist wallet, assets (symbol & slug), and current price cache in PostgreSQL.

3. **Create New Wallet**
    - `POST /wallet` with a **unique** email → creates an empty wallet or returns 409 if already exists.

4. **Add Asset to Wallet**
    - `POST /wallet/{id}/assets` with symbol & quantity → validates via CoinCap, then saves asset under the wallet.

5. **Show Wallet Information**
    - `GET /wallet/{id}` → returns each asset’s symbol, price, quantity, value, plus wallet total in USD.

6. **Wallet Profit Simulation**
    - `POST /wallet/simulate` with assets’ symbol, quantity & past value, plus optional date → computes total profit/loss, best & worst performers, for today or any past date.

---

## Tech Stack

- **Java 17**
- **Spring Boot 3.4.5**
- **Maven 3.9.5**
- **PostgreSQL**
- **Lombok** (entities, services)
- **Spring WebClient** (CoinCap integration)
- **JUnit 5, Mockito** (unit & slice tests)

---

## Prerequisites

- JDK 17 installed
- Maven 3.9.5 (or use the included `mvnw`)
- A running PostgreSQL instance
- A **CoinCap API key** (set via an environment variable)

---

## Running Locally
1. **Build**
   ```bash
   mvn clean package
   ```

2. **Run**
    ```bash
    mvn spring-boot:run
    # or
    java -jar target/crypto-wallet-0.1.0.jar
    ```
3. The service will start on http://localhost:8080

---

## Configuration
All settings live in src/main/resources/application.properties or via environment variables:

```properties
# Database (Postgres)
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DB>
spring.datasource.username=<USER>
spring.datasource.password=<PASSWORD>

# CoinCap API
coincap.api.base-url=https://api.coincap.io
coincap.api.key=${COINCAP_API_KEY}

# Price polling (ms)
app.prices.poll-rate-ms=60000
```

---

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/wallet` | Create a new wallet (unique email) |
| POST | `/wallet/{id}/assets` | Add an asset (symbol & quantity to a wallet |
| GET | `/wallet/{id}` | Get wallet valuation (assets + total) |
| POST | `/wallet/simulate` | Simulate profit/loss today or at a past date |

> **Swagger UI** is available at `/swagger-ui.html` or in the raw file `/docs/openapi.yaml`.


---

## Testing
- **Unit & Slice Tests**
```bash
mvn test 
```
- **Integration Tests** (PostgreSQL via Testcontainers + CoinCap stubs via WireMock)

These run under `@SpringBootTest` and execute when you do a full verify:
```bash
mvn verify
```

---

## Postman Collection
Import collection from:
 TODO: postman collection file
It includes ready-to-run examples for all endpoints

--- 

## License
This project is delivered as a technical coding challenge.
