#!/usr/bin/env python3
"""
Fix LivingEntity.java duplicate serverLevel declarations and other pattern issues.
"""

import re

def fix_living_entity(content):
    # Fix duplicate serverLevel declarations
    # Pattern: if (this.level() instanceof ServerLevel serverLevel) { ServerLevel serverLevel = (ServerLevel) this.level(); ... }
    # Remove the explicit cast line
    
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Check for: if (this.level() instanceof ServerLevel serverLevel) {
        match = re.match(r'^(\s*)if\s*\((this\.level\(\))\s+instanceof\s+(ServerLevel)\s+(\w+)\)\s*\{', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            
            # Add this line
            new_lines.append(line)
            i += 1
            
            # Skip the next line if it's: ServerLevel serverLevel = (ServerLevel) this.level();
            if i < len(lines):
                next_line = lines[i]
                if re.match(rf'^\s*{re.escape(type_name)}\s+{re.escape(var_name)}\s*=\s*\(ServerLevel\)\s*{re.escape(expr)}\s*;', next_line):
                    print(f"Skipping duplicate: {next_line.strip()}")
                    i += 1
                    continue
            continue
        
        # Check for: if (level instanceof ServerLevel serverLevel) { ServerLevel serverLevel = (ServerLevel) level; ... }
        match = re.match(r'^(\s*)if\s*\((level)\s+instanceof\s+(ServerLevel)\s+(\w+)\)\s*\{', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            
            new_lines.append(line)
            i += 1
            
            if i < len(lines):
                next_line = lines[i]
                if re.match(rf'^\s*{re.escape(type_name)}\s+{re.escape(var_name)}\s*=\s*\(ServerLevel\)\s*{re.escape(expr)}\s*;', next_line):
                    print(f"Skipping duplicate: {next_line.strip()}")
                    i += 1
                    continue
            continue
        
        # Check for: if (this.level instanceof ServerLevel serverLevel) { ServerLevel serverLevel = (ServerLevel) this.level; ... }
        match = re.match(r'^(\s*)if\s*\((this\.level)\s+instanceof\s+(ServerLevel)\s+(\w+)\)\s*\{', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            type_name = match.group(3)
            var_name = match.group(4)
            
            new_lines.append(line)
            i += 1
            
            if i < len(lines):
                next_line = lines[i]
                if re.match(rf'^\s*{re.escape(type_name)}\s+{re.escape(var_name)}\s*=\s*\(ServerLevel\)\s*{re.escape(expr)}\s*;', next_line):
                    print(f"Skipping duplicate: {next_line.strip()}")
                    i += 1
                    continue
            continue
        
        # Fix: ServerLevel level = (ServerLevel) this.isAlive() && this.level(); -> if (this.isAlive() && this.level() instanceof ServerLevel level)
        # This is a malformed line
        match = re.match(r'^(\s*)ServerLevel\s+level\s*=\s*\(ServerLevel\)\s*(this\.isAlive\(\)\s*&&\s*this\.level\(\))\s*;', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            new_line = f"{indent}if ({expr} instanceof ServerLevel level) {{"
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: ServerLevel level = (ServerLevel) (reason == ... && this.level()); -> if ((reason == ... && this.level()) instanceof ServerLevel level)
        match = re.match(r'^(\s*)ServerLevel\s+level\s*=\s*\(ServerLevel\)\s*\((.*?)\)\s*;', line)
        if match:
            indent = match.group(1)
            expr = match.group(2)
            new_line = f"{indent}if ({expr} instanceof ServerLevel level) {{"
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: float cannot be converted to LivingEntity - malformed cast
        # (LivingEntity) damageBlocked > 0.0F && ... -> damageBlocked > 0.0F && ...
        match = re.match(r'^(\s*)(LivingEntity)\s+\w+\s*=\s*\(LivingEntity\)\s*(.*?)\s*;', line)
        if match:
            indent = match.group(1)
            expr = match.group(3)
            # This is a boolean expression, not a LivingEntity
            new_line = f"{indent}{expr};"
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: float cannot be converted to ServerPlayer
        # (ServerPlayer) float_value -> float_value
        match = re.match(r'^(\s*)(ServerPlayer)\s+\w+\s*=\s*\(ServerPlayer\)\s*(.*?)\s*;', line)
        if match:
            indent = match.group(1)
            expr = match.group(3)
            new_line = f"{indent}{expr};"
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: source.getDirectEntity() instanceof AbstractArrow && abstractArrow.getPierceLevel()
        # -> source.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getPierceLevel()
        if 'source.getDirectEntity() instanceof AbstractArrow && abstractArrow.getPierceLevel()' in line:
            new_line = line.replace(
                'source.getDirectEntity() instanceof AbstractArrow && abstractArrow.getPierceLevel()',
                'source.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getPierceLevel()'
            )
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: sourceEntity instanceof Wolf && wolf.isTame()
        if 'sourceEntity instanceof Wolf && wolf.isTame()' in line:
            new_line = line.replace(
                'sourceEntity instanceof Wolf && wolf.isTame()',
                'sourceEntity instanceof Wolf wolf && wolf.isTame()'
            )
            new_lines.append(new_line)
            i += 1
            continue
        
        # Fix: this.setLastHurtByMob(livingSource) -> this.setLastHurtByMob(source.getDirectEntity())
        if 'this.setLastHurtByMob(livingSource)' in line and 'livingSource' not in line.replace('this.setLastHurtByMob(livingSource)', ''):
            new_line = line.replace('livingSource', 'source.getDirectEntity()')
            new_lines.append(new_line)
            i += 1
            continue
        
        new_lines.append(line)
        i += 1
    
    return '\n'.join(new_lines)

def fix_item_stack(content):
    # Fix ItemStack.java issues
    # Line 367: this instanceof ItemStack && ... stack
    content = re.sub(
        r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)',
        r'\1 instanceof \2 stack \3',
        content
    )
    
    # Line 369, 371: similar
    content = re.sub(
        r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)',
        r'\1 instanceof \2 stack \3',
        content
    )
    
    # Line 387: incompatible types: boolean cannot be converted to Success
    # This is likely a ternary with wrong types
    # Line 501: cannot find symbol
    
    return content

def fix_player(content):
    # Fix Player.java issues
    # Line 250: this instanceof Player && ... player
    content = re.sub(
        r'(this)\s+instanceof\s+(Player)\s+(&&\s+.*?\bplayer\.)',
        r'\1 instanceof \2 player \3',
        content
    )
    
    # Line 833: this instanceof LivingEntity && ... entity
    content = re.sub(
        r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)',
        r'\1 instanceof \2 entity \3',
        content
    )
    
    # Line 1015: this instanceof Player && ... player
    content = re.sub(
        r'(this)\s+instanceof\s+(Player)\s+(&&\s+.*?\bplayer\.)',
        r'\1 instanceof \2 player \3',
        content
    )
    
    # Line 1149: package serverPlayer does not exist
    # This is a malformed variable reference
    content = re.sub(r'\bserverPlayer\.', 'serverPlayer.', content)  # placeholder
    
    # Line 1170: cannot find symbol
    
    return content

def fix_item_entity(content):
    # Line 376: this instanceof ItemEntity && ... entity
    content = re.sub(
        r'(this)\s+instanceof\s+(ItemEntity)\s+(&&\s+.*?\bentity\.)',
        r'\1 instanceof \2 entity \3',
        content
    )
    return content

def fix_item(content):
    # Line 185: cannot find symbol
    return content

def process_file(filepath, fixer):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = fixer(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    files = [
        ('android/app/src/main/java/net/minecraft/world/entity/LivingEntity.java', fix_living_entity),
        ('android/app/src/main/java/net/minecraft/world/item/ItemStack.java', fix_item_stack),
        ('android/app/src/main/java/net/minecraft/world/entity/player/Player.java', fix_player),
        ('android/app/src/main/java/net/minecraft/world/entity/item/ItemEntity.java', fix_item_entity),
        ('android/app/src/main/java/net/minecraft/world/item/Item.java', fix_item),
    ]
    
    fixed_count = 0
    for filepath, fixer in files:
        try:
            if process_file(filepath, fixer):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()