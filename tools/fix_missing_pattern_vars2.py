#!/usr/bin/env python3
"""
Fix missing pattern variables in instanceof expressions - targeted version.
Only fixes specific known patterns:
1. equals() methods: o instanceof Type && ... varName.field
2. if statements: this.level() instanceof ServerLevel && ... serverLevel
"""

import re
from pathlib import Path

def fix_equals_method(content):
    """
    Fix equals() methods with pattern: return o instanceof Type && ... varName.field
    Common pattern variables: vec3i, that
    """
    # Pattern 1: Vec3i.equals - uses 'vec3i'
    content = re.sub(
        r'(return\s+\w+\s+instanceof\s+Vec3i)\s+&&\s+(.*?\bvec3i\.)',
        r'\1 vec3i && \2',
        content
    )
    
    # Pattern 2: Identifier.equals, BoundingBox.equals - uses 'that'
    content = re.sub(
        r'(return\s+\w+\s+instanceof\s+(?:Identifier|BoundingBox))\s+&&\s+(.*?\bthat\.)',
        r'\1 that && \2',
        content
    )
    
    return content

def fix_entity_serverlevel(content):
    """
    Fix Entity.java patterns: this.level() instanceof ServerLevel && ... serverLevel
    """
    # Pattern: if (this.level() instanceof ServerLevel && ...) { ... serverLevel ...
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Check for if statement with this.level() instanceof ServerLevel
        if 'this.level() instanceof ServerLevel' in line and 'serverLevel' in line:
            # Replace: this.level() instanceof ServerLevel -> this.level() instanceof ServerLevel serverLevel
            new_line = line.replace(
                'this.level() instanceof ServerLevel',
                'this.level() instanceof ServerLevel serverLevel'
            )
            new_lines.append(new_line)
            i += 1
            continue
        
        # Check for multi-line if condition
        if 'this.level() instanceof ServerLevel' in line:
            # Look ahead for serverLevel usage
            condition_block = line
            j = i
            while j < len(lines) and ')' not in condition_block:
                j += 1
                if j < len(lines):
                    condition_block += ' ' + lines[j]
            
            if 'serverLevel' in condition_block:
                # Need to fix the instanceof line
                new_line = line.replace(
                    'this.level() instanceof ServerLevel',
                    'this.level() instanceof ServerLevel serverLevel'
                )
                new_lines.append(new_line)
                i += 1
                continue
        
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_if_with_pattern_var(content):
    """
    Fix if statements where instanceof is followed by && and a variable is used.
    """
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Match: if (expr instanceof Type && ... varName. ...) {
        match = re.match(r'^(\s*)if\s*\((\w+(?:\.\w+)*)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            
            # Collect the full condition (might span multiple lines)
            condition = line
            j = i
            while j < len(lines) and not re.search(r'\)\s*\{?\s*$', condition):
                j += 1
                if j < len(lines):
                    condition += ' ' + lines[j].strip()
            
            # Find variable usage pattern
            # Look for varName.field or varName.method()
            var_match = re.search(r'&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+)', condition)
            if var_match:
                var_name = var_match.group(1)
                # Only fix if var_name is not 'this' and not the expr
                if var_name != 'this' and var_name != expr.split('.')[-1]:
                    # Replace in the condition
                    new_condition = re.sub(
                        rf'(\b{re.escape(expr)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        condition
                    )
                    # Reconstruct the if statement
                    if line.rstrip().endswith('{'):
                        new_line = re.sub(r'if\s*\(.*\)\s*\{', f'if ({new_condition}) {{', line)
                    else:
                        new_line = re.sub(r'if\s*\(.*\)\s*$', f'if ({new_condition})', line)
                    new_lines.append(new_line)
                    # Skip the lines we consumed
                    i = j + 1
                    continue
        
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # Apply targeted fixes
    content = fix_equals_method(content)
    content = fix_entity_serverlevel(content)
    content = fix_if_with_pattern_var(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        # Skip excluded directories
        if any(part in filepath.parts for part in ['net/fabricmc', 'com/mojang/renderpearl/backend/vulkan', 'stubs']):
            continue
        try:
            if process_file(filepath):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()