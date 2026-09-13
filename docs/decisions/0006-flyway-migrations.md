# ADR-0006: Use Flyway for Database Migrations

## Status
**Accepted** - 2024-02-06

## Context
The application needs a reliable way to manage database schema changes across environments (development, staging, production) with version control and rollback capabilities.

### Options Considered
- **Flyway**: SQL-based migrations, simple and reliable
- **Liquibase**: XML/YAML/SQL formats, more complex but more features
- **Manual scripts**: Full control but error-prone

## Decision
Use Flyway with SQL-based migrations stored in `classpath:db/migration`.

### Rationale
- Simple SQL-based approach is easy to review and version control
- Automatic migration tracking and execution
- Validates migrations against expected state
- Good Quarkus integration
- Low overhead compared to Liquibase's complexity

## Consequences
- **Positive**: Simple, reliable, easy to review in code reviews
- **Negative**: Manual rollback scripts needed (no automatic rollback)
- **Trade-offs**: Less metadata-driven than Liquibase; requires careful ordering of migration files
