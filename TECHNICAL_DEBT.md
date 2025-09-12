# Technical Debt & Future Improvements

This document tracks technical debt items and planned improvements for the NEAR Kotlin RPC Client.

## High Priority

### 1. Maven Central Publishing Setup
**Status:** Not Started  
**Effort:** Medium  
**Impact:** High - Required for production distribution

Currently, the project is configured for GitHub Packages publishing only. To publish to Maven Central, we need:

#### Requirements:
- [ ] **OSSRH Account Setup**
  - Create Sonatype JIRA account
  - Open new project ticket for `io.near` group ID (or use `io.github.username`)
  - Get approval for namespace

- [ ] **POM Metadata** - Add required metadata to `build.gradle.kts`:
  ```kotlin
  pom {
      name.set("NEAR Kotlin RPC Client")
      description.set("Type-safe Kotlin client for NEAR Protocol JSON-RPC")
      url.set("https://github.com/yourusername/near-kotlin-rpc")
      
      licenses {
          license {
              name.set("MIT License")
              url.set("https://opensource.org/licenses/MIT")
          }
      }
      
      developers {
          developer {
              id.set("developer-id")
              name.set("Developer Name")
              email.set("email@example.com")
          }
      }
      
      scm {
          connection.set("scm:git:git://github.com/yourusername/near-kotlin-rpc.git")
          developerConnection.set("scm:git:ssh://github.com/yourusername/near-kotlin-rpc.git")
          url.set("https://github.com/yourusername/near-kotlin-rpc")
      }
  }
  ```

- [ ] **GPG Signing Configuration**
  - Generate GPG key pair
  - Upload public key to key servers
  - Configure signing in build:
    ```kotlin
    signing {
        sign(publishing.publications["maven"])
    }
    ```

- [ ] **Sources and Javadoc JARs**
  ```kotlin
  java {
      withSourcesJar()
      withJavadocJar()
  }
  ```

- [ ] **Sonatype Repository Configuration**
  ```kotlin
  repositories {
      maven {
          name = "OSSRH"
          url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
          credentials {
              username = System.getenv("MAVEN_USERNAME")
              password = System.getenv("MAVEN_PASSWORD")
          }
      }
  }
  ```

- [ ] **GitHub Secrets Configuration**
  - `MAVEN_USERNAME` - Sonatype username
  - `MAVEN_PASSWORD` - Sonatype password
  - `SIGNING_KEY_ID` - GPG key ID
  - `SIGNING_KEY` - GPG private key (ASCII armored)
  - `SIGNING_PASSWORD` - GPG key passphrase

#### References:
- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [OSSRH Guide](https://central.sonatype.org/publish/publish-maven/)

---

## Medium Priority

### 2. Multiplatform Support
**Status:** 🟡 Not Started  
**Effort:** High  
**Impact:** Medium - Enables iOS/JS targets

Consider migrating to Kotlin Multiplatform to support:
- JVM (current)
- Android Native
- iOS (via Kotlin/Native)
- JavaScript (via Kotlin/JS)

This would match the approach of the Ktor client library and expand usability.

### 3. Coroutine Flow Support
**Status:** 🟡 Not Started  
**Effort:** Low  
**Impact:** Medium - Better reactive programming

Add support for Kotlin Flow for streaming operations:
- Block streaming
- Transaction monitoring
- Event subscriptions (when NEAR adds WebSocket support)

### 4. Retry and Circuit Breaker
**Status:** 🟡 Partially Implemented  
**Effort:** Medium  
**Impact:** Medium - Better resilience

Add resilience patterns:
- ✅ Basic retry with exponential backoff (implemented in TestUtils)
- ⚠️ Rate limiting handling for testnet integration tests
- 🔴 Circuit breaker for failing endpoints (not started)
- 🔴 Fallback to alternative RPC endpoints (not started)

**Note on Integration Tests:**
The testnet integration tests (`TestnetIntegrationTest`) experience rate limiting with the free NEAR testnet RPC endpoint. Current mitigations:
- Configurable delays between tests (`-DtestDelay=1000`)
- Configurable delays between test contexts (`-DcontextDelay=2000`)
- Retry logic with exponential backoff in `TestUtils.retryWithBackoff()`

For production use, consider:
- Using paid RPC providers (Pagoda, FastNEAR) for higher rate limits
- Implementing client-side rate limiting
- Adding circuit breaker pattern to prevent overwhelming the endpoint

---

## Low Priority

### 5. Custom Serializers
**Status:** 🟢 Not Critical  
**Effort:** Low  
**Impact:** Low - Performance optimization

Optimize serialization for specific types:
- Big integers as custom types instead of strings
- More efficient binary encoding options
- Custom date/time handling

### 6. Metrics and Observability
**Status:** 🟢 Not Critical  
**Effort:** Medium  
**Impact:** Low - Better monitoring

Add optional metrics collection:
- Request/response times
- Error rates
- Integration with Micrometer

### 7. Alternative HTTP Clients
**Status:** 🟢 Not Critical  
**Effort:** Medium  
**Impact:** Low - Flexibility

Add pluggable HTTP client support:
- Ktor client
- Retrofit
- Java 11 HTTP client

---

## Completed Items

### ✅ OpenAPI Code Generation
Automated code generation from NEAR's OpenAPI specification with proper path handling and snake_case conversion.

### ✅ GitHub Actions CI/CD
Complete automation pipeline with daily OpenAPI checks and release-please integration.

### ✅ Comprehensive Test Suite
Unit tests, serialization tests, and testnet integration tests with 80%+ coverage target.

### ✅ Package Separation
Clean separation between types and client packages as required.

---

## Notes

- **GitHub Packages** is currently configured and ready as an interim solution
- Migration to Maven Central should be prioritized once the project gains traction
- Consider applying for `io.near` namespace early as it may take time for approval
- The project structure already supports all Maven Central requirements, only configuration is missing

---

*Last updated: 2024*