# ADR-0007: Role-Based Access Control (RBAC)

## Status
**Accepted** - 2024-02-06

## Context
The workshop management system requires fine-grained access control to protect sensitive data and operations based on user roles and permissions.

### Options Considered
- **Simple role check**: Hardcoded role checks in code (inflexible)
- **RBAC**: Role-permission mapping with database-driven configuration
- **ABAC**: Attribute-based, more flexible but more complex

## Decision
Implement RBAC with the following model:
- **Users** have one or more **Roles**
- **Roles** have multiple **Permissions**
- Permissions control access to specific resources and operations
- RBAC module can be enabled/disabled via feature flag (`app.rbac.enabled`)

### Rationale
- Clear mapping between job functions and system access
- Database-driven configuration allows runtime changes
- Feature flag allows gradual rollout
- Simpler to understand and maintain than ABAC
- Scales well with the expected number of roles and permissions

## Consequences
- **Positive**: Clear access model, flexible, configurable at runtime
- **Negative**: Requires database tables and management UI for roles/permissions
- **Trade-offs**: Less granular than ABAC but simpler to implement and maintain
