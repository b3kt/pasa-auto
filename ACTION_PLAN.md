# ACTION_PLAN.md

Implementation plan based on ACTION_RULES.md to improve the pasa-auto project.

## Phase 1: Security & Sensitive Data (Priority: HIGH)

### 1.1 Immediate Security Issues Found
- **Hardcoded salt**: `app.security.salt=1234567890` in application.properties
- **Certificate paths**: Default SSL certificate paths exposed
- **JWT keys**: Using classpath keys instead of environment variables

### 1.2 Required Changes
1. Replace hardcoded salt with environment variable
2. Create .env.example template
3. Add secret scanning to CI
4. Move JWT keys to secure locations

## Phase 2: Documentation Structure (Priority: HIGH)

### 2.1 Missing Required Documents
- CHANGELOG.md (project level)
- Updated README.md with project-specific content
- ADRs in /docs directory

### 2.2 Required Changes
1. Create CHANGELOG.md with initial structure
2. Update README.md from generic Quarkus template
3. Add project documentation to /docs

## Phase 3: Testing & Coverage (Priority: MEDIUM)

### 3.1 Current State
- Test directory exists but minimal content
- No coverage configuration
- Missing integration tests

### 3.2 Required Changes
1. Add JaCoCo plugin for coverage reporting
2. Set coverage thresholds (≥80% unit tests)
3. Add Testcontainers for integration tests
4. Create test coverage CI check

## Phase 4: Code Cleanup (Priority: MEDIUM)

### 4.1 Analysis Required
- Scan for unused/dead code
- Identify TODO items for cleanup
- Review for over-engineered patterns

### 4.2 Required Changes
1. Mark unused code with TODO(cleanup)
2. Consolidate duplicate logic
3. Simplify complex flows

## Phase 5: CI/CD Integration (Priority: MEDIUM)

### 5.1 Missing CI Components
- Secret scanning
- Coverage enforcement
- Documentation validation
- AI-assisted checks

### 5.2 Required Changes
1. Update GitHub Actions workflows
2. Add quality gates
3. Implement automated checks

## Execution Order

1. **Phase 1**: Security fixes (immediate)
2. **Phase 2**: Documentation setup
3. **Phase 3**: Testing infrastructure
4. **Phase 4**: Code cleanup
5. **Phase 5**: CI/CD integration

## Success Criteria

- No hardcoded secrets
- Complete documentation
- ≥80% test coverage
- Clean, maintainable code
- Automated quality gates

## Timeline Estimate

- Phase 1: 1-2 days
- Phase 2: 1 day  
- Phase 3: 2-3 days
- Phase 4: 2-3 days
- Phase 5: 1-2 days

**Total: 7-11 days**
