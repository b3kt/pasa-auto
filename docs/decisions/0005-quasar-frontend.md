# ADR-0005: Use Quasar/Vue.js for Frontend

## Status
**Accepted** - 2024-02-06

## Context
The application needs a modern frontend framework that provides rich UI components, mobile support, and integrates well with the Quarkus backend.

### Options Considered
- **React + Material UI**: Popular but larger bundle size
- **Angular**: Comprehensive but heavier learning curve
- **Quasar/Vue.js**: Rich component library, PWA support, good dev experience

## Decision
Use Quasar Framework with Vue.js for the frontend, integrated via Quarkus Quinoa.

### Rationale
- Quasar provides a comprehensive component library out of the box
- Vue.js has a gentle learning curve and good developer experience
- Quinoa enables seamless integration between Quarkus and the frontend
- Built-in PWA support for mobile deployments
- Hot reload during development

## Consequences
- **Positive**: Fast development, rich UI components, good mobile support
- **Negative**: Smaller ecosystem than React
- **Trade-offs**: Quasar opinionated structure limits some customization options
