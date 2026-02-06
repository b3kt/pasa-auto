# Deployment Guide

## Overview

This guide covers deploying Pasa Auto to various environments, from development to production. The application is designed to be container-first and supports multiple deployment strategies.

## Prerequisites

### System Requirements
- **CPU**: Minimum 2 cores, recommended 4 cores
- **Memory**: Minimum 4GB RAM, recommended 8GB RAM
- **Storage**: Minimum 20GB available space
- **Network**: Internet connectivity for dependencies

### Software Requirements
- **Docker**: 20.10+ and Docker Compose 2.0+
- **Java**: 21+ (for non-Docker deployments)
- **PostgreSQL**: 14+ (if using external database)
- **Node.js**: 24.11.0+ (for local development)

## Environment Configuration

### Environment Variables

Copy the environment template and configure for your environment:

```bash
cp .env.example .env
```

#### Required Variables
```bash
# Database Configuration
DB_USERNAME=your_database_user
DB_PASSWORD=your_secure_password
DB_URL=jdbc:postgresql://your-db-host:5432/pasa_auto

# Security Configuration
APP_SECURITY_SALT=your_random_salt_string_at_least_16_chars

# JWT Configuration
JWT_PRIVATE_KEY=/path/to/your/private/key.pem
JWT_PUBLIC_KEY=/path/to/your/public/key.pem
JWT_ISSUER=https://your-domain.com

# Server Configuration
SERVER_URL=https://your-domain.com
HTTP_PORT=8080
HTTPS_PORT=8443

# SSL Configuration (optional for production)
CERT_FILE_PATH=/path/to/ssl/certificate.pem
KEY_FILE_PATH=/path/to/ssl/private.key
```

#### Optional Variables
```bash
# Feature Flags
RBAC_ENABLED=true
OPENAPI_ENABLED=false
SWAGGER_ENABLED=false

# Database Connection Pool
DB_MAX_SIZE=20
DB_MIN_SIZE=5

# Logging
LOG_PATH=/var/log/pasa-auto/app.log
```

## Deployment Options

### 1. Docker Deployment (Recommended)

#### Build Docker Image
```bash
# Build JVM image
docker build -f src/main/docker/Dockerfile.jvm -t pasa-auto:latest .

# Or build native image
docker build -f src/main/docker/Dockerfile.native -t pasa-auto:native .
```

#### Docker Compose Deployment

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  app:
    image: pasa-auto:latest
    ports:
      - "8080:8080"
      - "8443:8443"
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/pasa_auto
      - DB_USERNAME=pasa_user
      - DB_PASSWORD=${DB_PASSWORD}
      - APP_SECURITY_SALT=${APP_SECURITY_SALT}
      - JWT_PRIVATE_KEY=/app/keys/private.pem
      - JWT_PUBLIC_KEY=/app/keys/public.pem
    volumes:
      - ./keys:/app/keys:ro
      - ./logs:/var/log/pasa-auto
    depends_on:
      - postgres
      - redis
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/q/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=pasa_auto
      - POSTGRES_USER=pasa_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/ssl:ro
    depends_on:
      - app
    restart: unless-stopped

volumes:
  postgres_data:
```

#### Deploy with Docker Compose
```bash
# Generate JWT keys
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.pem -out keys/public.pem

# Set environment variables
export DB_PASSWORD=your_secure_password
export APP_SECURITY_SALT=your_random_salt_string

# Deploy
docker-compose up -d

# Check logs
docker-compose logs -f app

# Scale if needed
docker-compose up -d --scale app=2
```

### 2. Kubernetes Deployment

#### Namespace and ConfigMap
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: pasa-auto

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: pasa-auto-config
  namespace: pasa-auto
data:
  DB_URL: "jdbc:postgresql://postgres:5432/pasa_auto"
  DB_USERNAME: "pasa_user"
  SERVER_URL: "https://pasa-auto.example.com"
  RBAC_ENABLED: "true"
```

#### Secret
```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: pasa-auto-secrets
  namespace: pasa-auto
type: Opaque
data:
  DB_PASSWORD: <base64-encoded-password>
  APP_SECURITY_SALT: <base64-encoded-salt>
  JWT_PRIVATE_KEY: <base64-encoded-private-key>
  JWT_PUBLIC_KEY: <base64-encoded-public-key>
```

#### Deployment
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pasa-auto
  namespace: pasa-auto
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pasa-auto
  template:
    metadata:
      labels:
        app: pasa-auto
    spec:
      containers:
      - name: pasa-auto
        image: pasa-auto:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: pasa-auto-config
        - secretRef:
            name: pasa-auto-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /q/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### Service and Ingress
```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: pasa-auto-service
  namespace: pasa-auto
spec:
  selector:
    app: pasa-auto
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP

---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pasa-auto-ingress
  namespace: pasa-auto
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - pasa-auto.example.com
    secretName: pasa-auto-tls
  rules:
  - host: pasa-auto.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: pasa-auto-service
            port:
              number: 80
```

### 3. Traditional Deployment

#### Build and Run
```bash
# Build application
./mvnw clean package -DskipTests

# Run with JVM
java -jar target/quarkus-app/quarkus-run.jar

# Or run native executable
./mvnw package -Dnative -DskipTests
./target/pasa-auto-1.0.0-SNAPSHOT-runner
```

#### Systemd Service
Create `/etc/systemd/system/pasa-auto.service`:

```ini
[Unit]
Description=Pasa Auto Application
After=network.target

[Service]
Type=simple
User=pasa-auto
Group=pasa-auto
WorkingDirectory=/opt/pasa-auto
ExecStart=/opt/pasa-auto/target/pasa-auto-1.0.0-SNAPSHOT-runner
Restart=always
RestartSec=10
EnvironmentFile=/opt/pasa-auto/.env

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/pasa-auto

[Install]
WantedBy=multi-user.target
```

Enable and start service:
```bash
sudo systemctl enable pasa-auto
sudo systemctl start pasa-auto
sudo systemctl status pasa-auto
```

## Database Setup

### PostgreSQL Installation

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### CentOS/RHEL
```bash
sudo yum install postgresql-server postgresql-contrib
sudo postgresql-setup initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Database Creation
```bash
# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE pasa_auto;
CREATE USER pasa_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE pasa_auto TO pasa_user;
\q
```

### Migration
Database migrations run automatically on application startup. To run manually:

```bash
# Using Maven
./mvnw flyway:migrate

# Using Docker
docker run --rm \
  -e DB_URL=jdbc:postgresql://host:5432/pasa_auto \
  -e DB_USERNAME=pasa_user \
  -e DB_PASSWORD=your_password \
  pasa-auto:latest \
  java -jar quarkus-run.jar flyway:migrate
```

## SSL/TLS Configuration

### Let's Encrypt Certificate
```bash
# Install certbot
sudo apt install certbot

# Generate certificate
sudo certbot certonly --standalone -d your-domain.com

# Certificate paths
CERT_FILE_PATH=/etc/letsencrypt/live/your-domain.com/fullchain.pem
KEY_FILE_PATH=/etc/letsencrypt/live/your-domain.com/privkey.pem
```

### Self-Signed Certificate (Development)
```bash
# Generate private key
openssl genrsa -out private.key 2048

# Generate certificate
openssl req -new -x509 -key private.key -out certificate.crt -days 365
```

## Monitoring and Logging

### Application Logging
Configure logging in `application.properties`:

```properties
# File logging
quarkus.log.file.enable=true
quarkus.log.file.path=/var/log/pasa-auto/app.log
quarkus.log.file.rotation.max-file-size=10M
quarkus.log.file.rotation.max-backup-index=30

# Log levels
quarkus.log.level=INFO
quarkus.log.category."com.github.b3kt".level=DEBUG
```

### Health Checks
The application exposes health endpoints:
- `/q/health` - Basic health check
- `/q/health/ready` - Readiness probe
- `/q/health/live` - Liveness probe

### Metrics
Enable metrics in `application.properties`:

```properties
quarkus.smallrye-metrics.enabled=true
quarkus.smallrye-health.enabled=true
```

Access metrics at `/q/metrics`

## Backup and Recovery

### Database Backup
```bash
# Create backup
pg_dump -h localhost -U pasa_user -d pasa_auto > backup_$(date +%Y%m%d_%H%M%S).sql

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backup/pasa-auto"
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h localhost -U pasa_user -d pasa_auto | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# Keep last 7 days
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

### Application Backup
```bash
# Backup application and configuration
tar -czf pasa-auto-backup-$(date +%Y%m%d).tar.gz \
  target/pasa-auto-1.0.0-SNAPSHOT-runner \
  .env \
  keys/ \
  nginx.conf
```

## Performance Tuning

### JVM Optimization
```bash
# Production JVM settings
JAVA_OPTS="-Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"
```

### Database Optimization
```sql
-- PostgreSQL configuration
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
SELECT pg_reload_conf();
```

## Security Hardening

### Firewall Configuration
```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# iptables rules
sudo iptables -A INPUT -p tcp --dport 8080 -s 10.0.0.0/8 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### Application Security
- Disable Swagger/OpenAPI in production
- Use strong passwords and salts
- Enable rate limiting
- Regular security updates
- Monitor access logs

## Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check logs
docker-compose logs app
journalctl -u pasa-auto -f

# Check environment variables
docker-compose exec app env | grep -E "DB_|JWT_"

# Test database connection
docker-compose exec postgres psql -U pasa_user -d pasa_auto -c "SELECT 1;"
```

#### Database Connection Issues
```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Test connection
psql -h localhost -U pasa_user -d pasa_auto

# Check network connectivity
telnet postgres-host 5432
```

#### Memory Issues
```bash
# Check memory usage
docker stats
free -h

# Monitor JVM
jstat -gc <pid>
jmap -histo <pid>
```

### Performance Issues
```bash
# Database performance
SELECT * FROM pg_stat_activity WHERE state = 'active';
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;

# Application performance
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/q/health
```

## Rollback Procedures

### Docker Rollback
```bash
# List previous images
docker images pasa-auto

# Rollback to previous version
docker-compose down
docker-compose up -d --image pasa-auto:previous-version
```

### Database Rollback
```bash
# Check migration history
./mvnw flyway:info

# Rollback to specific version
./mvnw flyway:undo -Dflyway.targetVersion=1.0
```

## Support

For deployment issues:
1. Check logs and health endpoints
2. Review this troubleshooting guide
3. Create GitHub issue with `deployment` label
4. Contact support at deploy@pasa-auto.com
