# Database Migration Rollback Procedures

> **Version**: 1.0
> **Last Updated**: 2026-08-16
> **Applies to**: Pasa Auto v0.0.34+

---

## Overview

This document describes the rollback procedures for Flyway database migrations in the Pasa Auto application. Flyway does not support automatic rollbacks, so all rollbacks must be performed manually using compensating migrations or backup/restore procedures.

---

## Migration History

| Version | Description | Rollback Strategy |
|---------|-------------|-------------------|
| V1 | Initial schema | Full restore from backup |
| V2 | Initial data | Delete inserted data |
| V3 | Fix empty version | N/A (data fix) |
| V4 | Add new fields | Drop columns |
| V5 | Fix supplier sequence | Reset sequence |
| V6 | Add index pelanggan | Drop index |
| V7 | Add base entity fields | Drop columns |
| V8 | Fix SPK detail nama_jasa length | Alter column |
| V9 | Create audit trail | Drop triggers/tables |
| V10 | Audit trail readonly | Drop view |
| V11 | Create audit trigger function | Drop function |
| V12 | Add audit triggers | Drop triggers |
| V13 | Add discount to tb_penjualan | Drop column |
| V14 | Baseline marker | N/A (marker only) |

---

## Rollback Methods

### Method 1: Compensating Migration (Preferred)

Create a new migration that reverses the changes:

```sql
-- V15__rollback_add_discount_to_tb_penjualan.sql
-- Rollback for V13

ALTER TABLE tb_penjualan DROP COLUMN IF EXISTS discount;
```

**Naming Convention**: `V{next_version}__rollback_{original_description}.sql`

### Method 2: Backup/Restore (For Major Changes)

For irreversible changes (table drops, data transformations):

1. **Pre-deployment backup**:
   ```bash
   pg_dump -h localhost -U postgres -d pasa_auto -Fc -f pasa_auto_pre_v13_$(date +%Y%m%d).dump
   ```

2. **Rollback**:
   ```bash
   pg_restore -h localhost -U postgres -d pasa_auto --clean --if-exists pasa_auto_pre_v13_20260816.dump
   ```

3. **Reset Flyway** (if needed):
   ```bash
   flyway repair
   ```

### Method 3: Blue-Green Deployment (Zero Downtime)

1. Deploy new version to green environment
2. Run migrations on green
3. Validate green
4. Switch traffic
4. If issues: switch back to blue, fix, retry

---

## Specific Rollback Procedures

### V13: Add Discount to tb_penjualan
```sql
-- V15__rollback_v13_discount.sql
ALTER TABLE tb_penjualan DROP COLUMN IF EXISTS discount;
```

### V12: Add Audit Triggers
```sql
-- V15__rollback_v12_audit_triggers.sql
DROP TRIGGER IF EXISTS audit_trigger_tb_pelanggan ON tb_pelanggan;
DROP TRIGGER IF EXISTS audit_trigger_tb_kendaraan ON tb_kendaraan;
DROP TRIGGER IF EXISTS audit_trigger_tb_karyawan ON tb_karyawan;
-- ... repeat for all tables with audit triggers
DROP TRIGGER IF EXISTS audit_trigger_users ON users;
```

### V11: Create Audit Trigger Function
```sql
-- V15__rollback_v11_audit_function.sql
DROP FUNCTION IF EXISTS audit_trigger_function() CASCADE;
```

### V10: Audit Trail Readonly View
```sql
-- V15__rollback_v10_audit_view.sql
DROP VIEW IF EXISTS v_audit_trail_readonly;
```

### V9: Create Audit Trail
```sql
-- V15__rollback_v9_audit_trail.sql
DROP TABLE IF EXISTS tb_audit_trail;
```

### V8: Fix SPK Detail nama_jasa Length
```sql
-- V15__rollback_v8_spk_detail_length.sql
ALTER TABLE tb_spk_detail ALTER COLUMN nama_jasa TYPE VARCHAR(100);
-- Note: Verify data fits before running
```

### V7: Add Base Entity Fields
```sql
-- V15__rollback_v7_base_entity.sql
ALTER TABLE tb_pelanggan DROP COLUMN IF EXISTS created_at, DROP COLUMN IF EXISTS created_by, 
    DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS version;
-- Repeat for all tables that got base entity fields
```

### V5: Fix Supplier Sequence
```sql
-- V15__rollback_v5_supplier_sequence.sql
ALTER SEQUENCE tb_supplier_id_seq RESTART WITH 1;
-- Or restore previous sequence state
```

### V4: Add New Fields
```sql
-- V15__rollback_v4_new_fields.sql
-- Identify which columns were added in V4 and drop them
-- Example:
ALTER TABLE tb_pelanggan DROP COLUMN IF EXISTS new_field1;
ALTER TABLE tb_kendaraan DROP COLUMN IF EXISTS new_field2;
```

---

## Rollback Decision Matrix

| Scenario | Recommended Method |
|----------|-------------------|
| Column addition (nullable) | Compensating migration |
| Column addition (NOT NULL) | Compensating + data migration |
| Column drop | Backup/restore (data loss) |
| Data transformation | Compensating migration |
| Table drop | Backup/restore |
| Index change | Compensating migration |
| Trigger/function | Compensating migration |
| Major schema restructure | Blue-green + backup |

---

## Pre-Deployment Checklist

Before deploying any migration:

- [ ] Create compensating migration file (even if not immediately needed)
- [ ] Test migration + rollback in staging
- [ ] Verify backup exists and is recent (< 1 hour)
- [ ] Document rollback steps in deployment runbook
- [ ] Notify team of deployment window
- [ ] Ensure monitoring alerts are configured

---

## Post-Deployment Verification

After migration:

- [ ] Verify application health: `GET /health/ready`
- [ ] Run smoke tests on critical paths
- [ ] Check Flyway history: `flyway info`
- [ ] Monitor error rates for 15 minutes
- [ ] Document any issues in deployment log

---

## Emergency Rollback Procedure

**Time to rollback**: < 15 minutes

1. **Alert team**: "Emergency rollback initiated for migration V{version}"
2. **Stop traffic** to affected service (if using load balancer)
3. **Execute rollback**:
   ```bash
   # Option A: Compensating migration (preferred)
   flyway migrate -target={rollback_version}
   
   # Option B: Restore from backup
   pg_restore -h $DB_HOST -U $DB_USER -d $DB_NAME --clean --if-exists $BACKUP_FILE
   
   # Option C: Roll back the application binary
   sudo systemctl stop pasa-auto
   sudo cp /opt/pasa-auto/pasa-auto.backup /opt/pasa-auto/pasa-auto
   sudo systemctl start pasa-auto
   ```
4. **Verify rollback**: Check health endpoints and run smoke tests
5. **Communicate**: "Rollback complete. Investigating root cause."
6. **Document**: Record in incident log with timeline

---

## Flyway Commands Reference

```bash
# Check status
flyway info

# Migrate to specific version
flyway migrate -target=14

# Repair checksums (after manual DB changes)
flyway repair

# Baseline existing database
flyway baseline -baselineVersion=14

# Validate migrations
flyway validate

# Show migration history
flyway history
```

---

## Backup Strategy

| Frequency | Retention | Storage |
|-----------|-----------|---------|
| Before each deployment | 30 days | S3/MinIO |
| Daily (automated) | 7 days | S3/MinIO |
| Weekly | 90 days | S3/MinIO |
| Monthly | 1 year | S3/MinIO |

**Backup verification**: Monthly restore test to staging environment

---

## Contacts

| Role | Contact |
|------|---------|
| Database Admin | [TBD] |
| DevOps Lead | [TBD] |
| Application Owner | [TBD] |

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-16 | AI Assistant | Initial version |

---

*This document should be reviewed and updated after each major migration or schema change.*