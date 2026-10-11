#!/usr/bin/env python3
"""
Fix incorrect pattern variable 'this' in instanceof expressions.
"""

import re
from pathlib import Path


def fix_this_pattern_variable(content):
    """Replace 'instanceof Type this &&' with proper pattern variable."""
    
    # Pattern: o instanceof Type this &&field
    # The 'this' is incorrectly used as pattern variable. Replace with 'that' or appropriate name.
    
    # For equals() methods: o instanceof Type this &&field -> o instanceof Type that &&field
    def fix_equals(match):
        full = match.group(0)
        # Find the variable being compared (before instanceof)
        obj_match = re.search(r'(\w+)\s+instanceof', full)
        if obj_match:
            obj = obj_match.group(1)
            # Replace 'this' pattern variable with 'that'
            return full.replace(' instanceof ', ' that ').replace('this &&', 'that &&')
        return full
    
    # Fix equals() methods
    content = re.sub(
        r'return\s+\w+\s+==\s+\w+\s+\?\s+true\s+:\s+\w+\s+instanceof\s+[A-Z][a-zA-Z0-9_<>]*\s+this\s+&&',
        lambda m: m.group(0).replace('this &&', 'that &&'),
        content
    )
    
    # Fix if statements: if (obj instanceof Type this &&field)
    content = re.sub(
        r'if\s*\((\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'if (\1 instanceof \2 that &&',
        content
    )
    
    # Fix variable assignments: var = obj instanceof Type this &&field
    content = re.sub(
        r'=\s*(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'= \1 instanceof \2 that &&',
        content
    )
    
    # Fix lambda expressions: e -> e instanceof Type this &&field
    content = re.sub(
        r'->\s*(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'-> \1 instanceof \2 that &&',
        content
    )
    
    # Fix return statements in lambdas
    content = re.sub(
        r'return\s+(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'return \1 instanceof \2 that &&',
        content
    )
    
    # Fix ternary expressions
    content = re.sub(
        r'\?\s*(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'? \1 instanceof \2 that &&',
        content
    )
    
    # Fix while conditions
    content = re.sub(
        r'while\s*\((\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'while (\1 instanceof \2 that &&',
        content
    )
    
    # Fix assert statements
    content = re.sub(
        r'assert\s+(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        r'assert \1 instanceof \2 that &&',
        content
    )
    
    return content


def fix_missing_space_after_and(content):
    """Fix missing space after && in pattern matching."""
    # Pattern: &&name -> && name
    content = re.sub(r'&&([a-zA-Z_$][a-zA-Z0-9_$]*)', r'&& \1', content)
    return content


def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    content = fix_this_pattern_variable(content)
    content = fix_missing_space_after_and(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


def main():
    base_path = Path("/home/runner/work/mcJava/mcJava/android/app/src/main/java")
    java_files = list(base_path.rglob('*.java'))
    fixed_count = 0
    
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    
    print(f"\nTotal files fixed: {fixed_count}")


if __name__ == '__main__':
    main()