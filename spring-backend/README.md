# AidBridge Spring Backend

This is the Spring Boot backend for the AidBridge disaster relief coordination system.

## Current Setup Status ✅

**Project Configuration:**
- ✅ Spring Boot 3.4.1 with Java 17
- ✅ Dependencies configured (Web, Security, JPA, PostGIS, WebSocket, etc.)
- ✅ Local configuration template created

**Package Structure (According to README):**
```
spring-backend/
├── src/main/java/com/drc/aidbridge/
│   ├── config/                 ✅ CORS, WebSocket configurations
│   ├── controller/             ✅ REST API Endpoints (Health, User)
│   ├── dto/
│   │   ├── request/            ✅ CreateUserRequest
│   │   └── response/           ✅ ApiResponse wrapper
│   ├── domain/
│   │   ├── entity/             ✅ User entity with geospatial support
│   │   └── enums/              ✅ UserRole enum
│   ├── mapper/                 📝 Ready for MapStruct mappers
│   ├── exception/              ✅ GlobalExceptionHandler
│   ├── repository/             ✅ UserRepository with PostGIS queries
│   ├── security/               📝 Ready for JWT implementation
│   ├── service/
│   │   ├── impl/               📝 Ready for service implementations
│   │   ├── strategy/           📝 Ready for dispatch algorithms
│   │   └── observer/           📝 Ready for inventory observers
│   └── websocket/              📝 Ready for STOMP handlers
└── resources/
    ├── application.yaml                 ✅ Global configuration
    ├── application-local.example.yaml   ✅ Template file
    └── application-local.yaml           ✅ Local secrets (DO NOT commit)
```

## Setup Instructions

1. **Prerequisites:**
   - Java 25 installed
   - PostgreSQL with PostGIS extension

2. **Database Setup:**
   - Create database: `aidbridge`
   - Enable PostGIS extension
   - Run SQL scripts in `database/` folder

3. **Configuration:**
   - Copy `application-local.example.yaml` to `application-local.yaml`
   - Fill in your database credentials and other secrets

4. **Build & Run:**
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

## API Endpoints

- `GET /api/health` - Health check
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/role/{role}` - Get users by role
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Deactivate user

## Next Development Steps

1. **Database Layer:**
   - Create entities for SOS requests, Hubs, Inventory
   - Set up database schema with PostGIS

2. **Authentication:**
   - Implement JWT authentication
   - Add Spring Security configuration

3. **Business Logic:**
   - Implement dispatch algorithms (Strategy pattern)
   - Add inventory management with observers

4. **Real-time Features:**
   - WebSocket handlers for chat and live tracking
   - Firebase push notifications

5. **API Development:**
   - Complete REST endpoints for all user roles
   - Add comprehensive validation and error handling

## Technologies Used

- **Spring Boot 4.0.3** - Framework
- **Java 25** - Language
- **PostgreSQL + PostGIS** - Geospatial database
- **Spring Security + JWT** - Authentication
- **Spring WebSocket + STOMP** - Real-time communication
- **Firebase Admin SDK** - Push notifications
- **MapStruct** - DTO mapping
- **Lombok** - Boilerplate reduction
