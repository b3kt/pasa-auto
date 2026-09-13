# ADR-0003: Use PostgreSQL as Primary Database

## Status
**Accepted** - 2024-02-06

## Context
The application requires a reliable relational database for managing workshop operations, customers, vehicles, service orders, inventory, and financial records.

### Options Considered
- **PostgreSQL**: Open-source, ACID compliant, advanced features
- **MySQL**: Popular but fewer advanced features
- **H2**: Good for testing but not production-ready for this use case

## Decision
Use PostgreSQL 15+ as the primary database with Hibernate ORM for data access.

### Rationale
- Full ACID compliance for financial transactions
- Advanced features: JSONB, full-text search, window functions
- Excellent Hibernate ORM support
- Strong community and long-term support
- Free and open-source
- Good performance under load with proper indexing

## Consequences
- **Positive**: Reliable, feature-rich, well-supported, cost-effective
- **Negative**: Requires separate database server vs embedded options
- **Trade-offs**: More operational overhead than managed databases (e.g., AWS RDS)
