#!/usr/bin/env python3
"""
Careful fix for pattern matching instanceof issues in decompiled Minecraft code.
Fixes the pattern: x instanceof Type && ... varName... -> x instanceof Type varName && ... varName...
"""

import re
from pathlib import Path

def fix_instanceof_in_content(content):
    """
    Fix instanceof patterns in if conditions and ternary expressions.
    """
    original = content
    
    # Pattern 1: if (expr instanceof Type && ... varName.method())
    # The pattern variable is referenced after && but not declared in instanceof
    # We need to find the variable name used after && and add it to the instanceof
    
    def fix_if_instanceof(match):
        prefix = match.group(1)  # "if ("
        expr = match.group(2)    # expression before instanceof
        type_name = match.group(3)  # Type name
        middle = match.group(4)     # "&& ..."
        # Find the pattern variable used after &&
        var_match = re.search(r'\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z])', middle)
        if var_match:
            var_name = var_match.group(1)
            # Exclude common non-pattern variables
            if var_name not in ('this', expr.split('.')[-1], 'e', 'it', 'obj', 'object'):
                return f'{prefix}{expr} instanceof {type_name} {var_name} {middle}'
        return match.group(0)
    
    # Fix if statements with pattern matching instanceof
    # Match: if ( expr instanceof Type && ... varName.method() )
    pattern = r'(if\s*\(\s*)(\w+(?:\.\w+)*)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+(&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z][a-z]*\())'
    content = re.sub(pattern, fix_if_instanceof, content)
    
    # Pattern 2: Fix equals() methods: return o instanceof Type && ... varName.method()
    def fix_equals_instanceof(match):
        before = match.group(1)  # "return o instanceof Type"
        after = match.group(2)   # "&& ... varName.method()"
        var_match = re.search(r'\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z][a-z]*\()', after)
        if var_match:
            var_name = var_match.group(1)
            if var_name not in ('this', 'o', 'obj', 'object', 'that'):
                return f'{before} {var_name} {after}'
        return match.group(0)
    
    pattern2 = r'(return\s+\w+\s+instanceof\s+[A-Z][a-zA-Z0-9_<>]*)\s+(&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z][a-z]*\())'
    content = re.sub(pattern2, fix_equals_instanceof, content)
    
    # Pattern 3: Fix ternary expressions: expr instanceof Type ? varName.method() : ...
    # This is more complex, skip for now
    
    # Pattern 4: Fix specific known patterns with known variable names
    known_patterns = [
        # Entity/level patterns
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        (r'(this\.level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        (r'(this)\s+instanceof\s+(Leashable)\s+(&&\s+.*?\bleashable\.)', r'\1 instanceof \2 leashable \3'),
        (r'(this)\s+instanceof\s+(WaypointTransmitter)\s+(&&\s+.*?\bwaypointTransmitter\.)', r'\1 instanceof \2 waypointTransmitter \3'),
        (r'(this)\s+instanceof\s+(ServerPlayer)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        (r'(\w+)\s+instanceof\s+(ServerPlayer)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        (r'(getControllingPassenger\(\))\s+instanceof\s+(Player)\s+(&&\s+.*?\bcontroller\.)', r'\1 instanceof \2 controller \3'),
        (r'(level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        (r'(\w+)\s+instanceof\s+(DamageSource)\s+(&&\s+.*?\bsource\.)', r'\1 instanceof \2 source \3'),
        (r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)', r'\1 instanceof \2 stack \3'),
        (r'(this)\s+instanceof\s+(EntityType)\s+(&&\s+.*?\btype\.)', r'\1 instanceof \2 type \3'),
        (r'(this)\s+instanceof\s+(Registry)\s+(&&\s+.*?\bregistry\.)', r'\1 instanceof \2 registry \3'),
        (r'(this)\s+instanceof\s+(Component)\s+(&&\s+.*?\bcomponent\.)', r'\1 instanceof \2 component \3'),
        (r'(this)\s+instanceof\s+(CollisionContext)\s+(&&\s+.*?\bcontext\.)', r'\1 instanceof \2 context \3'),
        # Identifier.equals
        (r'(\w+)\s+instanceof\s+(Identifier)\s+(&&\s+.*?\bthat\.)', r'\1 instanceof \2 that \3'),
        # BoundingBox.equals
        (r'(\w+)\s+instanceof\s+(BoundingBox)\s+(&&\s+.*?\bthat\.)', r'\1 instanceof \2 that \3'),
        # Vec3i.equals
        (r'(\w+)\s+instanceof\s+(Vec3i)\s+(&&\s+.*?\bvec3i\.)', r'\1 instanceof \2 vec3i \3'),
    ]
    
    for pattern, replacement in known_patterns:
        content = re.sub(pattern, replacement, content)
    
    return content

def fix_missing_returns(content):
    """Fix methods with missing return statements."""
    # Direction.getPlane() - add missing return for VERTICAL
    content = re.sub(
        r'(public Direction\.Plane getPlane\(\)\s*\{\s*if \(this == X \|\| this == Z\)\s*\{\s*return Direction\.Plane\.HORIZONTAL;\s*\})',
        r'\1 else {\n             return Direction.Plane.VERTICAL;\n         }',
        content
    )
    return content

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    original = content
    
    # Apply fixes
    content = fix_instanceof_in_content(content)
    content = fix_missing_returns(content)
    
    if content != original:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed: {filepath}")
            return True
        except Exception as e:
            print(f"Error writing {filepath}: {e}")
            return False
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    # Skip excluded directories
    excluded = {'net/fabricmc', 'com/mojang/renderpearl/backend/vulkan', 'stubs'}
    java_files = [f for f in java_files if not any(e in str(f) for e in excluded)]
    
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()