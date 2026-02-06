# API Documentation

## Overview

Pasa Auto provides a comprehensive RESTful API for managing automotive workshop operations. The API follows OpenAPI 3.0 specifications and supports JSON for all request/response payloads.

## Base URL

- **Development**: `http://localhost:8080`
- **Production**: `https://your-domain.com`

## Authentication

All API endpoints (except authentication endpoints) require JWT authentication.

### Header Format
```
Authorization: Bearer <jwt_token>
```

### Token Endpoints

#### POST /api/auth/login
Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "token": "jwt_token_string",
  "refreshToken": "refresh_token_string",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "roles": ["ADMIN"]
  }
}
```

#### POST /api/auth/refresh
Refresh JWT token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "refresh_token_string"
}
```

#### POST /api/auth/logout
Invalidate current session.

**Headers:** `Authorization: Bearer <jwt_token>`

## API Endpoints

### Authentication Module

#### User Management
- `GET /api/users` - List all users (Admin only)
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user (Admin only)
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (Admin only)
- `GET /api/users/profile` - Get current user profile

#### Role Management
- `GET /api/roles` - List all roles
- `GET /api/roles/{id}` - Get role by ID
- `POST /api/roles` - Create new role (Admin only)
- `PUT /api/roles/{id}` - Update role
- `DELETE /api/roles/{id}` - Delete role

### Customer Management

#### Customers
- `GET /api/customers` - List all customers
- `GET /api/customers/{id}` - Get customer by ID
- `POST /api/customers` - Create new customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer
- `GET /api/customers/search` - Search customers by name/email/phone

#### Vehicles
- `GET /api/vehicles` - List all vehicles
- `GET /api/vehicles/{id}` - Get vehicle by ID
- `POST /api/vehicles` - Add new vehicle
- `PUT /api/vehicles/{id}` - Update vehicle
- `DELETE /api/vehicles/{id}` - Delete vehicle
- `GET /api/customers/{customerId}/vehicles` - Get vehicles by customer

### Service Management

#### Service Orders (SPK)
- `GET /api/service-orders` - List all service orders
- `GET /api/service-orders/{id}` - Get service order by ID
- `POST /api/service-orders` - Create new service order
- `PUT /api/service-orders/{id}` - Update service order
- `DELETE /api/service-orders/{id}` - Delete service order
- `PUT /api/service-orders/{id}/status` - Update order status
- `GET /api/service-orders/search` - Search service orders

#### Service Items
- `GET /api/service-orders/{orderId}/items` - Get items for service order
- `POST /api/service-orders/{orderId}/items` - Add service item
- `PUT /api/service-items/{id}` - Update service item
- `DELETE /api/service-items/{id}` - Delete service item

#### Service Catalog
- `GET /api/services` - List all available services
- `GET /api/services/{id}` - Get service by ID
- `POST /api/services` - Create new service (Admin only)
- `PUT /api/services/{id}` - Update service
- `DELETE /api/services/{id}` - Delete service

### Employee Management

#### Employees
- `GET /api/employees` - List all employees
- `GET /api/employees/{id}` - Get employee by ID
- `POST /api/employees` - Add new employee
- `PUT /api/employees/{id}` - Update employee
- `DELETE /api/employees/{id}` - Delete employee
- `GET /api/employees/search` - Search employees

#### Attendance
- `GET /api/attendance` - List attendance records
- `POST /api/attendance/check-in` - Check in employee
- `POST /api/attendance/check-out` - Check out employee
- `GET /api/attendance/employee/{id}` - Get employee attendance

### Inventory Management

#### Parts
- `GET /api/parts` - List all parts
- `GET /api/parts/{id}` - Get part by ID
- `POST /api/parts` - Add new part
- `PUT /api/parts/{id}` - Update part
- `DELETE /api/parts/{id}` - Delete part
- `GET /api/parts/search` - Search parts
- `PUT /api/parts/{id}/stock` - Update stock level

#### Suppliers
- `GET /api/suppliers` - List all suppliers
- `GET /api/suppliers/{id}` - Get supplier by ID
- `POST /api/suppliers` - Add new supplier
- `PUT /api/suppliers/{id}` - Update supplier
- `DELETE /api/suppliers/{id}` - Delete supplier

### Billing and Invoicing

#### Invoices
- `GET /api/invoices` - List all invoices
- `GET /api/invoices/{id}` - Get invoice by ID
- `POST /api/invoices` - Create new invoice
- `PUT /api/invoices/{id}` - Update invoice
- `DELETE /api/invoices/{id}` - Delete invoice
- `POST /api/invoices/{id}/payment` - Record payment
- `GET /api/invoices/search` - Search invoices

### Reports and Analytics

#### Reports
- `GET /api/reports/sales` - Sales report
- `GET /api/reports/services` - Service report
- `GET /api/reports/inventory` - Inventory report
- `GET /api/reports/employees` - Employee performance report
- `GET /api/reports/customers` - Customer report

## Data Models

### Customer
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "address": "123 Main St, City, State",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Vehicle
```json
{
  "id": 1,
  "customerId": 1,
  "make": "Toyota",
  "model": "Camry",
  "year": 2022,
  "vin": "1HGBH41JXMN109186",
  "licensePlate": "ABC-123",
  "color": "Blue",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Service Order
```json
{
  "id": 1,
  "customerId": 1,
  "vehicleId": 1,
  "orderNumber": "SPK-2024-001",
  "status": "IN_PROGRESS",
  "estimatedCompletion": "2024-01-02T00:00:00Z",
  "actualCompletion": null,
  "totalAmount": 150.00,
  "notes": "Regular maintenance service",
  "createdBy": 1,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Service Item
```json
{
  "id": 1,
  "serviceOrderId": 1,
  "serviceId": 1,
  "serviceName": "Oil Change",
  "quantity": 1,
  "unitPrice": 50.00,
  "totalPrice": 50.00,
  "assignedTo": 2,
  "status": "COMPLETED",
  "notes": "Used synthetic oil"
}
```

### Employee
```json
{
  "id": 1,
  "name": "Jane Smith",
  "email": "jane@example.com",
  "phone": "+1234567890",
  "position": "Mechanic",
  "department": "Service",
  "hireDate": "2023-01-01",
  "isActive": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

## Error Handling

### Standard Error Response
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "email",
        "message": "Email harus diisi"
      }
    ],
    "timestamp": "2024-01-01T00:00:00Z",
    "path": "/api/customers"
  }
}
```

### Common HTTP Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict
- `422 Unprocessable Entity` - Validation errors
- `500 Internal Server Error` - Server error

## Rate Limiting

API requests are limited to prevent abuse:
- **Authenticated users**: 1000 requests per hour
- **Unauthenticated requests**: 100 requests per hour
- **Burst limit**: 100 requests per minute

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995200
```

## Pagination

List endpoints support pagination using query parameters:

**Parameters:**
- `page` - Page number (default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Example:**
```
GET /api/customers?page=0&size=10&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15,
    "first": true,
    "last": false
  }
}
```

## Search and Filtering

Many endpoints support search and filtering:

**General Parameters:**
- `search` - Text search across multiple fields
- `filter[field]` - Filter by specific field
- `dateFrom` - Filter by date range (start)
- `dateTo` - Filter by date range (end)

**Example:**
```
GET /api/service-orders?search=Toyota&status=IN_PROGRESS&dateFrom=2024-01-01
```

## Webhooks

Configure webhooks to receive real-time notifications:

### Supported Events
- `service.order.created`
- `service.order.status_changed`
- `invoice.created`
- `invoice.paid`
- `inventory.low_stock`

### Configuration
Contact administrator to configure webhook endpoints and authentication.

## SDK and Libraries

Official SDKs are available for:
- JavaScript/TypeScript
- Java
- Python
- .NET

See the GitHub repository for installation and usage examples.

## Support

For API support:
- Documentation: [API Reference](http://localhost:8080/openapi)
- Issues: Create GitHub issue with `API` label
- Email: api-support@pasa-auto.com
