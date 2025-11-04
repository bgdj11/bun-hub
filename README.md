# BunHub - Distributed Social Platform for Rabbit Enthusiasts

<div align="center">

**A full-stack, scalable social media platform for rabbit lovers with advanced distributed systems features**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17+-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-orange.svg)](https://www.rabbitmq.com/)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Advanced Features](#advanced-features)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Monitoring & Analytics](#monitoring--analytics)
- [Testing](#testing)
- [Team & Contributions](#team--contributions)

---

## Overview

**BunHub** is an enterprise-grade social media platform designed for rabbit enthusiasts to share photos, connect with other owners, and access care resources. The project demonstrates advanced concepts in distributed systems, including:

- **Real-time Communication** via WebSockets
- **Message Queue Integration** (RabbitMQ + Custom Implementation)
- **Distributed Caching** (EhCache L2 caching)
- **Rate Limiting & Security** (Custom implementations + Bloom Filters)
- **Geospatial Features** (Interactive maps with care location markers)
- **Application Monitoring** (Prometheus + Grafana)
- **Advanced Concurrency Control** (Transaction management for race conditions)
- **Load Balancing** (Custom implementation)
- **Automated Background Jobs** (Scheduled tasks for cleanup, compression, notifications)

---

## Key Features

### User Management
- **Multi-tier Authentication System**
  - Email-based registration with account activation
  - JWT token authentication with role-based access control (RBAC)
  - Rate-limited login attempts (5 attempts per IP per minute)
  - Bloom Filter optimization for username availability checks
  
- **User Roles**
  - Unauthenticated users: Browse posts and profiles
  - Registered users: Full social features (post, comment, like, follow)
  - Administrators: User management, content moderation, analytics

### Content Management
- **Post Creation & Management**
  - Upload rabbit photos with descriptions
  - Geolocation tagging (map selection or address input)
  - Edit/delete own posts
  - L2 caching for images and locations
  
- **Social Interactions**
  - Like posts with concurrent transaction handling
  - Comment on posts (rate-limited: 60 comments/hour per user)
  - Follow users (rate-limited: 50 follows/minute)
  - Real-time follower count updates

### Geospatial Features
- **Interactive Map Interface**
  - Display nearby posts on map (centered on user's address)
  - Care location markers (shelters, veterinarians)
  - Clickable markers with post previews
  - Location caching for performance

### Real-time Chat
- **WebSocket-based Messaging**
  - Direct messaging between users
  - Group chat creation with admin controls
  - Add/remove participants in group chats
  - View last 10 messages when joining group
  - Real-time message delivery

### Analytics & Trends
- **User Analytics Dashboard**
  - Total post count network-wide
  - Monthly post statistics
  - Top 5 posts in last 7 days (most liked)
  - Top 10 posts of all time
  - Top 10 users who gave most likes (last 7 days)
  - Cached trending data for performance

- **Admin Analytics**
  - Post and comment statistics (weekly/monthly/yearly)
  - User engagement breakdown (posted, commented only, inactive)
  - Radial charts for visualization
  - User search with pagination (5 users per page)
  - Sort by followers, email, post count

### Automated Notifications
- **Email Notification System**
  - Inactive user notifications (7 days without login)
  - Weekly statistics summary
  - Account activation emails
  - Scheduled via Cron jobs

### Background Maintenance
- **Automated Cleanup & Optimization**
  - Monthly deletion of unactivated accounts (last day of month)
  - Daily image compression (images older than 30 days)
  - Scheduled task execution with Spring Scheduler

### Message Queue Integration
- **RabbitMQ Implementation**
  - **Direct Queue**: Care location data from partner organization
  - **Fanout Exchange**: Post promotions to advertising agencies
  - Custom message queue implementation for extensibility

### Monitoring & Observability
- **Prometheus + Grafana Integration**
  - Average HTTP request duration for post creation (24h window)
  - CPU utilization tracking
  - Active user count (24h window)
  - Custom metrics exporters

### Security & Performance
- **Rate Limiting**
  - Login attempts: 5 per minute per IP
  - Comments: 60 per hour per user
  - Follows: 50 per minute per user
  - Custom rate limiter implementation

- **Concurrency Control**
  - Optimistic locking for like counters
  - Pessimistic locking for follower counts
  - Transaction isolation for username registration
  - Unit tests for concurrent scenarios

- **Bloom Filter Optimization**
  - Fast username existence checks
  - Reduced database queries
  - Snapshot persistence for restarts

---

### Components

1. **only-buns-fe/** - Angular 17+ frontend with responsive UI
2. **only-buns-be/** - Spring Boot backend (main application)
3. **queue-service/** - Custom message queue implementation
4. **bunny-org-app/** - Care organization integration service
5. **demo_ad_agencies/** - Advertising agency demo consumers


---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: PostgreSQL 15+
- **ORM**: Hibernate with JPA
- **Caching**: EhCache (L2 cache)
- **Message Queue**: RabbitMQ 3.x + Custom Implementation
- **Security**: Spring Security + JWT
- **WebSocket**: Spring WebSocket + SockJS + STOMP
- **Monitoring**: Micrometer + Prometheus
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Migration**: Flyway

### Frontend
- **Framework**: Angular 17+
- **Language**: TypeScript 5+
- **UI Components**: Angular Material / Custom
- **Maps**: OpenLayers / Leaflet
- **Charts**: Chart.js / D3.js
- **Real-time**: WebSocket Client

### DevOps & Monitoring
- **Monitoring**: Prometheus + Grafana
- **Database**: PostgreSQL
- **Message Broker**: RabbitMQ
- **Version Control**: Git
- **Build Tools**: Maven (Backend), npm (Frontend)

---

## Advanced Features

### 1. Bloom Filter for Username Checks
- **Purpose**: Optimize username existence queries
- **Implementation**: Custom Bloom filter with configurable false positive rate
- **Persistence**: Snapshot saved to disk, loaded on startup
- **Performance**: O(k) lookup time vs O(1) database query with network latency

### 2. Custom Message Queue Implementation
- **Features**:
  - Direct queue type support
  - Message persistence
  - Multiple consumer connections
  - Acknowledgment mechanism
- **Use Case**: Care location data ingestion from partner organization

### 3. Custom Load Balancer
- **Algorithm**: Round-robin / Least connections
- **Features**:
  - Automatic retry policy
  - Health checks for instances
  - Request distribution across multiple backend instances
- **Note**: Chat functionality not required in load-balanced setup

### 4. Custom Rate Limiter
- **Strategy**: In-memory sliding window
- **Configuration**: Per-user request tracking
- **Implementation**: Demonstrated on comment creation endpoint
- **Limit**: 60 comments per hour per user

### 5. Concurrent Transaction Handling
- **Scenarios Handled**:
  - Like counter increment (optimistic locking)
  - Follower count increment (pessimistic locking)
  - Username registration conflict (transaction isolation)
- **Testing**: Unit tests with Thread.sleep() and concurrent execution

### 6. Image Caching Strategy
- **Cache Type**: EhCache L2
- **Cached Data**:
  - Post images
  - Location coordinates
  - Trending posts
- **Eviction Policy**: LRU (Least Recently Used)
- **TTL**: Configurable per cache region

### 7. Geospatial Indexing
- **Database**: PostGIS extension for PostgreSQL
- **Queries**: Efficient radius-based searches for nearby posts
- **Caching**: Location data cached to reduce DB load

---

## Getting Started

### Prerequisites

- **Java 17+** - [Download](https://adoptium.net/)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **PostgreSQL 15+** - [Download](https://www.postgresql.org/download/)
- **RabbitMQ 3.x** - [Download](https://www.rabbitmq.com/download.html)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Prometheus** (Optional) - [Download](https://prometheus.io/download/)
- **Grafana** (Optional) - [Download](https://grafana.com/grafana/download)

### Database Setup

```sql
-- Create database
CREATE DATABASE bunhub;

-- Enable PostGIS extension (for geospatial features)
CREATE EXTENSION postgis;

-- Application will auto-create tables via Hibernate
-- Sample data loaded from data.sql on startup
```

### Backend Setup

```powershell
# Navigate to backend directory
cd only-buns-be

# Configure application.properties
# Update database credentials, RabbitMQ settings, email config

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Backend will start on http://localhost:8080
```

### Frontend Setup

```powershell
# Navigate to frontend directory
cd only-buns-fe

# Install dependencies
npm install

# Run development server
npm start

# Frontend will start on http://localhost:4200
```

### Queue Service Setup

```powershell
cd queue-service
./mvnw spring-boot:run
# Starts on port 8081
```

### RabbitMQ Setup

```powershell
# Start RabbitMQ server
rabbitmq-server

# Access management console
# http://localhost:15672 (guest/guest)

# Queues will be auto-created by application
```

### Monitoring Setup (Optional)

1. **Prometheus Configuration** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'bunhub'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

2. **Run Prometheus**:
```powershell
prometheus --config.file=prometheus.yml
# Access at http://localhost:9090
```

3. **Grafana Setup**:
```powershell
# Start Grafana
grafana-server

# Access at http://localhost:3000
# Add Prometheus as data source
# Import custom dashboards from /grafana folder
```

---

## API Documentation

Once the backend is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Key API Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/activate?token=...` - Account activation

#### Posts
- `GET /api/posts` - Get all posts (paginated)
- `POST /api/posts` - Create new post
- `GET /api/posts/{id}` - Get post details
- `PUT /api/posts/{id}` - Update post
- `DELETE /api/posts/{id}` - Delete post
- `POST /api/posts/{id}/like` - Like a post
- `POST /api/posts/{id}/comments` - Add comment

#### Users
- `GET /api/users/{id}` - Get user profile
- `POST /api/users/{id}/follow` - Follow user
- `DELETE /api/users/{id}/follow` - Unfollow user
- `GET /api/users/{id}/followers` - Get followers
- `GET /api/users/{id}/following` - Get following

#### Map
- `GET /api/map/nearby` - Get nearby posts
- `GET /api/map/care-locations` - Get care facilities

#### Analytics
- `GET /api/analytics/trends` - Get network trends
- `GET /api/admin/analytics` - Get admin analytics

#### WebSocket
- `CONNECT /ws` - WebSocket connection endpoint
- `SUBSCRIBE /topic/chat/{chatId}` - Subscribe to chat
- `SEND /app/chat/{chatId}` - Send message

---

## Monitoring & Analytics

### Prometheus Metrics

- `http_server_requests_seconds` - HTTP request duration
- `system_cpu_usage` - CPU utilization
- `active_users_count` - Current active users (custom metric)
- `jvm_memory_used_bytes` - JVM memory usage
- `cache_hits_total` - Cache hit ratio

### Grafana Dashboards

1. **Application Performance**
   - Request duration by endpoint
   - Request rate and error rate
   - 95th percentile response times

2. **Resource Utilization**
   - CPU and memory usage
   - Database connection pool metrics
   - Cache performance

3. **User Activity**
   - Active users over time
   - Posts/comments per hour
   - User engagement metrics

---

## Testing

### Run Backend Tests

```powershell
cd only-buns-be
./mvnw test
```

### Test Coverage Includes:
- Unit tests for services and utilities
- Integration tests for REST endpoints
- Concurrent transaction tests
- Bloom filter performance tests
- Rate limiter functionality tests

### Manual Testing Scenarios

1. **Concurrent Like Test**: Multiple users like same post simultaneously
2. **Concurrent Follow Test**: Multiple users follow same user
3. **Rate Limiter Test**: Exceed comment/follow limits
4. **WebSocket Test**: Multiple users in group chat
5. **Message Queue Test**: Send care locations, verify receipt

---


## Acknowledgments

- University of Novi Sad, Faculty of Technical Sciences
- Course: Internet Software Architectures (ISA)
- Semester: Fall 2024/2025



