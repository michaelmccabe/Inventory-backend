# Inventory Service Microservice Template

This project serves as a template for building well-structured, production-ready microservices in Java using the Spring Boot framework. 
It provides a solid foundation with a clear project structure, robust testing facilities, and best practices for modern Java development.

## Core Technologies

*   **Java 21**
*   **Spring Boot 3:** For building the application.
*   **Spring Data JPA:** For data persistence.
*   **PostgreSQL:** As the relational database.
*   **Maven:** For dependency management and build automation.
*   **Lombok:** To reduce boilerplate code.
*   **MapStruct:** For mapping between DTOs and entities.
*   **Java Virtual Threads (JVHs)::** For parallel processing.

## Project Structure

The project follows a standard Maven layout:

*   `src/main/java`: Main application source code.
*   `src/main/resources`: Application configuration, static assets, and database migrations.
*   `src/test/java`: Test source code.
*   `src/test/resources`: Test-specific configuration.
*   `pom.xml`: Maven project configuration.

## API Definition

The API is defined using the OpenAPI 3.0 specification.

*   The definition file is located at `src/main/resources/openapi.yaml`.
*   The `openapi-generator-maven-plugin` is used to generate the API interfaces and model classes (`com.mictech.api` and `com.mictech.api.model`) 
*   This ensures the implementation stays in sync with the API contract.

### Inventory Management

The inventory management API provides a set of endpoints for managing the items in the inventory.

*   **`GET /api/items`:** Retrieves a list of all items in the inventory.
*   **`POST /api/items`:** Creates a new item in the inventory.
*   **`GET /api/items/{id}`:** Retrieves a specific item by its ID.
*   **`PUT /api/items/{id}`:** Updates an existing item's information.
*   **`DELETE /api/items/{id}`:** Removes an item from the inventory.

### Order Management

The order management API has been designed to separate the creation and modification of orders from the final purchase action. 
This provides a clear and robust workflow for handling customer orders.

*   **`POST /api/orders` (Create Order):** Creates a new order with a `SAVED` status. The system checks for sufficient inventory before creating the order.
*   **`PUT /api/orders/{id}` (Update Order):** Updates an existing order. The order must have a `SAVED` status to be updated.
*   **`POST /api/orders/{id}/purchase` (Purchase Order):** Transitions an order to the `PURCHASED` status. This is the final step in the order process and will decrement the inventory.

### Order Statuses

Orders can be in one of the following statuses:

*   **`SAVED`:** The order has been created but not yet purchased. It can be modified.
*   **`PURCHASED`:** The order has been paid for, and the inventory has been updated.
*   **`HELD`:** If an order cannot be purchased due to insufficient stock, its status is automatically changed to `HELD`.

## Virtual Threads for Concurrent Shipping

This project includes an optional feature to process the shipping of an order using Java's new virtual threads, introduced in Java 21. Virtual threads are a lightweight implementation of threads provided by the JDK, designed to dramatically reduce the effort of writing, maintaining, 
and observing high-throughput concurrent applications. For more information, see the official [Java documentation on Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html).

The `testVirtualVsPlatformThreadPerformance` test in `VirtualThreadShippingTest.java` is designed to compare the performance of shipping a large order (1000 items) using both traditional platform threads and new virtual threads. It times both operations and logs the results.

In this specific use case, which is heavily CPU-bound (logging to the console), the performance difference between virtual and platform threads may not be substantial. In fact, you might observe that platform threads are sometimes faster. 

However, the real power of virtual threads shines in I/O-bound tasks, where the ability to easily parallelise a high number of operations can lead to significant performance gains.

## Database Migrations

Database schema changes are managed using **Flyway**.

*   SQL migration scripts are placed in `src/main/resources/db/migration`.
*   Flyway automatically applies these migrations to the database on application startup, ensuring the schema is always up-to-date.

## Testing Facilities

This template is configured with a comprehensive testing strategy.

*   **JUnit 5:** The standard testing framework for Java.
*   **Testcontainers:** This is a key feature of the template. For integration testing, Testcontainers is used to spin up a real PostgreSQL database inside a Docker container.
    *   This allows tests to run against a live, ephemeral database, providing a high degree of confidence that the application will behave correctly in a production-like environment.
    *   The `OrderControllerIntegrationTest` provides a comprehensive set of tests for the order management workflow, including success and failure scenarios.
*   **Test-specific Properties:** A separate configuration file at `src/test/resources/application-test.yml` is used to configure the application context for the testing environment, such as setting the datasource URL to point to the Testcontainer.



`docker run --name local-postgres \
-e POSTGRES_PASSWORD=your_secure_password_here \
-e POSTGRES_DB=inventory  # Optional: Creates a default DB (matches your template)
-d \
-p 5432:5432 \
-v postgres_data:/var/lib/postgresql/data \
postgres:latest`