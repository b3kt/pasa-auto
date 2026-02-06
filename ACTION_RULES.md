# ACTION_RULES.md

This document defines **actionable rules and an implementation plan** to clean up the current GitHub project and establish a **continuous AI-assisted flow** for code quality, security, testing, and documentation.

---

## 1. Goals

* Eliminate leaking sensitive data from source code and configuration.
* Identify and remove unused files, dead code, and redundant logic.
* Achieve reliable code coverage with unit and integration tests.
* Enforce consistent documentation and changelog updates for every change.
* Enable continuous improvement using AI in local development and CI.

---

## 2. Security & Sensitive Data Handling

### 2.1 Detection Rules

* Scan **all files** for hardcoded secrets:

    * API keys, tokens, passwords, private keys
    * Database URLs with credentials
    * Cloud provider credentials
* Target locations:

    * `*.yml`, `*.yaml`, `*.properties`, `*.env`
    * Source code constants
    * Test fixtures and mocks

### 2.2 Remediation Rules

* Remove secrets immediately from code and commit history.
* Replace with:

    * Environment variables
    * Secret managers (Vault, AWS Secrets Manager, GCP Secret Manager)
* Add `.env.example` with **safe placeholders only**.

### 2.3 Enforcement

* Add secret scanning to CI:

    * GitHub Advanced Security / TruffleHog / Gitleaks
* CI must fail if a secret is detected.

---

## 3. Code Cleanup & Simplification

### 3.1 TODO Marking Rules

* Any file or code block that is:

    * Unused
    * Deprecated
    * Redundant
    * Over-engineered

Must be explicitly marked:

```java
// TODO(cleanup): Unused since v1.2. Remove after validation.
```

```kotlin
// TODO(cleanup): Redundant logic. Simplify flow using single responsibility.
```

### 3.2 File-Level Rules

* Unused files:

    * Mark the **entire file** with a TODO at the top.
* Duplicate logic:

    * Consolidate into shared utilities or services.
* Complex flows:

    * Refactor into smaller functions with clear boundaries.

### 3.3 Removal Policy

* TODO-marked items must be:

    * Removed or refactored within **2 release cycles**, or
    * Justified in documentation.

---

## 4. Testing Strategy & Coverage

### 4.1 Coverage Requirements

* Minimum coverage thresholds:

    * Unit tests: **≥ 80%**
    * Integration tests: **Critical paths only**

### 4.2 Unit Testing Rules

* Cover:

    * Business logic
    * Edge cases
    * Error handling
* Mock:

    * External services
    * Network and IO

### 4.3 Integration Testing Rules

* Validate:

    * Service-to-service interactions
    * Database and messaging integration
* Use:

    * Testcontainers or embedded services
* Tests must be deterministic and CI-safe.

### 4.4 Enforcement

* CI fails if coverage drops below threshold.
* Coverage reports must be published per build.

---

## 5. Documentation & Change Logs

### 5.1 Documentation Structure

Required documents:

* `README.md`
* `ACTION_RULES.md`
* `CHANGELOG.md`
* `/docs` directory for detailed design and ADRs

### 5.2 Change Log Rules

* Every change must update `CHANGELOG.md`:

```md
## [Unreleased]
### Changed
- Refactored authentication flow to remove redundant token parsing.

### Fixed
- Removed hardcoded database credentials.
```

* Entries must reference:

    * What changed
    * Why it changed
    * Impact (if any)

### 5.3 Commit Discipline

* Commits must be atomic.
* Commit messages must reference changelog entries.

---

## 6. Continuous AI-Assisted Flow

### 6.1 Local Development

AI is used to:

* Detect code smells and unused logic
* Suggest refactoring and simplification
* Generate missing unit and integration tests
* Review documentation consistency

### 6.2 CI Integration

AI-assisted checks in CI:

1. Secret scan
2. Dead code detection
3. Test coverage analysis
4. Documentation & changelog validation

CI fails if any rule is violated.

---

## 7. Definition of Done

A change is considered **done** only if:

* No sensitive data is leaked
* No new TODO(cleanup) without justification
* Tests are added or updated
* Coverage thresholds are met
* Documentation and changelog are updated

---

## 8. Review & Evolution

* Rules are reviewed every quarter.
* Improvements must be proposed via PR.
* AI prompts and checks should evolve with the codebase.

---

**This document is mandatory and enforceable.**
