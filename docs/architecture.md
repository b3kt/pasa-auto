# Clean Modular Architecture

This document describes the clean modular architecture implemented in the Quarkus backend.

## Architecture Overview

The application follows a **Clean Architecture** pattern with clear separation of concerns across four main layers:

```
com.github.b3kt
├── domain/          # Domain Layer (Business Logic)
├── application/     # Application Layer (Use Cases)
├── infrastructure/  # Infrastructure Layer (Technical Details)
└── presentation/    # Presentation Layer (API/Controllers)
```

## Layer Responsibilities

### 1. Domain Layer (`domain/`)

**Purpose**: Contains the core business logic and domain entities.

**Contents**:
- **`model/`**: Domain entities (e.g., `User`)
- **`exception/`**: Domain-specific exceptions

**Principles**:
- No dependencies on other layers
- Pure business logic
- Framework-agnostic

**Example**:
```java
// domain/model/User.java
public class User {
    // Business logic methods
    public boolean canAuthenticate() { ... }
}
```

### 2. Application Layer (`application/`)

**Purpose**: Orchestrates use cases and application logic.

**Contents**:
- **`dto/`**: Data Transfer Objects for API communication
- **`service/`**: Application service interfaces and implementations
- **`mapper/`**: Mappers between domain entities and DTOs

**Principles**:
- Depends only on domain layer
- Defines use cases (interfaces)
- Coordinates domain objects

**Example**:
```java
// application/service/AuthService.java (interface)
public interface AuthService {
    LoginResponse login(String username, String password);
}

// application/service/AuthServiceImpl.java (implementation)
@ApplicationScoped
public class AuthServiceImpl implements AuthService {
    // Orchestrates domain logic
}
```

### 3. Infrastructure Layer (`infrastructure/`)

**Purpose**: Implements technical concerns and external integrations.

**Contents**:
- **`repository/`**: Data access implementations
- **`security/`**: Security implementations (JWT, password encoding)

**Principles**:
- Implements interfaces defined in application layer
- Handles technical details (database, JWT, etc.)
- Can be swapped without affecting other layers

**Example**:
```java
// infrastructure/repository/UserRepository.java (interface)
public interface UserRepository {
    Optional<User> findByUsername(String username);
}

// infrastructure/repository/InMemoryUserRepository.java (implementation)
@ApplicationScoped
public class InMemoryUserRepository implements UserRepository {
    // In-memory storage implementation
}
```

### 4. Presentation Layer (`presentation/`)

**Purpose**: Handles HTTP requests and responses.

**Contents**:
- **`rest/`**: REST controllers
- **`exception/`**: Exception mappers for HTTP responses
- **`config/`**: Presentation-specific configuration

**Principles**:
- Depends on application layer
- Handles HTTP concerns
- Maps HTTP to application layer

**Example**:
```java
// presentation/rest/AuthResource.java
@Path("/api/auth")
public class AuthResource {
    @Inject
    AuthService authService; // Depends on application layer
    
    @POST
    @Path("/login")
    public Response login(LoginRequest request) { ... }
}
```

## Dependency Flow

```
Presentation → Application → Domain
     ↓              ↓
Infrastructure → Application → Domain
```

**Key Rules**:
1. **Domain** has no dependencies
2. **Application** depends only on **Domain**
3. **Infrastructure** implements **Application** interfaces and depends on **Domain**
4. **Presentation** depends on **Application** and **Domain**

## Benefits

1. **Testability**: Each layer can be tested independently
2. **Maintainability**: Clear separation makes code easier to understand and modify
3. **Flexibility**: Infrastructure can be swapped (e.g., in-memory to database)
4. **Scalability**: Easy to add new features without affecting existing code
5. **Independence**: Business logic is independent of frameworks

## Module Structure

### Authentication Module

```
domain/
  └── model/User.java
  └── exception/
      ├── AuthenticationException.java
      └── UserNotFoundException.java

application/
  └── dto/
      ├── LoginRequest.java
      ├── LoginResponse.java
      └── UserInfo.java
  └── service/
      ├── AuthService.java (interface)
      └── AuthServiceImpl.java
  └── mapper/
      └── UserMapper.java

infrastructure/
  └── repository/
      ├── UserRepository.java (interface)
      └── InMemoryUserRepository.java
  └── security/
      ├── JwtTokenService.java (interface)
      ├── JwtTokenServiceImpl.java
      ├── PasswordEncoder.java (interface)
      └── SimplePasswordEncoder.java

presentation/
  └── rest/
      └── AuthResource.java
  └── exception/
      └── GlobalExceptionHandler.java
```

## Adding New Features

To add a new feature (e.g., User Management):

1. **Domain**: Create `User` entity (if not exists) and domain exceptions
2. **Application**: Create DTOs, service interface, and implementation
3. **Infrastructure**: Implement repository interface (e.g., `JpaUserRepository`)
4. **Presentation**: Create REST controller

## Testing Strategy

- **Domain**: Unit tests for business logic
- **Application**: Integration tests with mocked infrastructure
- **Infrastructure**: Integration tests with test database
- **Presentation**: REST API tests

## Configuration

Configuration is externalized in `application.properties`:
- JWT settings
- CORS configuration
- Security settings

