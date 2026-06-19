# AuraDB - Complete Testing Guide (Phases 1-7)

This guide explains what each phase in the AuraDB project does, and exactly how you can test it to see it working in action.

## 🟢 Phase 1: The Control Plane (The Brain)
**What it does:** The Control Plane is the central management system. It's where you register tenants (customers like Netflix) and create database instances for them.

**How to test it:**
1. Start the `auradb-control-plane` Spring Boot application.
2. Open Postman or use `curl` to send a POST request to `http://localhost:8080/api/v1/tenants`.
3. Provide this JSON body: `{"id": "netflix", "name": "Netflix Inc.", "plan": "enterprise"}`.
4. Send a GET request to `http://localhost:8080/api/v1/tenants/netflix`. You should see the tenant details returned with status 200 OK.

---

## 🟢 Phase 2: Redis Routing (The Map)
**What it does:** When the Control Plane creates a database instance, it needs a way to tell the rest of the system where this database lives. It saves this "routing map" in Redis.

**How to test it:**
1. Make sure Redis is running (e.g., via Docker: `docker run -d -p 6379:6379 redis`).
2. Start the `auradb-control-plane`.
3. Send a POST request to create a database instance for Netflix.
4. Open the Redis CLI (`redis-cli`) and type `KEYS *`.
5. You should see a key like `route:tenant:netflix`.
6. Type `GET route:tenant:netflix` and you will see the URI (IP address and port) of Netflix's database.

---

## 🟢 Phase 3: The API Gateway (The Traffic Cop)
**What it does:** Applications don't talk directly to databases; they talk to the Gateway. The Gateway reads the `X-Tenant-ID` header from the request, looks up the destination in Redis (from Phase 2), and forwards the request to the correct database.

**How to test it:**
1. Start Redis, the Control Plane, and the `auradb-gateway`.
2. Start a dummy "Database" service running on port `9001` (representing Netflix's DB).
3. Ensure Redis has the route: `route:tenant:netflix` pointing to `http://localhost:9001`.
4. Send a request to the Gateway: `curl -H "X-Tenant-ID: netflix" http://localhost:8000/some-query`.
5. The Gateway should seamlessly route your request to the database on port `9001`.

---

## 🟢 Phase 4: Kafka Telemetry (The Heartbeat Monitor)
**What it does:** How does the Control Plane know if a database crashes? Every database instance constantly sends a "heartbeat" (telemetry data) to a Kafka topic. The Control Plane listens to this topic to monitor health.

**How to test it:**
1. Start Zookeeper and Kafka (usually via `docker-compose`).
2. Start the Control Plane (it connects to Kafka as a consumer).
3. Send a test message to the Kafka topic `auradb-telemetry` formatted as JSON: `{"instanceId": "db-1", "tenantId": "netflix", "status": "HEALTHY", "cpuUsage": 45}`.
4. Look at the Control Plane logs. You should see it reading the message and updating the health status of that instance in its internal memory.

---

## 🟢 Phase 5: Automatic Failover (The Rescue Team)
**What it does:** If a primary database stops sending heartbeats to Kafka, the Control Plane assumes it crashed. It automatically promotes a "Replica" database to become the new "Primary", and updates Redis so the Gateway routes traffic to the new database without the customer noticing.

**How to test it:**
1. Have a Tenant (Netflix) with two instances registered in the Control Plane: `db-primary` (Primary) and `db-replica` (Replica).
2. Start the Control Plane, Redis, Kafka, and the Gateway.
3. Stop sending Kafka heartbeats for `db-primary`.
4. Wait a few seconds for the Control Plane's health checker to notice the missing heartbeats.
5. Check the Control Plane logs: You should see `"Instance db-primary is DOWN. Initiating failover..."` followed by `"Promoted db-replica to PRIMARY"`.
6. Open Redis CLI and type `GET route:tenant:netflix`. You will see the route has automatically updated to point to the replica's IP address.

---

## 🔴 Phase 6: JWT Security (Not Started Yet)
**What it does:** Secures the Gateway and Control Plane using JSON Web Tokens (JWT). Only authenticated requests carrying a valid token will be allowed to pass through the Gateway.

**How to test it (Once Built):**
1. Send a request to the Gateway without a token -> You should get a `401 Unauthorized`.
2. Generate a valid JWT token.
3. Send a request with `Authorization: Bearer <token>` -> The Gateway should validate it and let the request through.

---

## 🔴 Phase 7: Resilience & Actuator (Not Started Yet)
**What it does:** Adds Spring Boot Actuator for `/health` and `/metrics` endpoints, and implements Resilience4j Circuit Breakers in the Gateway so it doesn't crash if a database becomes too slow to respond.

**How to test it (Once Built):**
1. Visit `http://localhost:8000/actuator/health` to see the system health.
2. Make the destination database extremely slow (e.g., add a 10-second delay).
3. Send traffic through the Gateway. The Circuit Breaker should "open" and return a fast error (like `503 Service Unavailable`) instead of hanging and crashing the Gateway.
