#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

OPENAPI_URL="https://raw.githubusercontent.com/near/nearcore/master/chain/jsonrpc/openapi/openapi.json"
OPENAPI_SPEC_FILE="$PROJECT_ROOT/openapi-spec.json"
GENERATED_DIR="$PROJECT_ROOT/build/generated"

echo "========================================="
echo "NEAR Kotlin RPC Code Generation"
echo "========================================="

echo "1. Fetching OpenAPI specification from nearcore..."
curl -L "$OPENAPI_URL" -o "$OPENAPI_SPEC_FILE"

if [ ! -f "$OPENAPI_SPEC_FILE" ]; then
    echo "Error: Failed to download OpenAPI spec"
    exit 1
fi

echo "2. Applying fixes to OpenAPI specification..."
python3 "$SCRIPT_DIR/openapi_fixes.py" --spec-fix "$OPENAPI_SPEC_FILE"

echo "3. Running Gradle task to generate Kotlin code..."
cd "$PROJECT_ROOT"
./gradlew generateNearRpcFromOpenApi

echo "4. Applying post-generation fixes to Kotlin code..."
python3 "$SCRIPT_DIR/openapi_fixes.py" --code-fix "$GENERATED_DIR"

echo "5. Processing generated code for snake_case to camelCase conversion..."
if [ -d "$GENERATED_DIR/src/main/kotlin" ]; then
    python3 "$SCRIPT_DIR/convert_naming.py" "$GENERATED_DIR/src/main/kotlin"
else
    echo "Warning: Generated code directory not found, skipping conversion"
fi

echo "6. Organizing generated code into modules..."

# Create directories if they don't exist
mkdir -p "$PROJECT_ROOT/near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated"
mkdir -p "$PROJECT_ROOT/near-jsonrpc-client/src/main/kotlin/io/near/jsonrpc/client/generated"

# Copy generated models to types module
if [ -d "$GENERATED_DIR/src/main/kotlin/io/near/jsonrpc/models" ]; then
    echo "   - Copying models to near-jsonrpc-types module..."
    cp -r "$GENERATED_DIR/src/main/kotlin/io/near/jsonrpc/models/"* \
        "$PROJECT_ROOT/near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated/" 2>/dev/null || true
fi

# Copy generated API client to client module  
if [ -d "$GENERATED_DIR/src/main/kotlin/io/near/jsonrpc/api" ]; then
    echo "   - Copying API client to near-jsonrpc-client module..."
    cp -r "$GENERATED_DIR/src/main/kotlin/io/near/jsonrpc/api/"* \
        "$PROJECT_ROOT/near-jsonrpc-client/src/main/kotlin/io/near/jsonrpc/client/generated/" 2>/dev/null || true
fi

echo "========================================="
echo "Code generation complete!"
echo "Generated files are in:"
echo "  - Types: near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated/"
echo "  - Client: near-jsonrpc-client/src/main/kotlin/io/near/jsonrpc/client/generated/"
echo "========================================="