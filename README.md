# Sentinel - Distributed Real-Time Fraud Detection Engine

> **Current Status:** 🚧 Under Active Development

A distributed, real-time fraud detection engine built on Event-Driven Architecture using Kafka, Quarkus, and Spring Boot.

## 🏗 Architecture

The system uses a microservices approach to process high-velocity transaction streams.

```mermaid
graph TD
    subgraph Data [Data Layer]
        Kafka[Apache Kafka]
        DB[(PostgreSQL)]
        Redis[(Redis)]
    end

    subgraph App [Application Layer]
        Gen[Generator Service] --> Kafka
        Kafka --> Analyzer[Analyzer Service]
        Analyzer --> Kafka
        Kafka --> Alerter[Alerter Service]
        
        Analyzer -->|Persist| DB
        Analyzer -->|Cache| Redis
        Alerter -->|WebSockets| Dash[Dashboard UI]
    end
```