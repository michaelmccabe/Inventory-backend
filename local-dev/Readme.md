# Local Development Environment

This directory contains the necessary configuration to run the application and its dependencies in a local Docker environment.

## Running the Environment

To start the local development environment, navigate to this directory in your terminal and run the following command:

```
 docker-compose up -d
```

This will start the application along with the services defined in the `docker-compose.yml` file.

To stop the environment, run the following command from this directory:

```
 docker-compose down -v
```

To view the Jaeger Service UI you can open a browser at http://localhost:16686/search

## Running the Inventory Service locally
Once the docker environment is started, you can open a terminal at the project root and run:

```
 mvn clean install
```
to build.

You then can run the service locally by typing:
```
 mvn spring-boot:run
```

Note the default logging in `application.yml` is quite verbose 

To view the trace from Inventory Service, you should open the Jaeger Service UI at http://localhost:16686/search and select `inventory-service` from the `Services` dropdown.

![jaeger.png](jaeger.png)

To add a trace for create-item, open a terminal window and run:

```
curl -v -X POST http://localhost:8080/api/items \
-H "Content-Type: application/json" \
-H "X-Correlation-ID: test-123" \
-d '{
"name": "Debug Widget 2",
"sku": "DEBUG-001",
"price": 9.99,
"stock_quantity": 50
}'
```

You can then access the details for this service call in the UI.

![jaeger-trace.png](jaeger-trace.png)

