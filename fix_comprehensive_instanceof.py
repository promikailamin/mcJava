#!/usr/bin/env python3
"""
Comprehensive fix for pattern matching instanceof issues in decompiled Minecraft code.
Fixes the pattern: x instanceof Type && ... varName... -> x instanceof Type varName && ... varName...
"""

import re
from pathlib import Path

def fix_instanceof_in_equals(content):
    """
    Fix equals() methods: return x instanceof Type && ... varName...
    Common pattern variables: vec3i, that, serverLevel, leashable, etc.
    """
    # Pattern 1: return o instanceof Type && ... varName.
    # The varName is typically lowercase and appears after && with field/method access
    
    # Fix Vec3i.equals - uses 'vec3i'
    content = re.sub(
        r'(return\s+\w+\s+instanceof\s+Vec3i)\s+(&&\s+.*?\bvec3i\.)',
        r'\1 vec3i \2',
        content
    )
    
    # Fix Identifier.equals, BoundingBox.equals - uses 'that'
    content = re.sub(
        r'(return\s+\w+\s+instanceof\s+(?:Identifier|BoundingBox))\s+(&&\s+.*?\bthat\.)',
        r'\1 that \2',
        content
    )
    
    # Fix other equals methods with common pattern variables
    # Pattern: return o instanceof Type && ... varName.field/method
    def fix_equals_match(match):
        full = match.group(0)
        before = match.group(1)
        after = match.group(2)
        # Find the variable name used in the condition
        var_match = re.search(r'&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+)', after)
        if var_match:
            var_name = var_match.group(1)
            # Only fix if it looks like a pattern variable (lowercase, not 'this', not 'o')
            if var_name not in ('this', 'o', 'obj', 'object') and var_name[0].islower():
                return f"{before} {var_name} {after}"
        return full
    
    # More general pattern for equals methods
    content = re.sub(
        r'(return\s+\w+\s+instanceof\s+[A-Z][a-zA-Z0-9_<>]*)\s+(&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+))',
        fix_equals_match,
        content
    )
    
    return content

def fix_instanceof_in_if(content):
    """
    Fix if statements: if (x instanceof Type && ...) { ... varName... }
    """
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Match if statement with instanceof and &&
        match = re.match(r'^(\s*)if\s*\((\w+(?:\.\w+)*)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            
            # Collect full condition (might span multiple lines)
            condition = line
            j = i
            while j < len(lines) and not re.search(r'\)\s*\{?\s*$', condition):
                j += 1
                if j < len(lines):
                    condition += ' ' + lines[j].strip()
            
            # Find variable usage in condition
            var_match = re.search(r'&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+)', condition)
            if var_match:
                var_name = var_match.group(1)
                if var_name not in ('this', expr.split('.')[-1]):
                    # Fix the condition
                    new_condition = re.sub(
                        rf'(\b{re.escape(expr)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        condition
                    )
                    # Reconstruct if statement
                    if line.rstrip().endswith('{'):
                        new_line = re.sub(r'if\s*\(.*\)\s*\{', f'if ({new_condition}) {{', line)
                    else:
                        new_line = re.sub(r'if\s*\(.*\)\s*$', f'if ({new_condition})', line)
                    new_lines.append(new_line)
                    i = j + 1
                    continue
        
        new_lines.append(line)
        i += 1
    return '\n'.join(new_lines)

def fix_instanceof_ternary(content):
    """
    Fix ternary expressions: expr instanceof Type ? varName.method() : ...
    """
    # Pattern: expr instanceof Type ? varName.method() : false_expr
    def replace(match):
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        true_expr = match.group(4)
        false_expr = match.group(5)
        # Replace varName. with ((Type) expr).
        new_true = true_expr.replace(var_name + '.', f'(({type_name}) {expr}).')
        # Also replace bare var_name
        new_true = re.sub(r'\b' + re.escape(var_name) + r'\b', f'({type_name}) {expr}', new_true)
        return f"{expr} instanceof {type_name} {var_name} ? {new_true} : {false_expr}"
    
    # Match: expr instanceof Type varName ? varName.method() : false_expr
    # This is already correct pattern matching, but the decompiled code might have it wrong
    pattern = re.compile(
        r'(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*([^:]+)\s*:\s*([^,;\)\n]+)',
        re.MULTILINE
    )
    return pattern.sub(replace, content)

def fix_specific_known_patterns(content):
    """
    Fix specific known patterns in the codebase.
    """
    fixes = [
        # Entity.java: this.level() instanceof ServerLevel && ... serverLevel
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(\?.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # Entity.java: this.level instanceof ServerLevel && ... serverLevel
        (r'(this\.level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # Entity.java: this instanceof Leashable && ... leashable
        (r'(this)\s+instanceof\s+(Leashable)\s+(&&\s+.*?\bleashable\.)', r'\1 instanceof \2 leashable \3'),
        
        # Entity.java: this instanceof WaypointTransmitter && ... waypointTransmitter
        (r'(this)\s+instanceof\s+(WaypointTransmitter)\s+(&&\s+.*?\bwaypointTransmitter\.)', r'\1 instanceof \2 waypointTransmitter \3'),
        
        # Entity.java: this instanceof ServerPlayer && ... player
        (r'(this)\s+instanceof\s+(ServerPlayer)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        
        # Entity.java: passenger instanceof ServerPlayer && ... player
        (r'(\w+)\s+instanceof\s+(ServerPlayer)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        
        # Entity.java: getControllingPassenger() instanceof Player && ... controller
        (r'(getControllingPassenger\(\))\s+instanceof\s+(Player)\s+(&&\s+.*?\bcontroller\.)', r'\1 instanceof \2 controller \3'),
        
        # LivingEntity.java: this.level instanceof ServerLevel && ... serverLevel
        (r'(this\.level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # LivingEntity.java: this.level() instanceof ServerLevel && ... serverLevel
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # Block.java: level instanceof ServerLevel && ... serverLevel
        (r'(level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # ServerLevel.java: various patterns
        (r'(\w+)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # DamageSource.java: this instanceof DamageSource && ... source
        (r'(this)\s+instanceof\s+(DamageSource)\s+(&&\s+.*?\bsource\.)', r'\1 instanceof \2 source \3'),
        
        # Player.java: this.level() instanceof ServerLevel && ... serverLevel
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        
        # ItemStack.java: this instanceof ItemStack && ... stack
        (r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)', r'\1 instanceof \2 stack \3'),
        
        # EntityType.java: this instanceof EntityType && ... type
        (r'(this)\s+instanceof\s+(EntityType)\s+(&&\s+.*?\btype\.)', r'\1 instanceof \2 type \3'),
        
        # Registry.java: this instanceof Registry && ... registry
        (r'(this)\s+instanceof\s+(Registry)\s+(&&\s+.*?\bregistry\.)', r'\1 instanceof \2 registry \3'),
        
        # Component.java: this instanceof Component && ... component
        (r'(this)\s+instanceof\s+(Component)\s+(&&\s+.*?\bcomponent\.)', r'\1 instanceof \2 component \3'),
        
        # CollisionContext.java: this instanceof CollisionContext && ... context
        (r'(this)\s+instanceof\s+(CollisionContext)\s+(&&\s+.*?\bcontext\.)', r'\1 instanceof \2 context \3'),
    ]
    
    for pattern, replacement in fixes:
        content = re.sub(pattern, replacement, content)
    
    return content

def fix_missing_return(content):
    """
    Fix methods with missing return statements.
    """
    # Direction.getPlane() - add missing return for VERTICAL
    content = re.sub(
        r'(public Direction\.Plane getPlane\(\)\s*\{\s*if \(this == X \|\| this == Z\)\s*\{\s*return Direction\.Plane\.HORIZONTAL;\s*\})',
        r'\1 else {\n             return Direction.Plane.VERTICAL;\n         }',
        content
    )
    return content

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # Apply fixes
    content = fix_specific_known_patterns(content)
    content = fix_instanceof_in_equals(content)
    content = fix_instanceof_in_if(content)
    content = fix_instanceof_ternary(content)
    content = fix_missing_return(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
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
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()