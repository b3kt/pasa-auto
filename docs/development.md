# Development Guide

## Quick Start

```bash
# 1. Clone
git clone https://github.com/b3kt/pasa-auto.git && cd pasa-auto

# 2. Configure
cp .env.example .env
# Edit .env with your values

# 3. Start database
docker run -d --name postgres-dev \
  -e POSTGRES_DB=pasa_auto -e POSTGRES_USER=dev \
  -e POSTGRES_PASSWORD=dev -p 5432:5432 postgres:15

# 4. Run
./mvnw quarkus:dev
```

Open:
- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui
- Dev UI: http://localhost:8080/q/dev/

---

## Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 25+ | Backend runtime |
| Maven | 3.9+ | Build tool |
| Node.js | 24.11.0+ | Frontend build (via Quinoa) |
| PostgreSQL | 15+ | Database |
| Docker | 20.10+ | Containerized database |

### Recommended IDEs
- **IntelliJ IDEA**: Install Lombok + Quarkus plugins
- **VS Code**: Install Extension Pack for Java + Volar

---

## Project Structure

```
src/main/java/com/github/b3kt/
├── domain/              # Business models, domain exceptions
├── application/         # DTOs, services, mappers
├── infrastructure/      # JPA entities, repositories, security
└── presentation/        # REST resources, exception handlers

src/main/resources/
├── application.properties
└── db/migration/        # Flyway migrations (V1-V13)

src/main/webui/          # Quasar/Vue.js frontend
└── src/
    ├── pages/           # Vue page components
    ├── components/      # Reusable components
    ├── services/        # API service layer
    ├── stores/          # Pinia state management
    ├── composables/     # Vue composables (useCrud, etc.)
    ├── boot/            # Boot files (axios, auth, i18n)
    └── router/          # Vue Router configuration
```

---

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/pasa_auto
DB_USERNAME=dev
DB_PASSWORD=dev

# Security
APP_SECURITY_SALT=your_random_salt_at_least_16_chars

# JWT (generate keys first)
JWT_PRIVATE_KEY=src/main/resources/privateKey-pkcs8.pem
JWT_PUBLIC_KEY=src/main/resources/publicKey.pem
JWT_ISSUER=http://localhost:8080

# Development
SWAGGER_ENABLED=true
RBAC_ENABLED=false
```

### Generate JWT Keys

```bash
openssl genpkey -algorithm RSA -out src/main/resources/privateKey.pem \
  -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in src/main/resources/privateKey.pem \
  -out src/main/resources/publicKey.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in src/main/resources/privateKey.pem \
  -out src/main/resources/privateKey-pkcs8.pem
```

---

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Develop

Backend changes auto-reload in dev mode. For frontend-only work:

```bash
cd src/main/webui && npm run dev
```

### 3. Database Changes

Create a new Flyway migration:

```bash
# Naming: V{number}__description.sql
# Example: src/main/resources/db/migration/V14__add_new_column.sql

CREATE TABLE tb_new_table (
    id BIGSERIAL PRIMARY KEY,
    -- columns...
);
```

### 4. Test

```bash
# Unit tests
./mvnw test

# Integration tests + coverage
./mvnw clean verify

# View coverage report
open target/site/jacoco/index.html
```

### 5. Commit

Follow conventional commits:

```bash
git add .
git commit -m "feat(spk): add filtering by date range"
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

---

## Testing

### Unit Tests

```bash
# Run all tests
./mvnw test

# Run single test class
./mvnw test -Dtest=TbSpkServiceTest

# Run tests matching pattern
./mvnw test -Dtest="*ServiceTest"
```

### Integration Tests

```bash
# Runs with Testcontainers (PostgreSQL)
./mvnw verify
```

### Test Structure

```
src/test/java/
└── com/github/b3kt/
    ├── application/service/   # Service unit tests
    ├── presentation/rest/     # REST endpoint tests
    └── infrastructure/        # Repository integration tests
```

### Coverage

Coverage threshold: **80% minimum** (enforced by JaCoCo in CI).

---

## Debugging

### Backend

```bash
# Dev mode includes debug port 5005
./mvnw quarkus:dev

# Connect IDE debugger to localhost:5005
```

Enable SQL logging in `application.properties`:
```properties
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.format-sql=true
```

### Frontend

```bash
cd src/main/webui && npm run dev
```

Use Vue DevTools browser extension.

### Database

```bash
# Connect to dev database
psql -h localhost -U dev -d pasa_auto

# List tables
\dt

# Describe table
\d tb_spk
```

---

## Troubleshooting

### Port 8080 Already in Use

```bash
# Find process
lsof -i :8080

# Kill or change port
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

### Database Connection Failed

```bash
# Check container
docker ps | grep postgres

# Restart
docker restart postgres-dev

# Reset (destroys data)
docker rm -f postgres-dev && docker run ...
```

### Build Failures

```bash
# Clean and rebuild
./mvnw clean compile

# Clear Maven cache
./mvnw dependency:purge-local-repository

# Reinstall frontend deps
cd src/main/webui && rm -rf node_modules && npm install
```

---

## Code Conventions

- Clean Architecture: domain → application → infrastructure → presentation
- Java: PascalCase classes, camelCase methods/fields
- Frontend: Vue Composition API, PascalCase components
- DTOs: Separate request/response objects in `application/dto/`
- Services: Extend `AbstractCrudService<T>` for standard CRUD
- Resources: Extend `AbstractCrudResource<T>` for standard REST endpoints

---

## Resources

| Resource | Link |
|----------|------|
| Quarkus Guides | https://quarkus.io/guides/ |
| Vue.js Guide | https://vuejs.org/guide/ |
| Quasar Docs | https://quasar.dev/ |
| Hibernate ORM | https://hibernate.org/orm/ |
