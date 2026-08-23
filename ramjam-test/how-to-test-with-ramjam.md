# How to Test Inventory Service with Ramjam

This guide explains how to use [ramjam](https://github.com/michaelmccabe/ramjam) to execute end-to-end integration and workflow tests against the Inventory Service.

## What is Ramjam?

Ramjam is a declarative CLI testing tool written in Go that executes HTTP API workflows defined in YAML files. Key features include:

- **Declarative YAML DSL**: Clean and human-readable test scenarios.
- **Variable Capture & Substitution**: Extract IDs, tokens, and fields from responses (`capture`) and inject them into subsequent steps using `${variable_name}`.
- **JSONPath Matchers**: Validate response payload structures and specific fields (`expect.json_path_match`).
- **External Payload Files**: Reference reusable JSON payloads with `body_file`.
- **Lightweight CI Integration**: Pre-compiled single binary without external runtime requirements.

---

## Prerequisites

1. **PostgreSQL Database** running on port 5432 (or start via local Docker Compose):
   ```bash
   docker compose -f local-dev/docker-compose.yml up -d postgres
   ```
2. **Java 21** & **Maven Wrapper** (`./mvnw`).
3. **Ramjam CLI**:
   - Install using Go:
     ```bash
     go install github.com/michaelmccabe/ramjam/cmd/ramjam@latest
     ```
   - Or download pre-built binary from [Ramjam Releases](https://github.com/michaelmccabe/ramjam/releases):
     ```bash
     curl -L -o ramjam https://github.com/michaelmccabe/ramjam/releases/download/v1.0.0-beta.1/ramjam-linux-amd64 # (or darwin-arm64 / darwin-amd64)
     chmod +x ramjam
     sudo mv ramjam /usr/local/bin/
     ```

---

## Starting the Application

Build and start the Inventory Service:

```bash
# 1. Package the service
./mvnw clean package -DskipTests

# 2. Run the application
java -jar target/Inventory-1.0-SNAPSHOT.jar
```

Verify that the service is running:
```bash
curl http://127.0.0.1:8080/actuator/health
# Output: {"status":"UP"}
```

---

## Running Ramjam Tests

### Run All Tests in the Test Suite

```bash
ramjam run ramjam-test/
```

### Run with Verbose Output

```bash
ramjam run ramjam-test/ --verbose
```

### Run Specific Test Files

```bash
# Run Health Check
ramjam run ramjam-test/health-check.yaml

# Run Items CRUD
ramjam run ramjam-test/items-crud.yaml

# Run Order Lifecycle
ramjam run ramjam-test/orders-lifecycle.yaml

# Run Full End-to-End Workflow
ramjam run ramjam-test/full-workflow.yaml
```

---

## Test Files Overview

| File | Description |
|---|---|
| `health-check.yaml` | Verifies Spring Boot Actuator `/actuator/health` endpoint. |
| `items-crud.yaml` | Complete CRUD test suite for `/api/items` (Create, Read, Update, Delete, Verify 404). |
| `duplicate-item.yaml` | Verifies unique item name constraint and `409 Conflict` response. |
| `orders-lifecycle.yaml` | Creates item, submits order (`SAVED`), updates address, purchases order (`PURCHASED`), and validates inventory deduction. |
| `order-stock-validation.yaml` | Tests insufficient stock rejection (`400 Bad Request`) and order hold behavior (`HELD`). |
| `file-reference.yaml` | Tests payload loading from external file (`payloads/create-item.json`). |
| `full-workflow.yaml` | Comprehensive multi-item ordering, virtual threads purchase (`?useVirtualThreads=true`), correlation ID header passing, and final stock assertions. |

---

## GitHub Actions CI/CD Integration

Ramjam integration tests are run automatically in GitHub Actions on every push and pull request against `main` via `.github/workflows/integration-tests.yml`.

### Workflow Structure

1. Spins up a PostgreSQL 16 service container.
2. Builds the project (`./mvnw clean package -DskipTests`).
3. Installs the `ramjam` CLI binary.
4. Starts the Spring Boot application in the background.
5. Polls `/actuator/health` until ready.
6. Executes `ramjam run ramjam-test/ --verbose`.
7. Tears down the background process.

---

## Troubleshooting

### Connection Refused
Ensure the application is running and listening on port 8080:
```bash
curl http://127.0.0.1:8080/actuator/health
```

### Unique Name Constraint Collisions
If re-running tests against a persistent local database, clear existing tables:
```bash
docker exec -i local-postgres psql -U user -d inventory -c "TRUNCATE order_item, orders, items RESTART IDENTITY CASCADE;"
```

### Debugging Failures
Use the `--verbose` flag to view exact request payloads, response codes, headers, and JSON evaluation steps:
```bash
ramjam run ramjam-test/<test-file>.yaml --verbose
```
