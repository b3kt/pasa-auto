# Swagger/OpenAPI Guide

## Access

| Resource | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui |
| OpenAPI JSON | http://localhost:8080/openapi |

**Note**: Swagger UI and the OpenAPI schema are available in dev mode (`quarkus:dev`) and are **not
included in packaged builds**, where both paths return 404. The schema lists every endpoint and its
payloads, which is exactly the map an attacker wants.

---

## Enable/Disable

These are Quarkus **build-time** settings: setting the variables on an already-built application has no
effect, you have to rebuild. Dev mode enables them through `%dev` overrides.

```bash
# Include the docs in a packaged build (e.g. a staging deployment)
./mvnw package -DOPENAPI_ENABLED=true -DSWAGGER_ENABLED=true -DSWAGGER_ALWAYS_INCLUDE=true
```

```properties
quarkus.smallrye-openapi.enabled=${OPENAPI_ENABLED:false}
quarkus.swagger-ui.enabled=${SWAGGER_ENABLED:false}
quarkus.swagger-ui.always-include=${SWAGGER_ALWAYS_INCLUDE:false}
quarkus.swagger-ui.path=/swagger-ui
quarkus.smallrye-openapi.path=/openapi
```

If you do expose them on a staging host, put them behind the reverse proxy's authentication.

---

## Using Swagger UI

### 1. Get a JWT Token

1. Navigate to `POST /api/auth/login` in Swagger UI
2. Click **Try it out**
3. Enter credentials:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
4. Click **Execute**
5. Copy the `token` value from the response

### 2. Authorize Requests

1. Click the **Authorize** button (lock icon) at the top
2. Enter: `Bearer <your-token>`
3. Click **Authorize** → **Close**

### 3. Test Endpoints

All protected endpoints now include your token automatically.

---

## Configuration

### API Metadata

Configured in `OpenApiConfig.java`:

```java
@Info(
    title = "Quarkus Quasar API",
    version = "0.0.28",
    description = "REST API for Quarkus Quasar Application"
)
```

### Security Scheme

JWT Bearer authentication is defined via `@SecurityScheme` annotation:

```java
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
```

---

## Customizing Documentation

### Add Endpoint Description

```java
@Operation(
    summary = "Create a new customer",
    description = "Creates a customer record with contact information"
)
@APIResponse(responseCode = "201", description = "Customer created")
@APIResponse(responseCode = "400", description = "Invalid input")
public Response create(CustomerRequest request) { ... }
```

### Document DTO Fields

```java
@Schema(description = "Customer request payload")
public class CustomerRequest {
    @Schema(description = "Customer full name", example = "John Doe")
    public String name;

    @Schema(description = "Email address", example = "john@example.com")
    public String email;
}
```

---

## Production

**Disable Swagger UI in production** to avoid exposing API details:

```bash
SWAGGER_ENABLED=false
OPENAPI_ENABLED=false
```

Or restrict access behind authentication/proxy.
