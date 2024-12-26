# README

---
## Overview

The **Order Fulfillment Simulation Service** is a Java-based solution designed to simulate an order placement and fulfillment system for a food delivery scenario. This system handles orders by placing them on designated shelves based on temperature requirements (HOT, COLD, or ROOM) and ensures efficient processing, from placement to pickup, within specified time constraints.

This project demonstrates the use of multithreading, distributed systems design, and real-time order processing in a structured and scalable manner.


## Key Components

### 1. **Main Class**
The `Main` class initializes the problem, sets up the simulation, and processes the orders using the provided `OrderService` and `OrderFulfilmentService`.

Key functionalities:
- Fetches the problem from the provided API endpoint.
- Configures and starts the order processing logic.
- Submits the results back to the server for validation.

### 2. **OrderService**
Handles the scheduling of order placement and pickups:
- Uses a `ScheduledExecutorService` to place orders at a regular interval (`rate`).
- Handles delayed pickups using another `ScheduledExecutorService` with a random delay between `min` and `max`.
- Ensures proper shutdown and cleanup of resources using a `CountDownLatch`.

### 3. **OrderFulfilmentService**
Manages the storage and movement of orders on temperature-based shelves:
- Supports three types of shelves: **Hot**, **Cold**, and **Room Temperature**.
- Implements logic to:
    - Place orders on their ideal shelf.
    - Move orders to/from the room shelf when necessary.
    - Evict stale orders if no space is available.
- Logs every action (e.g., placement, movement, pickup) for later validation.

### 4. **Shelf**
Represents a shelf with limited capacity and handles storage/retrieval of orders.

### application.properties
- Thread pool size for pickUp thread in OrderService can be customized based on load (default: 10)
---
### Logic for Order Movement and Order Discard
- If ideal shelf for the order is full then following logic is used to adjust this new order
  - Check if Room Shelf has space.
    - true - place the order in Room shelf and return
    - false - Refer to point 2
  - Check for count of hot+cold orders in Room Shelf
    - true - Count is non-zero refer to point 3
    - false - Return
  - Get the least fresh cold / hot order compared to current system time in the Room Shelf
    - Move the eligible order from Room shelf to respective cold/hot shelf and return. Set the action log to MOVE for that order
    - Get for the least fresh order with respect to current system time
    - Discard the order and set the action log for that order to DISCARD

---

---
## How to run

```
$ ./gradlew run --args="--auth=<token>" --rate=<rate>

e.g. gradle run --args='--endpoint=https://api.cloudkitchens.com --auth=9znboe4344k7

Command-Line Options
--endpoint: Problem server endpoint (default: https://api.cloudkitchens.com).
--auth: Authentication token (required).
--name: Problem name (optional, default is blank).
--seed: Random seed for generating orders  (NOT SUPPORTED)
--rate: Time interval between order placements (default: 150ms).
--min: Minimum pickup delay (default: 2 seconds).
--max: Maximum pickup delay (default: 4 seconds).
--hotcapacity: Hot Shelf capacity (optional default: 6)
--coldcapacity: Cold Shelf capacity (optional default: 6)
--roomcapacity: Room Shelf capacity (optional default: 12)
```

## Testing
#### Faster Kitchen vs Slower PickUp ()
- Inverse Order Rate - 300ms
- Min - 3s
- Max - 8s

 ./gradlew run --args='--endpoint=https://api.cloudkitchens.com --auth=9znboe4344k7 --rate=PT0.3S --min=PT3S --max=PT8S'

Result: Orders get moved to room shelf due to lack of space on hot and cold.

#### Slower Kitchen vs Faster Pickup (Slow Kitchen)
- Inverse Order Rate - 600ms
- Min - 2s
- Max - 5s
  ./gradlew run --args='--endpoint=https://api.cloudkitchens.com --auth=9znboe4344k7 --rate=PT0.6S --min=PT2S --max=PT5S'

Result: Pick up threads remain due to slow placing of orders on shelf.

## Logging
Logging
The solution uses SLF4J for logging:
```
INFO: General processing updates and results.
DEBUG: Detailed logs for order placement and pickup delays.
WARN: Alerts for evictions or invalid operations.
You can adjust the logging level by modifying the configuration in Main.java.
```


## Future Improvements
```
Add unit and integration tests for all services.
Implement metrics collection for performance monitoring.
Enhance error handling and retries for API calls.
```