# Global Rules for AI Development

## Testing
- Do NOT automatically run tests. Only inform the user when tests need to be run manually.
- When tests need to be executed, inform the user: "Please run the tests manually with: ./mvnw test"

## Code Quality
- Run lint/typecheck when explicitly requested by the user
- Do not run automatically unless asked

## Commits
- Do NOT commit changes unless explicitly requested by the user
- Ask for confirmation before creating commits