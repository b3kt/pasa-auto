# Swagger/OpenAPI Documentation

This project includes Swagger UI for interactive API documentation.

## Accessing Swagger UI

Once the application is running, you can access Swagger UI at:

**Development:**
- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI JSON: http://localhost:8080/openapi

## Features

### 1. Interactive API Documentation
- Browse all available endpoints
- See request/response schemas
- Test endpoints directly from the browser

### 2. JWT Authentication Support
- Swagger UI includes an "Authorize" button
- Enter your JWT token to test protected endpoints
- Token format: `Bearer <your-jwt-token>`

### 3. Request/Response Examples
- All DTOs include example values
- Validation rules are documented
- Error responses are documented

## Using Swagger UI

### Step 1: Login
1. Navigate to the `/api/auth/login` endpoint
2. Click "Try it out"
3. Enter credentials:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
4. Click "Execute"
5. Copy the `token` from the response

### Step 2: Authorize
1. Click the "Authorize" button at the top of the page
2. In the "bearerAuth" section, enter: `Bearer <your-token>`
3. Click "Authorize"
4. Click "Close"

### Step 3: Test Protected Endpoints
- Now you can test `/api/auth/me` and `/api/auth/logout` endpoints
- The JWT token will be automatically included in requests

## API Endpoints

### Authentication Endpoints

#### POST `/api/auth/login`
- **Description**: Authenticate user and get JWT token
- **Auth Required**: No
- **Request Body**: `LoginRequest` (username, password)
- **Response**: `LoginResponse` (token, username, email, expiresIn)

#### POST `/api/auth/logout`
- **Description**: Logout current user
- **Auth Required**: Yes (Bearer token)
- **Response**: Success message

#### GET `/api/auth/me`
- **Description**: Get current user information
- **Auth Required**: Yes (Bearer token)
- **Response**: `UserInfo` (username, email, roles)

## Configuration

Swagger configuration is in:
- `application.properties`: Basic settings
- `OpenApiConfig.java`: API metadata and security schemes

## Customization

To customize Swagger documentation:

1. **Update API Info**: Edit `OpenApiConfig.java`
2. **Add Endpoint Documentation**: Use `@Operation`, `@APIResponse` annotations
3. **Add Schema Documentation**: Use `@Schema` annotations on DTOs
4. **Configure Security**: Update `@SecurityScheme` in `OpenApiConfig.java`

## Production

In production, you may want to:
- Disable Swagger UI: Set `quarkus.swagger-ui.always-include=false`
- Restrict access: Use authentication/authorization
- Customize paths: Update `quarkus.swagger-ui.path` and `quarkus.smallrye-openapi.path`

