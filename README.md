# 🧩 Dynamic SAGA Orchestration Microservices (Order-Inventory-Payment Demo)

This project demonstrates a **dynamic SAGA orchestration platform** built with **Spring Boot 3.5.6**, **Kafka**, and **Redis**.  
It allows SAGA workflows to be **defined and executed dynamically** across independent microservices.

To showcase the orchestration system in action, an example domain — **Order → Inventory → Payment** — is implemented.

## 🚀 Overview

This system is composed of multiple Spring Boot microservices that communicate asynchronously through Kafka topics.  
The **Orchestrator Service** manages workflow states, triggers compensations when necessary, and keeps the overall process consistent.

```mermaid
flowchart LR
    A[Web UI] <--> B[Order Service]
    B <--> C[Orchestrator]
    C <--> D[Inventory]
    C <--> E[Payment]
```
* All communication between services occurs through Kafka topics.
* Orchestrator service drives the process using a dynamic SAGA definition (workflow steps and compensations).
* Redis is used both as:
    * A fast stock store (for reservation logic).
    * A timer mechanism (for SAGA timeouts and expirations).

## 🧩 Defining a SAGA Workflow Dynamically
SAGA workflows can be created and persisted programmatically.
Each workflow is composed of:
- A **SagaEntity**: defines the workflow name, version, trigger event, and global timeout.
- Multiple **SagaStepEntity** objects: each representing a single action in the process (command + expected event + compensation).

Below is an example of how to define a three-step workflow for the *Order–Inventory–Payment* process:
<details>
<summary>📘 Click to view example code</summary>

```java
class SagaService {
    public void createOrderStockSaga() {
        SagaEntity entity = new SagaEntity();
        entity.setName("OrderStockSaga");
        entity.setDefinedVersion(1);
        entity.setTriggerEvent(EventType.ORDER_CREATED);
        entity.setExpiration(SagaConstants.TOTAL_WORKFLOW_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Step 1: Reserve Stock
        SagaStepEntity reserveStock = new SagaStepEntity();
        reserveStock.setStepOrder(1);
        reserveStock.setName("Reserve Stock");
        reserveStock.setCommandType(CommandType.RESERVE_STOCK);
        reserveStock.setExpectedEventType(EventType.STOCK_RESERVED);
        reserveStock.setOnFailureCommand(CommandType.RELEASE_STOCK);
        reserveStock.setCommandTopic(KafkaNames.INVENTORY_COMMANDS);
        reserveStock.setSaga(entity);

        // Step 2: Process Payment
        SagaStepEntity processPayment = new SagaStepEntity();
        processPayment.setStepOrder(2);
        processPayment.setName("Process Payment");
        processPayment.setCommandType(CommandType.PROCESS_PAYMENT);
        processPayment.setExpectedEventType(EventType.PAYMENT_SUCCEEDED);
        processPayment.setOnFailureCommand(CommandType.REFUND_PAYMENT);
        processPayment.setCommandTopic(KafkaNames.PAYMENT_COMMANDS);
        processPayment.setSaga(entity);

        // Step 3: Complete Order
        SagaStepEntity completeOrder = new SagaStepEntity();
        completeOrder.setStepOrder(3);
        completeOrder.setName("Complete Order");
        completeOrder.setCommandType(CommandType.COMPLETE_ORDER);
        completeOrder.setExpectedEventType(EventType.ORDER_COMPLETED);
        completeOrder.setOnFailureCommand(CommandType.CANCEL_ORDER);
        completeOrder.setCommandTopic(KafkaNames.ORDER_COMMANDS);
        completeOrder.setSaga(entity);

        entity.setSteps(Set.of(reserveStock, processPayment, completeOrder));

        // Save to the database
        dao.save(entity);
    }
}

```
</details>

This code dynamically builds and persists a complete SAGA workflow:  
1.The trigger event (ORDER_CREATED) starts the orchestration.  
2.Each step defines both success and compensation commands.  
3.The workflow is stored in the orchestrator database and used to drive the actual event flow.  

### 🕒 Timeout Handling
The orchestration system uses Redis to manage SAGA workflow timeouts.
If a saga step exceeds its configured time limit, the orchestrator automatically triggers compensation flow and marks the saga as failed.

## 🧱 Microservices

| Service                  | Description                                                                                                                                    |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **order-service**        | Exposes REST API (`/api/orders`) and WebSocket endpoint. Publishes `ORDER_CREATED` events.                                                     |
| **inventory-service**    | Handles stock reservation using Redis for fast access. Publishes `STOCK_RESERVED` or `STOCK_RELEASED` events.                                  |
| **payment-service**      | Simulates payment processing; publishes `PAYMENT_SUCCEED` or `PAYMENT_REFUNDED` events.                                                        |
| **orchestrator-service** | Central coordinator that dynamically executes SAGA definitions, publishes commands, listens for results, and triggers compensations if needed. |
| **redis (infra)**        | Used both for inventory caching and orchestration workflow timeouts.                                                                           |
| **kafka (infra)**        | Message broker enabling reliable asynchronous communication.                                                                                   |

## ⚙️ Technologies
* Java 17
* Spring Boot 3.5.6
* Spring Kafka
* Spring Data Redis
* Docker & Docker Compose
* WebSocket (Spring Messaging)
* Maven build tool

## 🧪 Saga Flow Example
### ✅ Successful Flow
1.Client sends `POST /api/orders` or submits from UI.  
2.`order-service` publishes `ORDER_CREATED`.  
3.`orchestrator-service` starts the workflow and issues `RESERVE_STOCK`.  
4.`inventory-service` checks stock → publishes `STOCK_RESERVED`.  
5.`orchestrator-service` publishes `PROCESS_PAYMENT`.  
6.`payment-service` simulates success → publishes `PAYMENT_SUCCEED`.  
7.`orchestrator-service` finalizes the SAGA → publishes `ORDER_COMPLETED`.  
8.WebSocket in UI receives the final status.

### ❌ Failure & Compensation Flow
**If any step fails (e.g., payment fails):**

1.`orchestrator-service` triggers compensation logic.  
2.It dynamically issues compensation commands (e.g., release reserved stock).  
3.SAGA status transitions through `COMPENSATING → COMPENSATED → FAILED`.  
4.All relevant status updates are pushed to the UI via WebSocket.

## 🧰 Run the Project
1. **Clone the repository**
   ```bash
   git clone https://github.com/ali449/saga-orchestrator.git
   cd stock
   ```
2. **Start infrastructure**
   ```bash
   docker compose build
   docker compose up -d
   ```
   This brings up:
   * Kafka broker
   * Redis instance
   * All microservices (order, inventory, payment, orchestrator)

3. **Increase stock quantity**
    * Create stock with APIs in [`test_api.http`](./inventory/src/test/test_api.http)
      ```text
      POST localhost:8082/api/stocks
      {
        "name": "apple-123",
        "quantity": 20
      }
      ```
4. **Access the UI**
   * Open http://localhost:8081
   * Submit a new order and watch its status live via WebSocket.

## 🧪 Testing the Orchestrator
This project includes **some tests for the SAGA orchestration logic**, demonstrating how the orchestrator handles:

- Normal workflow execution (all steps succeed → SAGA completes).
- Compensation flow (any step fails → orchestrator triggers compensating commands).

The tests focus on **behavioral correctness** rather than full coverage, showing that the system reacts as expected to events and commands.
See full implementation: [`SagaTests.java`](./orchestration/src/test/java/com/example/orchestrator/SagaTests.java)

## 📜 Example Kafka Topics

| Category    | Example Topics                                             |
|-------------|------------------------------------------------------------|
| Events      | `order-events`, `inventory-events`, `payment-events`       |
| Commands    | `order-commands`, `inventory-commands`, `payment-commands` |
| SAGA events | `orchestrator-events`                                      |
> Note: Instead of creating all these topics, we could just have one for events and another for commands. 
> Right now, separate topics have been set up here to keep the processes organized.

## 🧩 Workflow Visualization
Below diagrams illustrate the internal state machines and message flow used in the dynamic SAGA orchestration system.
### SAGA Lifecycle
```mermaid
stateDiagram-v2
    [*] --> RUNNING : Saga started
    RUNNING --> COMPLETED : All steps succeeded
    RUNNING --> COMPENSATING : A step failed
    COMPENSATING --> FAILED : Compensation failed
    COMPENSATING --> COMPLETED : Compensation succeeded (full rollback)
    COMPLETED --> [*]
    FAILED --> [*]

    note right of RUNNING
        Saga is active and steps are executing
    end note
    note right of COMPENSATING
        Orchestrator issues compensation commands to undo prior steps
    end note
```

### SAGA Step Lifecycle
```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS : Step command received
    IN_PROGRESS --> COMPLETED : Step event succeeded
    IN_PROGRESS --> FAILED : Step execution failed
    FAILED --> COMPENSATED : Compensation executed successfully
    COMPENSATED --> [*]
    COMPLETED --> [*]
```
* It maps perfectly to your StepState enum.

### Order → Inventory → Payment Flow
**business flow diagram**
```mermaid
flowchart TD
    A[Order Created] --> B[Reserve Stock]
    B -->|Stock Reserved| C[Process Payment]
    C -->|Payment Succeeded| D[Complete Order]
    C -->|Payment Failed| E[Trigger Compensation]
    E --> F[Release Reserved Stock]
    F --> G[Saga Marked as Failed]
```

### 🧮 4. Full System Interaction
**microservices’ event choreography**
```mermaid
sequenceDiagram
    participant UI
    participant OrderService
    participant Orchestrator
    participant Inventory
    participant Payment

    UI->>OrderService: POST /api/orders
    OrderService->>Kafka: ORDER_CREATED event
    Orchestrator->>Inventory: RESERVE_STOCK command
    Inventory-->>Kafka: STOCK_RESERVED event
    Orchestrator->>Payment: PROCESS_PAYMENT command
    Payment-->>Kafka: PAYMENT_SUCCEED event
    Orchestrator->>OrderService: ORDER_COMPLETED event
    OrderService-->>UI: WebSocket update (ORDER_COMPLETED)
```

## 🚧 Future Improvements
* Add real persistence for Order and Payment services.
* Provide a REST API for defining SAGA workflows dynamically.
* Integrate distributed tracing (OpenTelemetry).
* Add monitoring dashboard for saga execution.

