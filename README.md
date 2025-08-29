# 🎬 MediaBuddy

MediaBuddy is a comprehensive, feature-rich platform for streaming and sharing media content, designed to provide an exceptional user experience. Users can watch movies and TV shows, upload their own videos, communicate via real-time WebSocket chats, and receive personalized recommendations.

The project is built on a modern hybrid architecture that combines a powerful core service with specialized microservices for asynchronous, resource-intensive operations.

---

## 🛠️ Technologies & Badges

[![Java](https://img.shields.io/badge/Java-17-blue.svg?style=for-the-badge&logo=java)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=for-the-badge)](LICENSE)

---

## 📋 Table of Contents

- [About the Project](#-about-the-project)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)
- [License](#-license)

---

## 📖 About the Project

MediaBuddy is an entire ecosystem for movie and TV enthusiasts.  
At its core lies **service-main**, which provides both a web interface and a REST API. It handles authentication, real-time interactions, and main application logic. Supporting microservices such as **service-compression** and **service-history** handle heavy asynchronous operations, ensuring responsiveness and scalability.

The system also includes a high-performance streaming microservice (**service-streaming**) built on **Spring WebFlux** to deliver optimized media playback.

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🎬 Media Library & Streaming | Watch a rich collection of movies and TV shows, upload user-generated videos, and stream them from Backblaze B2 cloud storage. |
| 🚀 Asynchronous Video Processing | Videos are processed in the background. `service-compression` applies the Strategy pattern to dynamically compress videos into multiple formats. |
| 🤖 Personalized Recommendations | `service-history` stores watch history and builds recommendation models tailored to each user. |
| 💬 Real-time Chats | Full-featured chat and instant notifications system using WebSockets for zero-latency interactions. |
| 🔍 Intelligent Search | Lightning-fast full-text search powered by Elasticsearch. |
| 🔐 Security | Robust authentication powered by Spring Security, JWT, and OAuth 2.0 support. |

---

## 🏗️ System Architecture

The project follows a hybrid microservice architecture. **service-main** acts as the central entry point, while other services operate independently and communicate via **RabbitMQ**, ensuring loose coupling and fault tolerance.

| Component | Responsibility |
|-----------|----------------|
| `service-main` | Central API and UI (Thymeleaf), user management, chats, search, authentication. |
| `service-compression` | Dedicated worker for background video compression. |
| `service-history` | Collects and analyzes user behavior for recommendations. |
| `service-streaming` | High-performance video streaming microservice built with Spring WebFlux. |

---

## 🛠️ Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 3, Spring Security, WebSockets, Spring Data (JPA, Mongo), Flyway |
| Frontend | Thymeleaf, HTML5, CSS3, Vanilla JavaScript |
| Databases | PostgreSQL, MongoDB, Redis |
| Message Broker | RabbitMQ |
| Search | Elasticsearch, Kibana |
| Infrastructure | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers |
| API Documentation | Springdoc (OpenAPI / Swagger) |

---

## 🚀 Quick Start

### 1️⃣ Launch Infrastructure
Run the following command from the project root to start required services (databases, RabbitMQ, etc.):

```bash
docker-compose -f docker/docker-compose.yml up -d
```
2️⃣ Start All Services
Use the provided script to launch all Java services:

```bash
./run-all.bat
```
The application will be available at: http://localhost:8080

### Access Points
🌐 MediaBuddy UI	http://localhost:8080

📖 API Docs (Swagger)	http://localhost:8080/swagger-ui.html

🗄️ pgAdmin	http://localhost:5050

🍃 Mongo Express	http://localhost:8081

🐇 RabbitMQ Management UI	http://localhost:15672

### Elasticsearch Setup
Before using the search functionality, you need to reindex films with an admin role:

GET http://localhost:8080/api/search/films/reindex

pgsql
Копировать код
> Make sure you are logged in as an admin user to perform this operation.

## 📂 Project Structure

. 
docker/                 # Docker Compose and infrastructure configs

service-main/           # Core service (API + UI)

service-compression/    # Microservice for video compression

service-history/        # Microservice for watch history & recommendations

service-streaming/      # Microservice for streaming (WebFlux)

pom.xml                 # Parent Maven configuration

run-all.bat             # Script to run all services on Windows

## 🌱 Future improvements
Integrate Kafka for more scalable event streaming.

Extract Authentication into a standalone Auth service.

Add CI/CD Pipeline (GitHub Actions) for automated build/test/deploy.

Expand Integration Testing across service interactions.

Enhance Swagger/OpenAPI Documentation for all public endpoints.

📄 License
This project is licensed under the MIT License.