# ADR-0004: JWT-based Stateless Authentication

## Status
**Accepted** - 2024-02-06

## Context
The application needs a secure, scalable authentication mechanism that works well with REST APIs and supports stateless horizontal scaling.

### Options Considered
- **Session-based**: Traditional but requires server-side state
- **JWT**: Stateless, scalable, widely adopted for APIs
- **OAuth2/OIDC**: More complex, better for third-party integrations

## Decision
Use JWT (JSON Web Tokens) with SmallRye JWT for authentication.

### Implementation Details
- RS256 asymmetric signing (private/public key pair)
- Token expiration with refresh mechanism
- Keys configured via environment variables
- Integration with Quarkus security subsystem

### Rationale
- Stateless design enables horizontal scaling
- No server-side session storage required
- Standard format with wide library support
- Asymmetric keys allow verification without exposing signing key
- Works well with microservices architecture if needed

## Consequences
- **Positive**: Scalable, standard, works well with REST APIs
- **Negative**: Tokens cannot be easily revoked before expiration
- **Trade-offs**: Token size larger than opaque tokens; need to manage key rotation
