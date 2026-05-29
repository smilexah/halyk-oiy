# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot microservices architecture for an order management system with authentication and notification capabilities. The project uses Gradle with Kotlin DSL for build management, Spring Cloud for service discovery, and Keycloak for OAuth2/OIDC authentication.

## Architecture

### Microservices Structure

The project consists of 4 independent Spring Boot services, each with its own Gradle build configuration:

1. **Eureka Server** (`eureka-server/`)
   - Service discovery and registry (Spring Cloud Netflix Eureka)
   - Runs on port 8761
   - Other services register with and discover each other through this

2. **API Gateway** (`api-gateway/`)
   - Entry point for all client requests
   - Spring Cloud Gateway with WebFlux (reactive stack)
   - Routes requests to backend services using load balancing (`lb://` URIs)
   - OAuth2 resource server that validates JWT tokens from Keycloak
   - Runs on port 8084 (configurable via PORT env var)
   - CORS enabled for all origins by default

3. **Auth Service** (`auth-service/`)
   - User authentication and authorization
   - Manages user data in PostgreSQL (Flyway migrations)
   - Integrates with Keycloak admin client for user management
   - Provides Swagger/OpenAPI documentation at `/api/swagger-ui.html`
   - Runs on port 8080
   - Uses MapStruct for DTO mapping and Lombok for boilerplate

4. **Notification Service** (`notification-service/`)
   - Minimal stub service (Spring Boot WebMVC only)
   - Placeholder for notification functionality

### Key Technology Stack

- **Java 21** (language version across all services)
- **Spring Boot 3.5.6** (api-gateway, auth-service, eureka-server)
- **Spring Boot 4.0.6** (notification-service - note the difference)
- **Spring Cloud 2025.0.0** (Eureka, Gateway)
- **Spring Security + OAuth2 Resource Server** (validates Keycloak JWT tokens)
- **PostgreSQL 18** (for auth-service data, with Flyway migrations)
- **Keycloak 26.x** (OAuth2/OIDC provider)
- **Gradle 8.13 with Kotlin DSL** (build tool)
- **Prometheus/Micrometer** (metrics)
- **SpringDoc OpenAPI 2.8.13** (Swagger UI for auth-service)

### Authentication & Security

- **Keycloak** is the central OAuth2/OIDC provider
- Services validate JWT tokens using the JWKS endpoint from Keycloak
- API Gateway enforces OAuth2 authentication for all routes except `/api/auth/**` and `/actuator/**`
- Auth Service manages user lifecycle and integration with Keycloak admin client
- Services use `spring-boot-starter-oauth2-resource-server` to validate bearer tokens

### Service Discovery & Load Balancing

- Services register with Eureka on startup
- API Gateway routes to services using `lb://service-name` URIs (Spring Cloud load balancer)
- Eureka is the single point of service discovery

### Database

- Auth Service uses PostgreSQL with Flyway schema versioning
- Migration files: `auth-service/src/main/resources/db/migration/`
- Database connection: configurable via `POSTGRES_ADDRESS`, `POSTGRES_DB_NAME`, etc.

## Build and Run Commands

Each service has its own `gradlew`/`gradlew.bat` script in its directory. Services are independent Gradle projects.

### Build Commands

**Build a specific service:**
```bash
cd api-gateway && ./gradlew build
cd auth-service && ./gradlew build
cd eureka-server && ./gradlew build
cd notification-service && ./gradlew build
```

**Build without running tests:**
```bash
./gradlew build -x test
```

**Build Docker image (creates JAR and packages in Docker):**
Each service has a Dockerfile that:
1. Builds the JAR using Gradle in an Alpine builder image
2. Runs the JAR in a lightweight JRE image

### Run Commands

**Run a service locally (from its directory):**
```bash
./gradlew bootRun
```

**Run tests for a service:**
```bash
./gradlew test
```

**Run a single test class:**
```bash
./gradlew test --tests TestClassName
```

**Run a single test method:**
```bash
./gradlew test --tests TestClassName.testMethodName
```

### Docker Compose

**Run the full stack:**
```bash
docker-compose up
```

This starts:
- PostgreSQL for auth-service
- PostgreSQL for Keycloak
- Keycloak (OAuth2 provider) on port 8181
- API Gateway on port 8080
- Eureka Server on port 8761
- Dozzle (Docker log viewer) on port 9999

**Configuration via `.env` file:**
The docker-compose.yml references environment variables like `KEYCLOAK_ADMIN`, `DATABASE_USERNAME`, etc. Create a `.env` file in the root to set these.

## Configuration

### Environment Variables

Services use environment variables for runtime configuration. Common ones:

**All services:**
- `PORT` - Server port
- `EUREKA_HOST`, `EUREKA_PORT` - Eureka server location

**API Gateway:**
- `KEYCLOAK_HOST`, `KEYCLOAK_PORT`, `KEYCLOAK_REALM` - Keycloak connection
- `ALLOWED_ORIGINS` - CORS allowed origins

**Auth Service:**
- `POSTGRES_ADDRESS`, `POSTGRES_ADDRESS_PORT`, `POSTGRES_DB_NAME`
- `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `OAUTH2_ISSUER` - Keycloak issuer URI for token validation

**Keycloak:**
- `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` - Admin credentials
- `KC_HOSTNAME_STRICT`, `KC_PROXY` - Network configuration

### Application Configuration Files

Each service has `application.yml` in `src/main/resources/`:
- Defines Spring application name (for Eureka registration)
- Database connection settings (auth-service)
- OAuth2 issuer URIs for token validation
- Gateway routes (api-gateway)
- Actuator endpoints for health checks and metrics

## Project Structure

### Common Structure Per Service

```
service-name/
├── build.gradle.kts        # Service-specific build config
├── settings.gradle.kts     # Gradle settings (rootProject.name)
├── Dockerfile              # Multi-stage build for service
├── src/main/java/          # Source code
│   └── sdu/ai/lab/
│       └── [service-name]/
│           ├── config/     # Spring configuration beans
│           ├── controllers/ # REST endpoints (if applicable)
│           ├── dto/        # Data transfer objects
│           ├── entities/   # JPA entities (if applicable)
│           ├── services/   # Business logic
│           ├── repositories/ # Data access (if applicable)
│           └── security/   # Security configs, Keycloak integration
├── src/main/resources/
│   ├── application.yml     # Spring Boot configuration
│   └── db/migration/       # Flyway SQL migrations (auth-service only)
└── src/test/               # Unit tests
```

### Auth Service Specifics

Rich architecture with layered structure:
- `controllers/` - REST APIs for auth and user management
- `services/impl/` - Business logic (UserServiceImpl, KeycloakServiceImpl)
- `security/keycloak/` - Keycloak client models (KeycloakBaseUser, KeycloakRole, etc.)
- `mappers/` - MapStruct DTOs (UserMapper)
- `validators/` - Custom validators for file uploads
- `exceptions/handler/` - Centralized exception handling with GlobalExceptionHandler

### API Gateway Specifics

Minimal config:
- `config/SecurityConfig.java` - OAuth2 resource server + WebFlux security
- `filter/UserContextFilter.java` - Propagates auth context downstream
- `application.yml` - Route definitions to auth-service and order-service (via load balancer)

## Testing

Uses **JUnit 5** with `junit-platform-launcher` for test running.

```bash
# Run all tests
./gradlew test

# Run with specific pattern
./gradlew test --tests "Auth*"

# Run a single test
./gradlew test --tests sdu.ai.lab.authservice.AuthServiceApplicationTests
```

## Linting & Code Quality

- **Lombok** is used extensively to reduce boilerplate
- **MapStruct** for DTO mapping (annotation processor generates implementations)
- No explicit lint/checkstyle configuration found - relies on IDE and Spring conventions

## Metrics and Health Checks

All services expose actuator endpoints:
- `/actuator/health` - Service health status
- `/actuator/info` - Application info
- `/actuator/prometheus` - Prometheus metrics (auth-service, api-gateway)

Docker compose health checks query these endpoints to determine service readiness.

## Common Development Patterns

### DTO Mapping
Auth Service uses MapStruct with Lombok:
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
```

### Service Injection
Heavy use of constructor injection with Lombok's `@RequiredArgsConstructor`:
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl {
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
}
```

### Exception Handling
Centralized in `GlobalExceptionHandler` with custom exceptions like `DbObjectNotFoundException`, `UnauthorizedException`.

### OAuth2 Token Validation
Services use Spring Security's `@EnableGlobalMethodSecurity` or `@EnableWebFluxSecurity` (gateway). Token validation happens automatically via JWT JWKS endpoint from Keycloak - no manual validation needed.

## Important Notes

1. **Java 21 Requirement** - All services target Java 21. Ensure your JDK matches.
2. **Gradle Wrapper** - Use `./gradlew` (Unix) or `gradlew.bat` (Windows) rather than system Gradle. Wrappers are committed.
3. **Service Independence** - Each service is a completely independent Gradle project. Building one doesn't affect others.
4. **Keycloak Integration** - Auth service uses `org.keycloak:keycloak-admin-client` for user management. Configuration is environment-based.
5. **API Gateway Routes** - Currently routes to `order-service` (not yet in repo) and `auth-service`. Extend `application.yml` to add new routes.
6. **Flyway Migrations** - Auth service uses Flyway for schema management with baseline-on-migrate enabled. Only auth-service has migrations; others use JPA DDL or no schema.
