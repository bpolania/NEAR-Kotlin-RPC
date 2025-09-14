#!/usr/bin/env python3
"""
Fix OpenAPI spec issues that prevent proper Kotlin code generation.
This script modifies the spec to make it compatible with OpenAPI Generator for Kotlin.
"""

import json
import sys
from pathlib import Path

def fix_empty_schemas(spec):
    """Fix empty schemas by converting them to string types or adding a value property."""
    schemas = spec.get('components', {}).get('schemas', {})
    
    # Define how to fix each empty schema
    empty_schema_fixes = {
        # These are essentially type aliases for strings
        'AccountId': {
            'type': 'string',
            'description': 'NEAR Account Identifier'
        },
        'CryptoHash': {
            'type': 'string',
            'description': 'Cryptographic hash'
        },
        'PublicKey': {
            'type': 'string',
            'description': 'Public key'
        },
        'FunctionArgs': {
            'type': 'string',
            'format': 'byte',
            'description': 'Base64-encoded function arguments'
        },
        # These need to be objects with a property
        'CreateAccountAction': {
            'type': 'object',
            'properties': {
                'type': {
                    'type': 'string',
                    'enum': ['CreateAccount'],
                    'default': 'CreateAccount'
                }
            },
            'required': ['type']
        },
        'ShardId': {
            'type': 'integer',
            'format': 'int64',
            'description': 'Shard identifier'
        },
        'NearGas': {
            'type': 'string',
            'description': 'NEAR Gas amount'
        },
        'AccountIdValidityRulesVersion': {
            'type': 'integer',
            'description': 'Account ID validity rules version'
        },
        'MutableConfigValue': {
            'type': 'object',
            'properties': {
                'value': {
                    'type': 'string',
                    'description': 'Configuration value'
                }
            }
        },
        'RpcSplitStorageInfoRequest': {
            'type': 'object',
            'properties': {
                'request_type': {
                    'type': 'string',
                    'default': 'split_storage_info'
                }
            }
        }
    }
    
    # Apply fixes to empty schemas
    for name, schema in schemas.items():
        if not schema.get('properties') and not schema.get('enum') and \
           not schema.get('anyOf') and not schema.get('oneOf') and not schema.get('allOf') and \
           not schema.get('type'):
            if name in empty_schema_fixes:
                print(f"Fixing empty schema: {name}")
                schemas[name] = empty_schema_fixes.get(name, {
                    'type': 'string',
                    'description': f'Auto-fixed empty schema for {name}'
                })
            else:
                # Default fix for unknown empty schemas
                print(f"Fixing empty schema with default: {name}")
                schemas[name] = {
                    'type': 'string',
                    'description': f'Auto-fixed empty schema for {name}'
                }

def fix_anyof_schemas(spec):
    """Fix anyOf schemas by simplifying them or converting to oneOf with discriminator."""
    schemas = spec.get('components', {}).get('schemas', {})
    
    for name, schema in schemas.items():
        if 'anyOf' in schema:
            print(f"Fixing anyOf schema: {name}")
            anyof_items = schema['anyOf']
            
            # Special handling for BlockId - it's either a string hash or integer height
            if name == 'BlockId':
                schemas[name] = {
                    'type': 'object',
                    'description': schema.get('description', ''),
                    'properties': {
                        'block_hash': {
                            'type': 'string',
                            'description': 'Block hash'
                        },
                        'block_height': {
                            'type': 'integer',
                            'format': 'int64',
                            'description': 'Block height'
                        }
                    }
                }
            # For RPC requests/responses, keep the structure but ensure it's valid
            elif 'Rpc' in name and 'Request' in name:
                # These are usually request objects with different parameter types
                # Convert to object with optional properties
                properties = {}
                for item in anyof_items:
                    if '$ref' in item:
                        ref_name = item['$ref'].split('/')[-1]
                        properties[ref_name.lower()] = item
                    elif 'type' in item:
                        properties[item.get('title', 'value')] = item
                
                if properties:
                    schemas[name] = {
                        'type': 'object',
                        'description': schema.get('description', ''),
                        'properties': properties
                    }
            else:
                # For other anyOf schemas, try to merge them into a single object
                # or pick the most complex one
                if len(anyof_items) > 0:
                    # Just use the first non-null item for now
                    for item in anyof_items:
                        if item and item != {'type': 'null'}:
                            schemas[name] = item
                            break

def fix_problematic_patterns(spec):
    """Fix other problematic patterns in the spec."""
    schemas = spec.get('components', {}).get('schemas', {})
    
    for name, schema in schemas.items():
        # Fix schemas with only a description
        if len(schema) == 1 and 'description' in schema:
            print(f"Fixing description-only schema: {name}")
            schemas[name] = {
                'type': 'string',
                'description': schema['description']
            }
        
        # Fix references to non-existent schemas
        if '$ref' in schema and schema['$ref'].startswith('#/components/schemas/'):
            ref_name = schema['$ref'].split('/')[-1]
            if ref_name not in schemas:
                print(f"Fixing broken reference in {name} to {ref_name}")
                schemas[name] = {
                    'type': 'string',
                    'description': f'Reference to {ref_name}'
                }

def remove_pattern_properties(spec):
    """Remove patternProperties which cause validation issues."""
    def remove_pattern_props(obj):
        if isinstance(obj, dict):
            if 'patternProperties' in obj:
                print(f"Removing patternProperties from schema")
                del obj['patternProperties']
            for value in obj.values():
                remove_pattern_props(value)
        elif isinstance(obj, list):
            for item in obj:
                remove_pattern_props(item)
    
    remove_pattern_props(spec)

def main():
    if len(sys.argv) < 2:
        print("Usage: fix_openapi_spec.py <openapi-spec.json>")
        sys.exit(1)
    
    spec_file = Path(sys.argv[1])
    
    if not spec_file.exists():
        print(f"Error: {spec_file} does not exist")
        sys.exit(1)
    
    print(f"Loading OpenAPI spec from {spec_file}")
    with open(spec_file, 'r') as f:
        spec = json.load(f)
    
    print("\nApplying fixes...")
    
    # Apply all fixes
    fix_empty_schemas(spec)
    fix_anyof_schemas(spec)
    fix_problematic_patterns(spec)
    remove_pattern_properties(spec)
    
    # Save the fixed spec
    print(f"\nSaving fixed spec to {spec_file}")
    with open(spec_file, 'w') as f:
        json.dump(spec, f, indent=2)
    
    print("OpenAPI spec fixed successfully!")

if __name__ == "__main__":
    main()