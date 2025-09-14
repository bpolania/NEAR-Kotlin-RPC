#!/usr/bin/env python3
"""
Post-processing fixes for OpenAPI spec and generated Kotlin code
Similar to progenitor_fixes.py in the Rust implementation
"""

import json
import os
import re
import sys
from pathlib import Path
from typing import Dict, Any

def fix_openapi_spec(spec_file: str):
    """Apply fixes to the OpenAPI specification before code generation"""
    with open(spec_file, 'r') as f:
        spec = json.load(f)
    
    # CRITICAL FIX: Convert all paths to use single "/" endpoint for JSON-RPC
    # The OpenAPI spec has unique paths for each method but JSON-RPC uses a single endpoint
    if 'paths' in spec:
        all_methods = {}
        for path, path_item in spec['paths'].items():
            method_name = path.strip('/')
            if 'post' in path_item:
                # Extract the method info and store it
                all_methods[method_name] = path_item['post']
        
        # Replace all paths with a single "/" endpoint containing all methods
        spec['paths'] = {
            '/': {
                'post': {
                    'description': 'NEAR JSON-RPC endpoint',
                    'operationId': 'jsonrpc',
                    'x-methods': all_methods,  # Store methods as extension
                    'requestBody': {
                        'content': {
                            'application/json': {
                                'schema': {
                                    '$ref': '#/components/schemas/JsonRpcRequest'
                                }
                            }
                        }
                    },
                    'responses': {
                        '200': {
                            'description': 'Successful response',
                            'content': {
                                'application/json': {
                                    'schema': {
                                        '$ref': '#/components/schemas/JsonRpcResponse'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    
    # Fix common issues in the OpenAPI spec
    # 1. Handle nullable types properly
    def fix_nullable_types(obj):
        if isinstance(obj, dict):
            if 'type' in obj and 'nullable' in obj:
                if obj.get('nullable'):
                    # Kotlin generator handles nullable differently
                    obj['x-nullable'] = True
            for key, value in obj.items():
                fix_nullable_types(value)
        elif isinstance(obj, list):
            for item in obj:
                fix_nullable_types(item)
    
    fix_nullable_types(spec)
    
    # 2. Fix enum naming
    if 'components' in spec and 'schemas' in spec['components']:
        for schema_name, schema in spec['components']['schemas'].items():
            if 'enum' in schema:
                # Ensure enum values are properly formatted
                schema['x-enum-varnames'] = [
                    re.sub(r'[^a-zA-Z0-9_]', '_', str(val)).upper() 
                    for val in schema['enum']
                ]
    
    # 3. Add missing descriptions
    def add_descriptions(obj, path=""):
        if isinstance(obj, dict):
            if 'properties' in obj and 'description' not in obj:
                obj['description'] = f"Auto-generated type for {path}"
            for key, value in obj.items():
                add_descriptions(value, f"{path}.{key}" if path else key)
        elif isinstance(obj, list):
            for i, item in enumerate(obj):
                add_descriptions(item, f"{path}[{i}]")
    
    add_descriptions(spec)
    
    # Save the fixed spec
    with open(spec_file, 'w') as f:
        json.dump(spec, f, indent=2)
    
    print(f"Fixed OpenAPI spec saved to {spec_file}")

def fix_generated_kotlin(generated_dir: str):
    """Apply fixes to the generated Kotlin code"""
    generated_path = Path(generated_dir)
    
    if not generated_path.exists():
        print(f"Generated directory {generated_dir} does not exist")
        return
    
    # Fix all Kotlin files
    for kt_file in generated_path.rglob("*.kt"):
        with open(kt_file, 'r') as f:
            content = f.read()
        
        original_content = content
        
        # 1. Fix import statements
        content = fix_imports(content)
        
        # 2. Fix serialization annotations
        content = fix_serialization_annotations(content)
        
        # 3. Fix nullable types
        content = fix_nullable_types_kotlin(content)
        
        # 4. Add missing @Serializable annotations
        content = add_serializable_annotations(content)
        
        # 5. Fix enum serialization
        content = fix_enum_serialization(content)
        
        # 6. Fix API client to use single endpoint
        content = fix_api_client_paths(content)
        
        if content != original_content:
            with open(kt_file, 'w') as f:
                f.write(content)
            print(f"Fixed {kt_file}")

def fix_api_client_paths(content: str) -> str:
    """Fix generated API client to use single JSON-RPC endpoint"""
    # Replace any path-based API calls with JSON-RPC calls
    # Pattern: fun methodName(...) with path "/method_name"
    pattern = r'@(GET|POST|PUT|DELETE)\("([^"]+)"\)\s*\n\s*suspend fun (\w+)'
    
    def replace_with_jsonrpc(match):
        method_path = match.group(2).strip('/')
        function_name = match.group(3)
        
        # Convert to JSON-RPC style call
        return f'''suspend fun {function_name}'''
    
    content = re.sub(pattern, replace_with_jsonrpc, content)
    
    # Replace base URL patterns to use single endpoint
    content = re.sub(
        r'interface\s+\w+Api\s*\{',
        'interface NearJsonRpcApi {',
        content
    )
    
    # Ensure all API calls go through the RPC endpoint
    content = re.sub(
        r'retrofit\.create\(\w+Api::class\.java\)',
        'retrofit.create(NearJsonRpcApi::class.java)',
        content
    )
    
    return content

def fix_imports(content: str) -> str:
    """Fix import statements in Kotlin files"""
    # Add kotlinx serialization imports if missing
    if '@Serializable' in content and 'import kotlinx.serialization' not in content:
        content = 'import kotlinx.serialization.Serializable\n' + content
    
    if '@SerialName' in content and 'import kotlinx.serialization.SerialName' not in content:
        content = 'import kotlinx.serialization.SerialName\n' + content
    
    return content

def fix_serialization_annotations(content: str) -> str:
    """Fix serialization annotations for snake_case to camelCase conversion"""
    # Pattern to find properties with snake_case names
    pattern = r'val\s+([a-z]+(?:_[a-z]+)+)\s*:'
    
    def replace_property(match):
        snake_name = match.group(1)
        camel_name = snake_to_camel(snake_name)
        
        # Check if SerialName annotation already exists
        lines = content[:match.start()].split('\n')
        last_line = lines[-1] if lines else ''
        
        if '@SerialName' not in last_line:
            # Add SerialName annotation
            indent = len(last_line) - len(last_line.lstrip())
            annotation = ' ' * indent + f'@SerialName("{snake_name}")\n'
            return f'{annotation}{" " * indent}val {camel_name}:'
        else:
            return f'val {camel_name}:'
    
    # Apply the replacement
    content = re.sub(pattern, replace_property, content, flags=re.MULTILINE)
    
    return content

def fix_nullable_types_kotlin(content: str) -> str:
    """Fix nullable type declarations in Kotlin"""
    # Fix optional/nullable types that might not have been properly converted
    content = re.sub(r':\s*Optional<([^>]+)>', r': \1?', content)
    
    # Fix array types
    content = re.sub(r':\s*Array<([^>]+)>', r': List<\1>', content)
    
    return content

def add_serializable_annotations(content: str) -> str:
    """Add @Serializable annotations to data classes and enums"""
    # Add @Serializable to data classes
    content = re.sub(
        r'^(data\s+class\s+\w+)',
        r'@Serializable\n\1',
        content,
        flags=re.MULTILINE
    )
    
    # Add @Serializable to enum classes
    content = re.sub(
        r'^(enum\s+class\s+\w+)',
        r'@Serializable\n\1',
        content,
        flags=re.MULTILINE
    )
    
    # Remove duplicate @Serializable annotations
    content = re.sub(
        r'(@Serializable\s*\n)+',
        r'@Serializable\n',
        content
    )
    
    return content

def fix_enum_serialization(content: str) -> str:
    """Fix enum serialization to use SerialName"""
    # Pattern to find enum values
    enum_pattern = r'enum\s+class\s+(\w+)[^{]*\{([^}]+)\}'
    
    def fix_enum(match):
        enum_name = match.group(1)
        enum_body = match.group(2)
        
        # Split enum values
        values = enum_body.strip().split(',')
        fixed_values = []
        
        for value in values:
            value = value.strip()
            if value:
                # If it's a simple enum value without SerialName
                if '@SerialName' not in value:
                    # Convert UPPER_SNAKE to original format for SerialName
                    original = value.replace('_', '-').lower()
                    fixed_values.append(f'    @SerialName("{original}")\n    {value}')
                else:
                    fixed_values.append(f'    {value}')
        
        values_str = ",\n".join(fixed_values)
        return f'enum class {enum_name} {{\n{values_str}\n}}'
    
    content = re.sub(enum_pattern, fix_enum, content, flags=re.DOTALL)
    
    return content

def snake_to_camel(snake_str: str) -> str:
    """Convert snake_case to camelCase"""
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def main():
    if len(sys.argv) < 2:
        print("Usage: openapi_fixes.py [--spec-fix|--code-fix] [path]")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == '--spec-fix':
        spec_file = sys.argv[2] if len(sys.argv) > 2 else 'openapi-spec.json'
        fix_openapi_spec(spec_file)
    elif command == '--code-fix':
        generated_dir = sys.argv[2] if len(sys.argv) > 2 else 'build/generated'
        fix_generated_kotlin(generated_dir)
    else:
        print(f"Unknown command: {command}")
        sys.exit(1)

if __name__ == "__main__":
    main()