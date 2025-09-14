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

echo "2. Building KotlinPoet generator..."
cd "$PROJECT_ROOT"
./gradlew :generator:build

echo "3. Generating Kotlin models using KotlinPoet generator..."
# Clean existing generated models
rm -rf "$PROJECT_ROOT/near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated"
mkdir -p "$PROJECT_ROOT/near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated"

# Generate using our KotlinPoet generator
./gradlew :generator:run --args="--spec $OPENAPI_SPEC_FILE --output $PROJECT_ROOT/near-jsonrpc-types/src/main/kotlin --package io.near.jsonrpc.types.generated"

echo "========================================="
echo "Code generation complete!"
echo "Generated files are in:"
echo "  - Types: near-jsonrpc-types/src/main/kotlin/io/near/jsonrpc/types/generated/"
echo "========================================="