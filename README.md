# Pasa Auto

A modern automotive workshop management system built with Quarkus and Vue.js, designed to streamline operations for automotive service centers.

## Overview

Pasa Auto is a comprehensive workshop management solution that handles:
- Vehicle and customer management
- Service and repair tracking
- Employee and attendance management
- Inventory and parts management
- Billing and invoicing
- Role-based access control

## Tech Stack

### Backend
- **Framework**: Quarkus 3.29.3
- **Language**: Java 21
- **Database**: PostgreSQL with Hibernate ORM
- **Security**: JWT authentication with RBAC
- **API**: RESTful with OpenAPI documentation

### Frontend
- **Framework**: Vue.js (via Quinoa)
- **Build Tool**: Vite
- **UI Components**: Modern component library

### Infrastructure
- **Containerization**: Docker support
- **Database Migrations**: Flyway
- **Testing**: JUnit 5 with Mockito
- **CI/CD**: GitHub Actions

## Quick Start

### Prerequisites
- Java 21+
- Node.js 24.11.0+
- PostgreSQL 14+
- Maven 3.8+

### Environment Setup
1. Copy environment template:
   ```bash
   cp .env.example .env
   ```

2. Configure your environment variables in `.env`:
   ```bash
   # Database
   DB_USERNAME=your_db_user
   DB_PASSWORD=your_db_password
   DB_URL=jdbc:postgresql://localhost:5432/pasa_auto
   
   # Security
   APP_SECURITY_SALT=your_random_salt_string
   
   # JWT Keys
   JWT_PRIVATE_KEY=path/to/your/private/key.pem
   JWT_PUBLIC_KEY=path/to/your/public/key.pem
   ```

### Running the Application

#### Development Mode
```bash
./mvnw quarkus:dev
```
The application will be available at `http://localhost:8080`

#### Production Build
```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

#### Native Executable
```bash
./mvnw package -Dnative
./target/pasa-auto-1.0.0-SNAPSHOT-runner
```

## API Documentation

- **Swagger UI**: `http://localhost:8080/swagger-ui` (when enabled)
- **OpenAPI Spec**: `http://localhost:8080/openapi`

## Project Structure

```
src/
├── main/
│   ├── java/com/github/b3kt/
│   │   ├── application/     # Application services and DTOs
│   │   ├── domain/          # Domain models
│   │   ├── infrastructure/  # Data access and external integrations
│   │   └── presentation/    # REST controllers and configuration
│   ├── resources/
│   │   ├── db/migration/    # Flyway database migrations
│   │   └── application.properties
│   └── webui/               # Vue.js frontend application
└── test/                    # Unit and integration tests
```

## Security

This application implements enterprise-grade security:
- JWT-based authentication
- Role-based access control (RBAC)
- Password hashing with salt
- CORS protection
- SSL/TLS support

See [ACTION_RULES.md](ACTION_RULES.md) for security guidelines and best practices.

## Database

The application uses PostgreSQL with the following key tables:
- `users` - User authentication and profiles
- `tb_karyawan` - Employee management
- `tb_kendaraan` - Vehicle information
- `tb_jasa` - Service catalog
- `tb_spk` - Service orders

Database migrations are managed with Flyway and located in `src/main/resources/db/migration/`.

## Development

### Code Quality
- Follows clean architecture principles
- Domain-driven design patterns
- Comprehensive test coverage (≥80% target)
- Automated security scanning

### Testing
```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Generate coverage report
./mvnw jacoco:report
```

## Deployment

### Docker
```bash
docker build -f src/main/docker/Dockerfile.jvm -t pasa-auto .
docker run -i --rm -p 8080:8080 pasa-auto
```

### Environment Variables
See `.env.example` for all required environment variables. Critical variables:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `APP_SECURITY_SALT`
- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add/update tests
5. Update CHANGELOG.md
6. Submit a pull request

All contributions must follow the guidelines in [ACTION_RULES.md](ACTION_RULES.md).

## Documentation

- [Project Overview](docs/overview.md)
- [API Documentation](docs/api.md)
- [Deployment Guide](docs/deployment.md)
- [Development Guide](docs/development.md)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the [documentation](docs/)
- Review [ACTION_RULES.md](ACTION_RULES.md) for guidelines
