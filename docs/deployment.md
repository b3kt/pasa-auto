# Deployment Guide

## Overview

Pasa Auto supports two deployment modes:

| Mode | Description |
|------|-------------|
| **Web App** | Quarkus serves Quasar SPA via Quinoa |
| **Electron Desktop** | Quarkus native binary wrapped in Electron |

---

## Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| CPU | 2 cores | 4 cores |
| Memory | 2 GB RAM | 4 GB RAM |
| Storage | 10 GB | 20 GB |
| PostgreSQL | 15 | 15+ |

---

## Environment Configuration

Copy and configure environment variables:

```bash
cp .env.example .env
```

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/pasa_auto` |
| `DB_USERNAME` | Database user | `pasa_user` |
| `DB_PASSWORD` | Database password | `(secure)` |
| `APP_SECURITY_SALT` | Password hashing salt | `(random 16+ chars)` |
| `JWT_PRIVATE_KEY` | Path to RSA private key | `/app/keys/private.pem` |
| `JWT_PUBLIC_KEY` | Path to RSA public key | `/app/keys/public.pem` |
| `JWT_ISSUER` | Token issuer URL | `https://your-domain.com` |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `HTTP_PORT` | 8080 | HTTP port |
| `HTTPS_PORT` | 443 | HTTPS port |
| `DB_MAX_SIZE` | 10 | Max DB connections |
| `DB_MIN_SIZE` | 2 | Min DB connections |
| `SWAGGER_ENABLED` | false | Enable Swagger UI |
| `RBAC_ENABLED` | false | Enable RBAC module |
| `LOG_PATH` | `./app.log` | Log file path |

---

## Deployment Options

### Option 1: Docker (Recommended)

#### 1. Build Image

```bash
# JVM mode
docker build -f src/main/docker/Dockerfile.jvm -t pasa-auto:latest .

# Native mode (faster startup, smaller image)
docker build -f src/main/docker/Dockerfile.native -t pasa-auto:native .
```

#### 2. Docker Compose

Create `docker-compose.yml`:

```yaml
services:
  app:
    image: pasa-auto:latest
    ports:
      - "8080:8080"
    env_file: .env
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/pasa_auto
    volumes:
      - ./keys:/app/keys:ro
      - ./logs:/var/log/pasa-auto
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=pasa_auto
      - POSTGRES_USER=${DB_USERNAME}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

#### 3. Deploy

```bash
# Generate JWT keys
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.pem -out keys/public.pem

# Set environment
export DB_USERNAME=pasa_user
export DB_PASSWORD=your_secure_password
export APP_SECURITY_SALT=$(openssl rand -hex 16)

# Deploy
docker compose up -d

# View logs
docker compose logs -f app
```

---

### Option 2: Kubernetes

#### 1. Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: pasa-auto-secrets
type: Opaque
stringData:
  DB_PASSWORD: "your_password"
  APP_SECURITY_SALT: "your_salt"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: pasa-auto-config
data:
  DB_URL: "jdbc:postgresql://postgres:5432/pasa_auto"
  DB_USERNAME: "pasa_user"
  JWT_PRIVATE_KEY: "/app/keys/private.pem"
  JWT_PUBLIC_KEY: "/app/keys/public.pem"
  JWT_ISSUER: "https://your-domain.com"
```

#### 2. Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pasa-auto
spec:
  replicas: 2
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
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

#### 3. Service + Ingress

```yaml
apiVersion: v1
kind: Service
metadata:
  name: pasa-auto
spec:
  selector:
    app: pasa-auto
  ports:
  - port: 80
    targetPort: 8080
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pasa-auto
  annotations:
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
            name: pasa-auto
            port:
              number: 80
```

---

### Option 3: Traditional (Systemd)

#### 1. Build

```bash
# Native binary (recommended for production)
./mvnw clean package -Pnative -DskipTests

# Or JVM mode
./mvnw clean package -DskipTests
```

#### 2. Install

```bash
sudo useradd --system --no-create-home pasa-auto
sudo mkdir -p /opt/pasa-auto /var/log/pasa-auto
sudo cp target/*-runner /opt/pasa-auto/pasa-auto
sudo chown -R pasa-auto:pasa-auto /opt/pasa-auto /var/log/pasa-auto
```

#### 3. Systemd Service

Create `/etc/systemd/system/pasa-auto.service`:

```ini
[Unit]
Description=Pasa Auto Application
After=network.target postgresql.service

[Service]
Type=simple
User=pasa-auto
Group=pasa-auto
WorkingDirectory=/opt/pasa-auto
ExecStart=/opt/pasa-auto/pasa-auto
Restart=always
RestartSec=10
EnvironmentFile=/opt/pasa-auto/.env
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

#### 4. Enable and Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable pasa-auto
sudo systemctl start pasa-auto
sudo systemctl status pasa-auto
```

---

### Option 4: Electron Desktop

Built automatically via CI/CD on `release` branch:

1. Quarkus native binary built for Windows
2. Binary bundled into Electron app
3. Windows installer generated

See `.github/workflows/quarkus-native-build.yml` for details.

---

## Database Setup

### Migrations

Flyway migrations run automatically on startup:

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_audit_trail.sql
├── ...
└── V13__latest_change.sql
```

To check migration status:
```bash
./mvnw flyway:info
```

### Manual Database Creation

```bash
sudo -u postgres psql

CREATE DATABASE pasa_auto;
CREATE USER pasa_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE pasa_auto TO pasa_user;
\q
```

---

## SSL/TLS

### Let's Encrypt

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d your-domain.com

# Update .env
CERT_FILE_PATH=/etc/letsencrypt/live/your-domain.com/fullchain.pem
KEY_FILE_PATH=/etc/letsencrypt/live/your-domain.com/privkey.pem
```

---

## Monitoring

### Health Checks

| Endpoint | Purpose |
|----------|---------|
| `/health` | Overall health status |

### Logging

Logs are written to:
- Console (stdout)
- File: `/var/log/pasa-auto/app.log` (configurable via `LOG_PATH`)

Log rotation: 10 MB max per file, 30 backups retained.

### OpenTelemetry

OpenTelemetry is enabled by default for tracing and log injection:

```properties
quarkus.otel.enabled=true
quarkus.otel.traces.enabled=true
quarkus.otel.logs.enabled=true
quarkus.otel.exporter.otlp.enabled=false
```

Set `quarkus.otel.exporter.otlp.enabled=true` and configure exporter endpoint for production tracing.

---

## Backup

### Database

```bash
# Backup
pg_dump -h localhost -U pasa_user pasa_auto > backup_$(date +%Y%m%d).sql

# Restore
psql -h localhost -U pasa_user pasa_auto < backup_20260101.sql
```

### Automated Backup Script

```bash
#!/bin/bash
BACKUP_DIR="/backup/pasa-auto"
mkdir -p $BACKUP_DIR
pg_dump -h localhost -U pasa_user pasa_auto | gzip > $BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).sql.gz
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
journalctl -u pasa-auto -f
# or
docker compose logs app

# Verify environment
docker compose exec app env | grep -E "DB_|JWT_"

# Test DB connection
docker compose exec postgres pg_isready
```

### Database Issues

```bash
# Check PostgreSQL
sudo systemctl status postgresql

# Test connection
psql -h localhost -U pasa_user -d pasa_auto -c "SELECT 1;"
```

### Memory Issues

```bash
# Check usage
free -h
docker stats

# JVM heap (if using JVM mode)
java -Xms512m -Xmx1g -jar quarkus-run.jar
```

---

## Rollback

### Docker

```bash
docker compose down
docker tag pasa-auto:previous pasa-auto:latest
docker compose up -d
```

### Systemd

```bash
sudo systemctl stop pasa-auto
sudo cp /opt/pasa-auto/pasa-auto.backup /opt/pasa-auto/pasa-auto
sudo systemctl start pasa-auto
```

### Database

```bash
# Check migration history
./mvnw flyway:info

# Restore from backup if needed
psql -h localhost -U pasa_user pasa_auto < backup.sql
```
