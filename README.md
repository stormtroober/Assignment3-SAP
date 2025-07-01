# EBike Event-Driven Microservices Application

## Assignment Overview

This project implements the "EBike application" using an event-driven microservices architecture with Event Sourcing patterns, deployed on Kubernetes. The system extends the original EBike case study with autonomous e-bikes ("a-bikes") for smart city environments.

### Key Requirements
- Event-driven microservices architecture using Apache Kafka
- Event Sourcing implementation for critical data management
- Kubernetes deployment with distributed infrastructure features
- Autonomous a-bike features:
  - Can autonomously reach the nearest station after use
  - Can autonomously reach users on demand
- Digital Twin integration for smart city monitoring

## Solution Architecture

### Microservices
- **API Gateway**: Entry point for client requests
- **User Microservice**: User management with event sourcing (MongoDB)
- **EBike Microservice**: Traditional e-bike management
- **ABike Microservice**: Autonomous a-bike and station management
- **Ride Microservice**: Ride orchestration and business logic
- **Map Microservice**: Real-time visualization

### Technology Stack
- **Event Streaming**: Apache Kafka with Avro schemas
- **Event Store**: MongoDB for event sourcing
- **Service Discovery**: Eureka
- **Digital Twin**: Eclipse Ditto platform
- **Deployment**: Docker Compose (development) / Kubernetes (production)

### Event Flow
```
ebike-updates, abike-updates    → State and position updates
ebike-ride-update, abike-ride-update → Ride lifecycle events
station-updates                 → Station state changes
ride-map-update                 → Map visualization updates
ride-user-update               → User credit and ride events
ride-bike-dispatch             → Autonomous bike dispatch
user-update                    → User data synchronization
```

## Deployment

### Docker Compose (Development)
```bash
docker-compose up -d
```

### Kubernetes (Production)
```bash
cd deployment/kubernetes
./deploy.sh
kubectl get pods -n ebikes
```

### Access Points
- API Gateway: http://localhost:8088
- Eureka Dashboard: http://localhost:8761
- Map Interface: http://localhost:8082
- Kafka Console: http://localhost:8087
- Digital Twin: http://localhost:8080

## Key Implementation Features

### Event Sourcing
- User microservice implements complete event sourcing with MongoDB
- Event types: UserCreated, CreditUpdated, CreditRecharged
- Aggregate reconstruction from event history

### Autonomous A-Bike
- Request a-bike to user location
- Automatic return to nearest station
- Real-time Digital Twin monitoring via Eclipse Ditto

### Kubernetes Transformation
- ConfigMaps for environment configuration
- Init containers for dependency management
- PersistentVolumeClaims for data storage
- LoadBalancer services for external access

---

**Assignment 3 - Software Architecture and Platforms**  
**University of Bologna - Campus di Cesena**  
**Student**: Alessandro Becci
