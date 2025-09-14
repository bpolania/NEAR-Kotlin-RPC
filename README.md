# NEAR Kotlin RPC Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![CI](https://github.com/bpolania/NEAR-Kotlin-RPC/actions/workflows/ci.yml/badge.svg)](https://github.com/bpolania/NEAR-Kotlin-RPC/actions/workflows/ci.yml)

A high-quality, type-safe Kotlin client for NEAR Protocol's JSON-RPC interface. **100% auto-generated** from [NEAR's official OpenAPI specification](https://github.com/near/nearcore/blob/master/chain/jsonrpc/openapi/openapi.json) using a custom KotlinPoet generator.

This project is part of the NEAR Protocol client ecosystem, alongside:
- [near-jsonrpc-client-rs](https://github.com/near/near-jsonrpc-client-rs) - Rust implementation
- [near-jsonrpc-client-ts](https://github.com/near/near-jsonrpc-client-ts) - TypeScript implementation
- [near-openapi-client](https://github.com/PolyProgrammist/near-openapi-client) - OpenAPI-based Rust client

## Features

- **100% Auto-generated** from NEAR's official OpenAPI specification
- **Custom KotlinPoet Generator** handles complex schemas (anyOf, oneOf, empty schemas)
- **Two packages**: Lightweight types package and full client package
- **Type-safe** Kotlin data classes with kotlinx.serialization
- **Coroutine-based** async API for non-blocking operations
- **Automatic** snake_case to camelCase conversion
- **245+ Generated Types** covering the entire NEAR RPC API
- **Comprehensive** error handling and validation
- **CI/CD automation** with daily spec synchronization

## Installation

### From GitHub Packages

Add GitHub Packages repository to your build configuration:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/bpolania/NEAR-Kotlin-RPC")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // For types only (lightweight)
    implementation("io.near:near-jsonrpc-types:1.0.0")
    
    // For full client (includes types)
    implementation("io.near:near-jsonrpc-client:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    // For types only (lightweight)
    implementation 'io.near:near-jsonrpc-types:1.0.0'
    
    // For full client (includes types)
    implementation 'io.near:near-jsonrpc-client:1.0.0'
}
```

## Quick Start

```kotlin
import io.near.jsonrpc.client.NearRpcClient
import io.near.jsonrpc.types.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create a client instance
    val client = NearRpcClient("https://rpc.mainnet.near.org")
    
    // Get network status
    val status = client.status()
    println("Chain ID: ${status.chainId}")
    println("Latest block height: ${status.syncInfo.latestBlockHeight}")
    
    // View account details
    val account = client.viewAccount("example.near")
    println("Account balance: ${account.amount}")
    
    // Call a view function on a contract
    val response = client.callFunction(
        accountId = "contract.near",
        methodName = "get_status",
        argsBase64 = "e30=", // {} in base64
        blockReference = BlockReference(finality = Finality.FINAL)
    )
    println("Function call logs: ${response.logs}")
}
```

### Running the Example

The project includes a comprehensive example demonstrating various RPC methods:

```bash
# Run the example
./gradlew :example:run

# Run integration tests
./gradlew :example:test
```

## Available Methods

### Network Information
- `status()` - Get network status
- `networkInfo()` - Get network information
- `gasPrice()` - Get current gas price
- `validators()` - Get validator information

### Block & Chunk Operations
- `block()` - Get block by reference
- `chunk()` - Get chunk by ID

### Account Operations
- `viewAccount()` - View account details
- `viewAccessKey()` - View specific access key
- `viewAccessKeyList()` - List all access keys

### Contract Operations
- `callFunction()` - Call a view function on a contract

### Transaction Operations
- `transaction()` - Get transaction status
- `sendTransaction()` - Send transaction asynchronously
- `sendTransactionAndWait()` - Send transaction and wait for result

### Advanced Operations
- `lightClientProof()` - Get light client proof
- Plus many more experimental methods

## Configuration

### Custom HTTP Client

You can provide your own OkHttpClient instance for custom configuration:

```kotlin
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

val customHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Custom-Header", "value")
            .build()
        chain.proceed(request)
    }
    .build()

val client = NearRpcClient(
    rpcUrl = "https://rpc.mainnet.near.org",
    httpClient = customHttpClient
)
```

### Using Different Networks

```kotlin
// Mainnet
val mainnetClient = NearRpcClient("https://rpc.mainnet.near.org")

// Testnet
val testnetClient = NearRpcClient("https://rpc.testnet.near.org")

// Local node
val localClient = NearRpcClient("http://localhost:3030")
```

## Error Handling

The client provides comprehensive error handling through the `RpcException` class:

```kotlin
try {
    val account = client.viewAccount("nonexistent.near")
} catch (e: RpcException) {
    println("RPC Error: ${e.message}")
    e.error?.let { error ->
        println("Error code: ${error.code}")
        println("Error message: ${error.message}")
    }
}
```

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/bpolania/NEAR-Kotlin-RPC.git
cd NEAR-Kotlin-RPC

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate code from OpenAPI spec (fetches latest from nearcore)
./scripts/generate-from-openapi.sh
```

### Project Structure

```
NEAR-Kotlin-RPC/
├── generator/                # Custom KotlinPoet code generator
│   └── src/main/kotlin/
│       └── io/near/generator/
│           ├── KotlinGenerator.kt   # Main generator logic
│           ├── OpenApiParser.kt     # OpenAPI spec parser
│           └── Main.kt              # CLI entry point
├── near-jsonrpc-types/       # Generated type definitions (245+ models)
├── near-jsonrpc-client/      # RPC client implementation
├── example/                  # Example usage and integration tests
├── scripts/
│   └── generate-from-openapi.sh  # Main generation script
└── .github/workflows/        # CI/CD automation
```

### Code Generation Architecture

This project achieves **100% automation** using a custom KotlinPoet-based generator:

#### Why Custom Generator?

- **OpenAPI Generator limitations**: Couldn't handle NEAR's complex OpenAPI spec
- **Complex schema support**: Properly handles anyOf, oneOf, allOf, and empty schemas
- **Full control**: Direct control over generated code structure and naming
- **No post-processing needed**: Generates ready-to-compile Kotlin code

#### Generation Process

The generation pipeline:

1. **Fetch**: Downloads latest OpenAPI spec from nearcore
2. **Parse**: Custom parser processes all schema types
3. **Generate**: KotlinPoet creates type-safe Kotlin classes
4. **Compile**: Generated code compiles without modifications

```bash
# Run the complete generation pipeline
./scripts/generate-from-openapi.sh

# This will:
# 1. Fetch latest spec from https://github.com/near/nearcore
# 2. Build the KotlinPoet generator
# 3. Generate 245+ Kotlin data classes
# 4. Output to near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated/
```

#### CI/CD Integration

GitHub Actions automatically:
- Checks for OpenAPI spec updates daily
- Regenerates code when spec changes
- Runs full test suite
- Publishes to GitHub Packages

## Testing

The project includes comprehensive tests:

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html

# Run only unit tests (no network)
./gradlew test -DskipIntegrationTests=true
```

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Key Points for Contributors

1. **Do not manually edit generated code** - Modify the generator instead
2. **Generator location**: `generator/src/main/kotlin/io/near/generator/`
3. **Test your changes**: Ensure generated code compiles and tests pass
4. **Follow conventions**: Use conventional commits for clear history

## Technical Details

### Generator Features

The custom KotlinPoet generator handles:
- **Empty schemas** → Converted to Kotlin `object` singletons
- **anyOf/oneOf** → Sealed classes with proper subtyping
- **allOf** → Merged properties into single data class
- **Nullable types** → Proper Kotlin nullable type annotations
- **Snake_case** → Automatic conversion to camelCase
- **SerialName** → Preserves original JSON field names

### Generated Types

All 245+ types from NEAR's OpenAPI spec are generated, including:
- Core types: `AccountId`, `BlockId`, `CryptoHash`
- Request/Response types: `RpcQueryRequest`, `RpcStatusResponse`
- Complex unions: `ExecutionStatus`, `ActionView`
- Nested structures: Full support for deeply nested types

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- NEAR Protocol team for the comprehensive RPC API and OpenAPI specification
- Square for KotlinPoet, the excellent Kotlin code generation library
- Kotlin community for kotlinx.serialization and coroutines

## Support

For issues, questions, or suggestions, please open an issue on [GitHub](https://github.com/bpolania/NEAR-Kotlin-RPC/issues).