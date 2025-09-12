#!/usr/bin/env python3

import os
import re
import sys
from pathlib import Path

def snake_to_camel(snake_str):
    """Convert snake_case to camelCase."""
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def convert_kotlin_file(file_path):
    """Convert snake_case to camelCase in Kotlin files."""
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Convert property names in data classes
    pattern = r'@SerialName\("([^"]+)"\)\s+val\s+(\w+):'
    
    def replace_property(match):
        serialized_name = match.group(1)
        current_name = match.group(2)
        camel_name = snake_to_camel(serialized_name)
        return f'@SerialName("{serialized_name}")\n    val {camel_name}:'
    
    content = re.sub(pattern, replace_property, content)
    
    # Convert function names
    func_pattern = r'fun\s+([a-z]+_[a-z_]+)'
    
    def replace_function(match):
        snake_name = match.group(1)
        camel_name = snake_to_camel(snake_name)
        return f'fun {camel_name}'
    
    content = re.sub(func_pattern, replace_function, content)
    
    with open(file_path, 'w') as f:
        f.write(content)

def main():
    if len(sys.argv) != 2:
        print("Usage: convert_naming.py <directory>")
        sys.exit(1)
    
    directory = Path(sys.argv[1])
    
    if not directory.exists():
        print(f"Directory {directory} does not exist")
        sys.exit(1)
    
    # Find all Kotlin files
    kotlin_files = directory.rglob("*.kt")
    
    for file_path in kotlin_files:
        print(f"Processing {file_path}")
        convert_kotlin_file(file_path)
    
    print("Naming conversion complete!")

if __name__ == "__main__":
    main()