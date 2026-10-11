#!/usr/bin/env python3
"""
Fix all remaining 'instanceof Type this &&' patterns.
"""

import re
from pathlib import Path


def fix_this_patterns(content):
    """Replace 'instanceof Type this &&' with proper pattern variable."""
    
    # Pattern 1: return/assignment/if/while with 'instanceof Type this &&'
    # The pattern variable 'this' should be replaced with a descriptive name
    
    def replace_this_pattern(match):
        full = match.group(0)
        # Extract the type name
        type_match = re.search(r'instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&', full)
        if type_match:
            type_name = type_match.group(1)
            # Create a pattern variable name from the type
            var_name = type_name[0].lower() + type_name[1:] if type_name else 'obj'
            # Handle common cases
            if type_name == 'ServerPlayer':
                var_name = 'serverPlayer'
            elif type_name == 'LivingEntity':
                var_name = 'livingEntity'
            elif type_name == 'Player':
                var_name = 'player'
            elif type_name == 'Llama':
                var_name = 'llama'
            elif type_name == 'Camel':
                var_name = 'camel'
            elif type_name == 'AbstractFurnaceBlockEntity':
                var_name = 'abstractFurnaceBlockEntity'
            elif type_name == 'ServerLevel':
                var_name = 'serverLevel'
            # Replace 'this' with the proper variable name
            return full.replace('instanceof ' + type_name + ' this &&', f'instanceof {type_name} {var_name} &&')
        return full
    
    # Apply to all occurrences
    content = re.sub(
        r'(\w+\s+instanceof\s+[A-Z][a-zA-Z0-9_<>]*\s+this\s+&&)',
        replace_this_pattern,
        content
    )
    
    # Also fix standalone 'this &&' after instanceof in various contexts
    content = re.sub(
        r'(instanceof\s+[A-Z][a-zA-Z0-9_<>]*)\s+this\s+&&',
        lambda m: f'{m.group(1)} {m.group(1)[0].lower()}{m.group(1)[1:]} &&' if m.group(1) else m.group(0),
        content
    )
    
    # Specific fixes for known types
    replacements = [
        (r'instanceof ServerPlayer this &&', 'instanceof ServerPlayer serverPlayer &&'),
        (r'instanceof LivingEntity this &&', 'instanceof LivingEntity livingEntity &&'),
        (r'instanceof Player this &&', 'instanceof Player player &&'),
        (r'instanceof Camel this &&', 'instanceof Camel camel &&'),
        (r'instanceof AbstractFurnaceBlockEntity this &&', 'instanceof AbstractFurnaceBlockEntity abstractFurnaceBlockEntity &&'),
        (r'instanceof ServerLevel this &&', 'instanceof ServerLevel serverLevel &&'),
    ]
    
    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)
    
    # Fix nested parentheses issues like (entity instanceof Type var && expr).method()
    content = re.sub(r'\((\w+\s+instanceof\s+\w+\s+\w+\s+&&[^)]+)\)\.', r'\1.', content)
    
    return content


def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = fix_this_patterns(content)
    
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