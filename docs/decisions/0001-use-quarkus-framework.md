# ADR-0001: Use Quarkus as Backend Framework

## Status
**Accepted** - 2024-02-06

## Context
The project needed a modern Java framework for building a high-performance backend API for the automotive workshop management system.

### Options Considered
- **Spring Boot**: Industry standard but heavier footprint
- **Micronaut**: Similar to Quarkus but less mature ecosystem
- **Quarkus**: Fast startup, low memory, native compilation support

## Decision
Use Quarkus as the backend framework with Java 25.

### Rationale
- Fast startup time and low memory footprint suitable for containerized deployments
- Native image compilation support for even faster cold starts
- First-class support for modern Java features
- Excellent integration with Hibernate ORM, REST, and security
- Developer-friendly live reload during development

## Consequences
- **Positive**: Fast builds, efficient resource usage, good developer experience
- **Negative**: Smaller community than Spring Boot, fewer third-party integrations
- **Trade-offs**: Some enterprise features require manual configuration vs Spring's auto-configuration
