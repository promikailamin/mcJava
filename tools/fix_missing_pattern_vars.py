#!/usr/bin/env python3
"""
Fix missing pattern variables in instanceof expressions.
The decompiled code has patterns like:
    o instanceof Type && ... varName.method()
But it should be:
    o instanceof Type varName && ... varName.method()
"""

import re
from pathlib import Path

def fix_missing_pattern_var_in_equals(content):
    """
    Fix equals() methods that use instanceof without pattern variable
    but then reference a variable name.
    """
    # Pattern: return o instanceof Type && this.field == varName.field ...
    # The varName should be the pattern variable
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        # Match: return o instanceof Type && ... varName. ...
        # We need to find the pattern variable name being used
        # Look for: instanceof Type && ... varName.method() or varName.field
        match = re.search(r'return\s+(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', line)
        if match:
            obj = match.group(1)
            type_name = match.group(2)
            # Now find what variable name is used after &&
            # Look for patterns like varName.field or varName.method()
            var_match = re.search(r'&&\s+.*?\b([a-zA-Z_$][a-zA-Z0-9_$]*)\.(?:get[A-Z]|[a-z]+)', line)
            if var_match:
                var_name = var_match.group(1)
                # Check if this looks like a pattern variable (lowercase, not 'this')
                if var_name != 'this' and var_name != obj and var_name[0].islower():
                    # Replace: o instanceof Type && -> o instanceof Type varName &&
                    new_line = re.sub(
                        rf'(\b{re.escape(obj)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        line
                    )
                    new_lines.append(new_line)
                    continue
        new_lines.append(line)
    return '\n'.join(new_lines)

def fix_missing_pattern_var_in_if(content):
    """
    Fix if statements that use instanceof without pattern variable
    but then reference a variable name.
    """
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Match: if (o instanceof Type && ... varName. ...) {
        match = re.match(r'^(\s*)if\s*\((\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', line)
        if match:
            indent = match.group(1)
            obj = match.group(2)
            type_name = match.group(3)
            # Find the variable name used in the condition
            # Look ahead in the condition (might span multiple lines)
            condition = line
            # Check if condition ends with ) or continues
            while not re.search(r'\)\s*\{?\s*$', condition) and i + 1 < len(lines):
                i += 1
                condition += ' ' + lines[i].strip()
            
            # Find variable usage in condition
            var_match = re.search(r'&&\s+.*?\b([a-zA-Z_$][a-zA-Z0-9_$]*)\.(?:get[A-Z]|[a-z]+)', condition)
            if var_match:
                var_name = var_match.group(1)
                if var_name != 'this' and var_name != obj and var_name[0].islower():
                    # Replace in the condition
                    new_condition = re.sub(
                        rf'(\b{re.escape(obj)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        condition
                    )
                    # Now reconstruct the if statement
                    if line.rstrip().endswith('{'):
                        new_line = re.sub(r'if\s*\(.*\)\s*\{', f'if ({new_condition}) {{', line)
                    else:
                        new_line = re.sub(r'if\s*\(.*\)\s*$', f'if ({new_condition})', line)
                    new_lines.append(new_line)
                    i += 1
                    continue
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_missing_pattern_var_simple(content):
    """
    Simple fix: find patterns like 'o instanceof Type &&' where the next part
    uses a variable that should be the pattern variable.
    """
    # This is a more aggressive fix - look for specific patterns
    patterns = [
        # Vec3i.equals pattern
        (r'(\w+)\s+instanceof\s+(Vec3i)\s+&&\s+.*?\b(vec3i)\.', r'\1 instanceof \2 \3 &&'),
        # Identifier.equals pattern
        (r'(\w+)\s+instanceof\s+(Identifier)\s+&&\s+.*?\b(that)\.', r'\1 instanceof \2 \3 &&'),
        # BoundingBox.equals pattern
        (r'(\w+)\s+instanceof\s+(BoundingBox)\s+&&\s+.*?\b(that)\.', r'\1 instanceof \2 \3 &&'),
        # Entity pattern: this.level() instanceof ServerLevel && ... serverLevel
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+&&\s+.*?\b(serverLevel)\b', r'\1 instanceof \2 \3 &&'),
        # Generic: obj instanceof Type && ... varName.field/method
        (r'(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.', r'\1 instanceof \2 \3 &&'),
    ]
    
    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)
    return content

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    content = fix_missing_pattern_var_simple(content)
    content = fix_missing_pattern_var_in_equals(content)
    content = fix_missing_pattern_var_in_if(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()