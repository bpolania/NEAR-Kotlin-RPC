# Contributing to NEAR Kotlin RPC Client

Thank you for your interest in contributing to the NEAR Kotlin RPC Client! This document provides guidelines and information for contributors.

## Getting Started

### Prerequisites

- JDK 11 or higher
- Kotlin 1.9.22 or higher
- Python 3.10+ (for code generation scripts)
- Git

### Development Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/yourusername/near-kotlin-rpc.git
   cd near-kotlin-rpc
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

## Project Structure

```
near-kotlin-rpc/
├── near-jsonrpc-types/       # Type definitions only
├── near-jsonrpc-client/      # RPC client implementation
├── example/                  # Usage examples and integration tests
├── scripts/                  # Code generation scripts
│   └── generate-from-openapi.sh
└── .github/workflows/        # CI/CD automation
```

## Development Workflow

### Making Changes

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes following the coding standards

3. Add tests for new functionality

4. Ensure all tests pass:
   ```bash
   ./gradlew testAll
   ```

5. Check code coverage:
   ```bash
   ./gradlew coverage
   open build/reports/jacoco/aggregate/html/index.html
   ```

### Code Generation

The project uses automated code generation from NEAR's OpenAPI specification.

To regenerate code:
```bash
./scripts/generate-from-openapi.sh
```

Or using Gradle:
```bash
./gradlew fetchOpenApiSpec generateNearRpcFromOpenApi
```

**Important:** Do not manually edit generated code. Instead:
1. Modify the KotlinPoet generator if needed
2. Update the generator in `generator/` module
3. Regenerate the code

### Testing

#### Run all tests:
```bash
./gradlew testAll
```

#### Run only unit tests (no network):
```bash
./gradlew testUnit
```

#### Run only integration tests:
```bash
./gradlew testIntegration
```

#### Skip integration tests:
```bash
./gradlew test -DskipIntegrationTests=true
```

#### Use custom testnet:
```bash
NEAR_TESTNET_RPC_URL=https://custom.testnet.rpc ./gradlew test
```

## Coding Standards

### Kotlin Style Guide

We follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use camelCase for functions and properties
- Use PascalCase for classes and interfaces
- Use UPPER_SNAKE_CASE for constants

### Specific Guidelines

1. **Type Safety**: Always prefer type-safe approaches
2. **Nullability**: Be explicit about nullable types
3. **Coroutines**: Use suspend functions for async operations
4. **Serialization**: Use kotlinx.serialization annotations
5. **Documentation**: Add KDoc comments for public APIs

### Example:
```kotlin
/**
 * Fetches account information from the NEAR network.
 * 
 * @param accountId The NEAR account identifier
 * @param blockReference Optional block reference for historical queries
 * @return Account details including balance and storage
 * @throws RpcException if the account doesn't exist or RPC fails
 */
suspend fun viewAccount(
    accountId: String,
    blockReference: BlockReference = BlockReference(finality = Finality.FINAL)
): AccountView {
    // Implementation
}
```

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test additions or changes
- `chore:` Maintenance tasks
- `refactor:` Code refactoring
- `perf:` Performance improvements

Examples:
```
feat: add support for EXPERIMENTAL_changes RPC method
fix: handle null values in block header deserialization
docs: improve README examples for account queries
test: add integration tests for validator operations
```

## Pull Request Process

1. **Before submitting:**
   - Ensure all tests pass
   - Update documentation if needed
   - Add/update tests for new functionality
   - Run code formatter

2. **PR Title:**
   - Use conventional commit format
   - Be descriptive but concise

3. **PR Description:**
   - Describe what changes were made
   - Explain why the changes are needed
   - List any breaking changes
   - Reference related issues

4. **Review Process:**
   - PRs require at least one review
   - Address review feedback promptly
   - Keep PRs focused and reasonably sized

## Reporting Issues

### Bug Reports

Include:
- Kotlin version
- Library version
- Minimal code to reproduce
- Expected vs actual behavior
- Stack trace if applicable

### Feature Requests

Include:
- Use case description
- Proposed API design
- Alternative solutions considered

## OpenAPI Specification Updates

The client is auto-generated from NEAR's OpenAPI spec. When the spec updates:

1. **Automatic:** GitHub Actions checks daily and creates PRs
2. **Manual:** Run `./scripts/generate-from-openapi.sh`

If generation fails:
1. Check the OpenAPI spec for breaking changes
2. Update the KotlinPoet generator if needed
3. Test thoroughly before merging

## Release Process

Releases are automated using release-please:

1. Merge PRs to main with conventional commits
2. Release-please creates a release PR
3. Review and merge the release PR
4. GitHub Actions publishes to package repositories

## Local Publishing

To test publishing locally:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Then in another project, add mavenLocal() to repositories
repositories {
    mavenLocal()
    mavenCentral()
}
```

## Getting Help

- Open an issue for bugs or features
- Check existing issues first
- Join NEAR Discord for discussions
- Review the [Technical Debt](TECHNICAL_DEBT.md) document

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT).

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- Release notes for significant contributions
- README acknowledgments for major features

Thank you for contributing to NEAR Kotlin RPC Client!