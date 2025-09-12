# NEAR Kotlin RPC Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![CI](https://github.com/yourusername/near-kotlin-rpc/actions/workflows/ci.yml/badge.svg)](https://github.com/yourusername/near-kotlin-rpc/actions/workflows/ci.yml)

A high-quality, type-safe Kotlin client for NEAR Protocol's JSON-RPC interface. Auto-generated from [NEAR's official OpenAPI specification](https://github.com/near/nearcore/blob/master/chain/jsonrpc/openapi/openapi.json).

This project is part of the NEAR Protocol client ecosystem, alongside:
- [near-jsonrpc-client-rs](https://github.com/near/near-jsonrpc-client-rs) - Rust implementation
- [near-jsonrpc-client-ts](https://github.com/near/near-jsonrpc-client-ts) - TypeScript implementation
- [near-openapi-client](https://github.com/PolyProgrammist/near-openapi-client) - OpenAPI-based Rust client

## Features

- **Auto-generated** from NEAR's official OpenAPI specification
- **Two packages**: Lightweight types package and full client package
- **Type-safe** Kotlin data classes with kotlinx.serialization
- **Coroutine-based** async API for non-blocking operations
- **Automatic** snake_case to camelCase conversion
- **Tree-shakable** for optimal bundle size
- **Comprehensive** error handling and validation
- **80%+ test coverage** with integration tests
- **CI/CD automation** for keeping up-to-date with nearcore

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.near:near-jsonrpc-client:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.near:near-jsonrpc-client:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.near</groupId>
    <artifactId>near-jsonrpc-client</artifactId>
    <version>1.0.0</version>
</dependency>
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
git clone https://github.com/yourusername/near-kotlin-rpc.git
cd near-kotlin-rpc

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate code from OpenAPI spec (fetches from nearcore)
./scripts/generate-from-openapi.sh

# Or use Gradle task directly
./gradlew fetchOpenApiSpec generateNearRpcFromOpenApi
```

### Project Structure

```
near-kotlin-rpc/
├── near-jsonrpc-types/       # Lightweight types package
├── near-jsonrpc-client/      # RPC client implementation
├── example/                  # Example usage and integration tests
├── scripts/                  # Code generation and utility scripts
│   ├── generate-from-openapi.sh
│   ├── openapi_fixes.py     # Pre/post-processing fixes
│   └── convert_naming.py    # Snake_case to camelCase converter
└── .github/workflows/        # CI/CD automation

### Code Generation

This project uses automated code generation from NEAR's OpenAPI specification located at [`nearcore/chain/jsonrpc/openapi/openapi.json`](https://github.com/near/nearcore/blob/master/chain/jsonrpc/openapi/openapi.json). 

The generation process:

1. Fetches the latest OpenAPI spec from the nearcore repository
2. Generates Kotlin code using OpenAPI Generator with kotlinx_serialization
3. Converts naming from snake_case to camelCase for idiomatic Kotlin
4. Organizes generated code into appropriate modules (types and client)

To regenerate the code:

```bash
# Using the shell script
./scripts/generate-from-openapi.sh

# Or using Gradle tasks
./gradlew fetchOpenApiSpec generateNearRpcFromOpenApi
```

The generated code is placed in:
- `near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated/` - Data models
- `near-jsonrpc-client/src/main/kotlin/io/near/jsonrpc/client/generated/` - API client

## Testing

The project includes comprehensive unit tests using Kotest and MockWebServer:

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- NEAR Protocol team for the comprehensive RPC API
- OpenAPI Generator for code generation tools
- Kotlin community for excellent libraries and tools

## Support

For issues, questions, or suggestions, please open an issue on [GitHub](https://github.com/yourusername/near-kotlin-rpc/issues).