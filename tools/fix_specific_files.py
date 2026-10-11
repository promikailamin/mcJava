#!/usr/bin/env python3
"""
Targeted fix for pattern matching instanceof in specific files with known errors.
Only fixes the exact patterns causing compilation errors.
"""

import re
from pathlib import Path

def fix_file_instanceof(content, patterns):
    """
    Apply specific instanceof fixes for a file.
    patterns: list of (regex_pattern, replacement) tuples
    """
    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)
    return content

# Define fixes for each file with known errors
FILE_FIXES = {
    "net/minecraft/world/entity/EntityType.java": [
        # Line 236: this instanceof EntityType && ... type
        (r'(this)\s+instanceof\s+(EntityType)\s+(&&\s+.*?\btype\.)', r'\1 instanceof \2 type \3'),
    ],
    "net/minecraft/core/Registry.java": [
        # Line 51: this instanceof Registry && ... registry
        (r'(this)\s+instanceof\s+(Registry)\s+(&&\s+.*?\bregistry\.)', r'\1 instanceof \2 registry \3'),
    ],
    "net/minecraft/network/chat/Component.java": [
        # Line 57: this instanceof Component && ... component
        (r'(this)\s+instanceof\s+(Component)\s+(&&\s+.*?\bcomponent\.)', r'\1 instanceof \2 component \3'),
    ],
    "net/minecraft/world/level/block/Block.java": [
        # Line 135: level instanceof ServerLevel && ... serverLevel
        (r'(level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 437: level instanceof ServerLevel && ... serverLevel
        (r'(level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 527: if (level instanceof ServerLevel) -> if (level instanceof ServerLevel serverLevel)
        (r'if\s*\((level)\s+instanceof\s+(ServerLevel)\)', r'if (\1 instanceof \2 serverLevel)'),
        # Line 633: level instanceof ServerLevel && ... serverLevel (two occurrences)
        (r'(level)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
    ],
    "net/minecraft/server/level/ServerLevel.java": [
        # Line 876: this instanceof ServerLevel && ... serverLevel
        (r'(this)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 2017: this instanceof ServerLevel && ... serverLevel
        (r'(this)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 2018: this instanceof ServerLevel && ... serverLevel
        (r'(this)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 2051: this instanceof ServerLevel && ... serverLevel
        (r'(this)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
        # Line 2052: this instanceof ServerLevel && ... serverLevel
        (r'(this)\s+instanceof\s+(ServerLevel)\s+(&&\s+.*?\bserverLevel\.)', r'\1 instanceof \2 serverLevel \3'),
    ],
    "net/minecraft/world/phys/shapes/CollisionContext.java": [
        # Line 60: this instanceof CollisionContext && ... context
        (r'(this)\s+instanceof\s+(CollisionContext)\s+(&&\s+.*?\bcontext\.)', r'\1 instanceof \2 context \3'),
    ],
    "net/minecraft/world/damagesource/DamageSource.java": [
        # Line 81: this instanceof DamageSource && ... source
        (r'(this)\s+instanceof\s+(DamageSource)\s+(&&\s+.*?\bsource\.)', r'\1 instanceof \2 source \3'),
        # Line 107: this instanceof DamageSource && ... source
        (r'(this)\s+instanceof\s+(DamageSource)\s+(&&\s+.*?\bsource\.)', r'\1 instanceof \2 source \3'),
    ],
    "net/minecraft/world/entity/LivingEntity.java": [
        # Line 371: this.level() instanceof ServerLevel -> this.level() instanceof ServerLevel serverLevel
        (r'(this\.level\(\))\s+instanceof\s+(ServerLevel)(?=\s+&&)', r'\1 instanceof \2 serverLevel'),
        # Line 388: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 430: if (this.level() instanceof ServerLevel) -> if (this.level() instanceof ServerLevel serverLevel)
        (r'if\s*\((this\.level\(\))\s+instanceof\s+(ServerLevel)\)', r'if (\1 instanceof \2 serverLevel)'),
        # Line 729: if (this.level() instanceof ServerLevel) -> if (this.level() instanceof ServerLevel serverLevel)
        (r'if\s*\((this\.level\(\))\s+instanceof\s+(ServerLevel)\)', r'if (\1 instanceof \2 serverLevel)'),
        # Line 1339: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1356: this instanceof LivingEntity && ... entity (float cannot be converted)
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1357: if (this.level() instanceof ServerLevel) -> if (this.level() instanceof ServerLevel serverLevel)
        (r'if\s*\((this\.level\(\))\s+instanceof\s+(ServerLevel)\)', r'if (\1 instanceof \2 serverLevel)'),
        # Line 1377: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1386: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1387: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1388: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 2923: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 2926: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 2944: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 3152: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 3191: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 3877: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
    ],
    "net/minecraft/world/entity/player/Player.java": [
        # Line 250: this instanceof Player && ... player
        (r'(this)\s+instanceof\s+(Player)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        # Line 530: if (this.level() instanceof ServerLevel) -> if (this.level() instanceof ServerLevel serverLevel)
        (r'if\s*\((this\.level\(\))\s+instanceof\s+(ServerLevel)\)', r'if (\1 instanceof \2 serverLevel)'),
        # Line 833: this instanceof LivingEntity && ... entity
        (r'(this)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
        # Line 1015: this instanceof Player && ... player
        (r'(this)\s+instanceof\s+(Player)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
        # Line 1149: serverPlayer.getServer() -> serverPlayer.getServer() (package serverPlayer does not exist)
        # This is a different issue - serverPlayer is not defined
        # Line 1170: this instanceof Player && ... player
        (r'(this)\s+instanceof\s+(Player)\s+(&&\s+.*?\bplayer\.)', r'\1 instanceof \2 player \3'),
    ],
    "net/minecraft/world/entity/item/ItemEntity.java": [
        # Line 376: this instanceof ItemEntity && ... entity
        (r'(this)\s+instanceof\s+(ItemEntity)\s+(&&\s+.*?\bentity\.)', r'\1 instanceof \2 entity \3'),
    ],
    "net/minecraft/world/item/ItemStack.java": [
        # Line 367: this instanceof ItemStack && ... stack
        (r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)', r'\1 instanceof \2 stack \3'),
        # Line 369: this instanceof ItemStack && ... stack
        (r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)', r'\1 instanceof \2 stack \3'),
        # Line 371: this instanceof ItemStack && ... stack
        (r'(this)\s+instanceof\s+(ItemStack)\s+(&&\s+.*?\bstack\.)', r'\1 instanceof \2 stack \3'),
    ],
}

def process_file(filepath):
    rel_path = str(filepath).replace('android/app/src/main/java/', '')
    if rel_path not in FILE_FIXES:
        return False
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = fix_file_instanceof(content, FILE_FIXES[rel_path])
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    fixed_count = 0
    for rel_path in FILE_FIXES.keys():
        filepath = Path('android/app/src/main/java') / rel_path
        if filepath.exists():
            try:
                if process_file(filepath):
                    fixed_count += 1
                    print(f"Fixed: {filepath}")
            except Exception as e:
                print(f"Error processing {filepath}: {e}")
        else:
            print(f"File not found: {filepath}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()