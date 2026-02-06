# Development Guide

## Overview

This guide covers setting up a development environment for Pasa Auto, including local development, testing, debugging, and contribution guidelines.

## Prerequisites

### Required Software
- **Java 21+**: OpenJDK or Oracle JDK
- **Maven 3.8+**: Build tool and dependency management
- **Node.js 24.11.0+**: Frontend development
- **PostgreSQL 14+**: Database (can use Docker)
- **Git**: Version control
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse

### Recommended Tools
- **Docker & Docker Compose**: For containerized development
- **Postman/Insomnia**: API testing
- **pgAdmin**: Database management
- **Git Hooks**: Pre-commit quality checks

## Development Environment Setup

### 1. Clone Repository
```bash
git clone https://github.com/b3kt/pasa-auto.git
cd pasa-auto
```

### 2. Backend Setup

#### Java and Maven
```bash
# Verify Java installation
java -version
javac -version

# Verify Maven installation
mvn -version
```

#### Database Setup
```bash
# Using Docker (recommended for development)
docker run --name postgres-dev \
  -e POSTGRES_DB=pasa_auto \
  -e POSTGRES_USER=dev_user \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  -d postgres:15-alpine

# Or install PostgreSQL locally
# Ubuntu/Debian:
sudo apt install postgresql postgresql-contrib
sudo -u postgres createuser --interactive
sudo -u postgres createdb pasa_auto
```

#### Environment Configuration
```bash
# Copy environment template
cp .env.example .env

# Edit .env for development
nano .env
```

Development `.env` example:
```bash
# Database
DB_USERNAME=dev_user
DB_PASSWORD=dev_password
DB_URL=jdbc:postgresql://localhost:5432/pasa_auto

# Security
APP_SECURITY_SALT=dev_salt_string_16_chars

# JWT (generate keys for development)
JWT_PRIVATE_KEY=keys/dev_private.pem
JWT_PUBLIC_KEY=keys/dev_public.pem
JWT_ISSUER=http://localhost:8080

# Development settings
SERVER_URL=http://localhost:8080
HTTP_PORT=8080
HTTPS_PORT=8443

# Enable development features
OPENAPI_ENABLED=true
SWAGGER_ENABLED=true
RBAC_ENABLED=true
```

#### Generate JWT Keys
```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/dev_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/dev_private.pem -out keys/dev_public.pem
```

### 3. Frontend Setup

#### Node.js and npm
```bash
# Verify Node.js installation
node --version
npm --version

# Navigate to webui directory
cd src/main/webui

# Install dependencies
npm install
```

### 4. IDE Configuration

#### IntelliJ IDEA
1. Import project as Maven project
2. Set JDK to Java 21
3. Enable Lombok plugin
4. Configure code style:
   - File → Settings → Editor → Code Style → Java
   - Import from `code-style.xml` if available

#### VS Code
1. Install extensions:
   - Extension Pack for Java
   - Lombok
   - Quarkus
   - Vue Language Features (Volar)

2. Configure settings:
```json
{
  "java.home": "/path/to/java21",
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic"
}
```

## Running the Application

### Development Mode
```bash
# From project root
./mvnw quarkus:dev

# Or with specific profile
./mvnw quarkus:dev -Dquarkus.profile=dev
```

Features available in dev mode:
- Live reload for Java code
- Hot reload for frontend
- Dev UI at `http://localhost:8080/q/dev/`
- Swagger UI at `http://localhost:8080/swagger-ui`
- Database console at `http://localhost:8080/q/console`

### Frontend Development
```bash
# Terminal 1: Backend
./mvnw quarkus:dev

# Terminal 2: Frontend (in separate terminal)
cd src/main/webui
npm run dev
```

### Testing the Setup
```bash
# Health check
curl http://localhost:8080/q/health

# API documentation
open http://localhost:8080/swagger-ui

# Frontend application
open http://localhost:8080
```

## Development Workflow

### 1. Feature Development
```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes
# ... develop feature ...

# Run tests
./mvnw test

# Run integration tests
./mvnw verify

# Commit changes
git add .
git commit -m "feat: add new feature description"

# Push branch
git push origin feature/new-feature
```

### 2. Database Changes
```bash
# Create new migration
./mvnw quarkus:add-extension -Dextensions=flyway

# Create migration file
# src/main/resources/db/migration/V3__feature_change.sql

# Test migration
./mvnw flyway:migrate

# Verify database state
psql -h localhost -U dev_user -d pasa_auto -c "\dt"
```

### 3. Code Quality Checks
```bash
# Run all quality checks
./mvnw clean verify

# Check test coverage
./mvnw jacoco:report
open target/site/jacoco/index.html

# Run security scan
./mvnw dependency-check:check

# Format code
./mvnw spotless:apply
```

## Testing

### Unit Tests
```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserServiceTest

# Run with coverage
./mvnw test jacoco:report

# Watch mode (requires plugin)
./mvnw test -Dwatch=true
```

### Integration Tests
```bash
# Run integration tests
./mvnw verify

# Run specific integration test
./mvnw verify -Dit.test=UserIntegrationTest

# Skip unit tests, run only integration
./mvnw verify -DskipTests=false -DskipITs=false
```

### Frontend Tests
```bash
cd src/main/webui

# Run unit tests
npm run test

# Run E2E tests
npm run test:e2e

# Run with coverage
npm run test:coverage
```

### Test Database
```bash
# Start test database
docker run --name postgres-test \
  -e POSTGRES_DB=pasa_auto_test \
  -e POSTGRES_USER=test_user \
  -e POSTGRES_PASSWORD=test_password \
  -p 5433:5432 \
  -d postgres:15-alpine

# Configure test profile
# src/test/resources/application-test.properties
```

## Debugging

### Backend Debugging
```bash
# Start in debug mode
./mvnw quarkus:dev -Ddebug

# Or with specific debug port
./mvnw quarkus:dev -Ddebug=5005

# Connect IDE debugger to localhost:5005
```

### Frontend Debugging
```bash
cd src/main/webui

# Debug mode
npm run dev

# Browser DevTools
# Open Developer Tools in browser
# Vue DevTools extension recommended
```

### Database Debugging
```bash
# Enable SQL logging
# In application.properties:
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.format-sql=true

# Connect with psql
psql -h localhost -U dev_user -d pasa_auto

# Common queries
\dt                    # List tables
\d table_name          # Describe table
SELECT * FROM users;   # Query data
```

### API Debugging
```bash
# Use curl for testing
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_TOKEN"

# Postman collection available at:
# docs/postman-collection.json
```

## Code Style and Standards

### Java Code Style
- Follow Google Java Style Guide
- Use Lombok for boilerplate reduction
- Keep methods under 20 lines
- Use meaningful variable names
- Add JavaDoc for public APIs

### Frontend Code Style
- Follow Vue.js Style Guide
- Use Composition API
- TypeScript for type safety
- Component-based architecture
- Responsive design principles

### Commit Message Format
```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style
- `refactor`: Refactoring
- `test`: Tests
- `chore`: Maintenance

Examples:
```
feat(auth): add JWT refresh token support

fix(api): handle null values in customer endpoint

docs(readme): update installation instructions
```

## Performance Optimization

### Backend Performance
```bash
# Profile application
./mvnw quarkus:dev -Dquarkus.profile=profiling

# Memory analysis
jmap -histo <pid>
jstack <pid>

# Database query analysis
# Enable slow query logging in PostgreSQL
```

### Frontend Performance
```bash
cd src/main/webui

# Bundle analysis
npm run build -- --analyze

# Lighthouse audit
npm run lighthouse

# Performance testing
npm run perf
```

## Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check what's using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Change port in application.properties
quarkus.http.port=8081
```

#### Database Connection Issues
```bash
# Check PostgreSQL status
docker ps | grep postgres

# Test connection
psql -h localhost -U dev_user -d pasa_auto

# Reset database
docker rm -f postgres-dev
docker run --name postgres-dev ...
```

#### Build Issues
```bash
# Clean build
./mvnw clean compile

# Clear Maven cache
./mvnw dependency:purge-local-repository

# Rebuild frontend
cd src/main/webui
rm -rf node_modules package-lock.json
npm install
```

#### Frontend Issues
```bash
# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Check Node version compatibility
node --version  # Should be 24.11.0+
```

## Contributing

### Before Contributing
1. Read [ACTION_RULES.md](../ACTION_RULES.md)
2. Set up development environment
3. Run existing tests to ensure setup is correct
4. Create issue for your contribution (if not already exists)

### Contribution Process
1. Fork the repository
2. Create feature branch from `main`
3. Implement your changes
4. Add/update tests
5. Ensure code quality standards are met
6. Update documentation if needed
7. Submit pull request with:
   - Clear description of changes
   - Reference to related issue
   - Screenshots if UI changes
   - Test results

### Code Review Guidelines
- Review for functionality and correctness
- Check code style and standards
- Verify test coverage
- Ensure documentation is updated
- Check for security implications

### Pull Request Template
```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Added new tests

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
```

## Development Tools

### Recommended VS Code Extensions
- Quarkus Tools
- Java Extension Pack
- Lombok
- Vue Language Features
- GitLens
- Docker
- Thunder Client (API testing)

### Recommended IntelliJ Plugins
- Quarkus
- Lombok
- SonarLint
- Docker
- .env files support
- Key Promoter X

### Browser Extensions
- Vue.js devtools
- React Developer Tools (if needed)
- JSON Viewer
- Postman Interceptor

## Resources

### Documentation
- [Quarkus Documentation](https://quarkus.io/guides/)
- [Vue.js Documentation](https://vuejs.org/guide/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Hibernate ORM](https://hibernate.org/orm/documentation/)

### Community
- [Quarkus Discord](https://discord.gg/quarkus)
- [Vue.js Discord](https://discord.vuejs.org/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus)

### Learning Resources
- [Quarkus Tutorials](https://quarkus.io/guides/)
- [Vue.js Tutorial](https://vuejs.org/tutorial/)
- [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350884)

## Support

For development issues:
1. Check this troubleshooting guide
2. Search existing GitHub issues
3. Create new issue with `development` label
4. Join our Discord community
5. Email dev-support@pasa-auto.com
