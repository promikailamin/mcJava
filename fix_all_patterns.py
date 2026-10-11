#!/usr/bin/env python3
"""
Comprehensive fix for all pattern matching instanceof issues in decompiled Minecraft code.
"""

import re
from pathlib import Path

def fix_file_content(content, filepath):
    original = content
    filename = filepath.name
    
    # Fix 1: if (expr instanceof Type && varName.method()) -> if (expr instanceof Type varName && varName.method())
    # More robust pattern that handles multiline
    def fix_if_instanceof(match):
        full = match.group(0)
        prefix = match.group(1)  # "if ("
        expr = match.group(2)    # expression
        type_name = match.group(3)  # Type name
        middle = match.group(4)     # "&& ..."
        
        # Find pattern variable used after &&
        var_match = re.search(r'\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z][a-z]*\()', middle)
        if var_match:
            var_name = var_match.group(1)
            # Exclude common non-pattern variables
            if var_name not in ('this', expr.split('.')[-1], 'e', 'it', 'obj', 'object', 'source', 'level', 'target', 'entity', 'player', 'damage', 'result', 'value', 'item', 'stack', 'block', 'state', 'pos', 'direction', 'axis', 'rotation', 'movement', 'velocity', 'position', 'bounding', 'box', 'shape', 'context', 'world', 'server', 'client', 'packet', 'listener', 'handler', 'processor', 'manager', 'registry', 'holder', 'reference', 'key', 'entry', 'element', 'component', 'property', 'attribute', 'tag', 'data', 'nbt', 'compound', 'list', 'byte', 'short', 'int', 'long', 'float', 'double', 'string', 'array', 'map', 'set', 'collection', 'iterator', 'stream', 'optional', 'supplier', 'consumer', 'function', 'predicate', 'bi', 'unary', 'binary', 'runnable', 'callable', 'future', 'completable', 'stage', 'thread', 'executor', 'service', 'pool', 'queue', 'deque', 'stack', 'vector', 'arraylist', 'linkedlist', 'hashset', 'hashmap', 'treeset', 'treemap', 'linkedhashset', 'linkedhashmap', 'priorityqueue', 'arraydeque', 'enumset', 'enummap', 'weakhashmap', 'identityhashmap', 'concurrenthashmap', 'copyonwritearraylist', 'copyonwritearrayset', 'blockingqueue', 'linkedblockingqueue', 'arrayblockingqueue', 'priorityblockingqueue', 'delayqueue', 'synchronousqueue', 'linkedtransferqueue', 'transferqueue'):
                # Check if var_name is not already in the instanceof
                if f'instanceof {type_name} {var_name}' not in full:
                    return f'{prefix}{expr} instanceof {type_name} {var_name} {middle}'
        return full
    
    # Match if statements with instanceof and pattern variable usage after &&
    # This handles multiline conditions
    pattern1 = r'(if\s*\(\s*)(\w+(?:\.\w+)*)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+(&&[\s\S]*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|[A-Z][a-z]*\())'
    content = re.sub(pattern1, fix_if_instanceof, content, flags=re.MULTILINE)
    
    # Fix 2: Assignment from broken pattern matching: Type var = (Type) condition && expr;
    # This is a decompilation artifact where: if (condition && expr instanceof Type var) was decompiled as: Type var = (Type) condition && expr;
    # We need to convert back to proper if statement
    def fix_broken_assignment(match):
        full = match.group(0)
        type_name = match.group(1)
        var_name = match.group(2)
        condition = match.group(3)
        expr = match.group(4)
        
        # This pattern: Type var = (Type) condition && expr;
        # Should become: if (condition && expr instanceof Type var) { ... }
        # But we don't have the body, so we need to be careful
        return full  # Skip for now, handle specific cases below
    
    # Fix 3: Specific known broken patterns
    # Pattern: Type var = (Type) condition && expr;
    # This appears in LivingEntity and other files
    broken_assignment_pattern = r'(\b\w+\s+\w+\s*=\s*\(\s*\w+\s*\)\s+[^;]+\&\&\s+[^;]+;)'
    # Too complex for regex, handle specific cases
    
    # Fix 4: Specific known files and patterns
    if filename == 'LivingEntity.java':
        content = fix_living_entity(content)
    elif filename == 'Player.java':
        content = fix_player(content)
    elif filename == 'ItemStack.java':
        content = fix_item_stack(content)
    elif filename == 'Item.java':
        content = fix_item(content)
    elif filename == 'Vec2.java':
        content = fix_vec2(content)
    elif filename == 'Entity.java':
        content = fix_entity(content)
    
    # Fix 5: equals() methods with pattern matching
    def fix_equals(match):
        full = match.group(0)
        before = match.group(1)
        after = match.group(2)
        var_match = re.search(r'\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|equals|hashCode)', after)
        if var_match:
            var_name = var_match.group(1)
            if var_name not in ('this', 'o', 'obj', 'object', 'that', 'other'):
                if f'instanceof {var_name}' not in before:
                    return f'{before} {var_name} {after}'
        return full
    
    pattern5 = r'(return\s+\w+\s+instanceof\s+[A-Z][a-zA-Z0-9_<>]*)\s+(&&\s+.*?\b([a-z][a-zA-Z0-9_]*)\.(?:get[A-Z]|[a-z]+\(|equals|hashCode))'
    content = re.sub(pattern5, fix_equals, content, flags=re.MULTILINE)
    
    # Fix 6: Ternary with instanceof
    def fix_ternary(match):
        full = match.group(0)
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        true_expr = match.group(4)
        false_expr = match.group(5)
        
        # Replace varName. with ((Type) expr).
        new_true = true_expr.replace(var_name + '.', f'(({type_name}) {expr}).')
        new_true = re.sub(r'\b' + re.escape(var_name) + r'\b', f'({type_name}) {expr}', new_true)
        return f'{expr} instanceof {type_name} {var_name} ? {new_true} : {false_expr}'
    
    pattern6 = r'(\b\w+(?:\.\w+)*)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*([^:]+)\s*:\s*([^,;\)\n]+)'
    content = re.sub(pattern6, fix_ternary, content)
    
    # Fix 7: Specific known patterns with explicit variable names
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
        # Arrow
        (r'(\w+)\s+instanceof\s+(AbstractArrow)\s+(&&\s+.*?\babstractArrow\.)', r'\1 instanceof \2 abstractArrow \3'),
        # Wolf
        (r'(\w+)\s+instanceof\s+(Wolf)\s+(&&\s+.*?\bwolf\.)', r'\1 instanceof \2 wolf \3'),
        # RespawnAnchorBlock
        (r'(\w+)\s+instanceof\s+(RespawnAnchorBlock)\s+(&&\s+.*?\bblockState\.)', r'\1 instanceof \2 blockState \3'),
        # AbstractBedBlock
        (r'(\w+)\s+instanceof\s+(AbstractBedBlock)\s+(&&\s+.*?\babstractBedBlock\.)', r'\1 instanceof \2 abstractBedBlock \3'),
        # Projectile
        (r'(\w+)\s+instanceof\s+(Projectile)\s+(&&\s+.*?\bprojectile\.)', r'\1 instanceof \2 projectile \3'),
        # ArmorStand
        (r'(\w+)\s+instanceof\s+(ArmorStand)\s+(&&\s+.*?\barmorStand\.)', r'\1 instanceof \2 armorStand \3'),
        # CrafterMenu
        (r'(\w+)\s+instanceof\s+(CrafterMenu)\s+(&&\s+.*?\bcrafterMenu\.)', r'\1 instanceof \2 crafterMenu \3'),
        # CrafterBlockEntity
        (r'(\w+)\s+instanceof\s+(CrafterBlockEntity)\s+(&&\s+.*?\bcrafterBlockEntity\.)', r'\1 instanceof \2 crafterBlockEntity \3'),
        # LivingEntity
        (r'(\w+)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\blivingEntity\.)', r'\1 instanceof \2 livingEntity \3'),
        (r'(\w+)\s+instanceof\s+(LivingEntity)\s+(&&\s+.*?\blivingTarget\.)', r'\1 instanceof \2 livingTarget \3'),
        # ItemEntity
        (r'(\w+)\s+instanceof\s+(ItemEntity)\s+(&&\s+.*?\bitem\.)', r'\1 instanceof \2 item \3'),
        # InteractionResult.Success
        (r'(\w+)\s+instanceof\s+(InteractionResult\.Success)\s+(&&\s+.*?\bsuccess\.)', r'\1 instanceof \2 success \3'),
        # Success (short)
        (r'(\w+)\s+instanceof\s+(Success)\s+(&&\s+.*?\bsuccess\.)', r'\1 instanceof \2 success \3'),
    ]
    
    for pattern, replacement in known_patterns:
        content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    return content

def fix_living_entity(content):
    """Fix specific LivingEntity.java issues."""
    # Line 370-371: if (this.level() instanceof ServerLevel && ...) -> add pattern variable and cast
    content = re.sub(
        r'if \(this\.level\(\) instanceof ServerLevel && onGround && this\.fallDistance > 0\.0\)',
        r'if (this.level() instanceof ServerLevel serverLevel && onGround && this.fallDistance > 0.0)',
        content
    )
    # Fix the call to onChangedBlock to use serverLevel
    content = re.sub(
        r'this\.onChangedBlock\(this\.level\(\), pos\);',
        r'this.onChangedBlock(serverLevel, pos);',
        content
    )
    
    # Line 388: this.level().sendParticles -> serverLevel.sendParticles
    content = re.sub(
        r'this\.level\(\)\.sendParticles\(new BlockParticleOption',
        r'serverLevel.sendParticles(new BlockParticleOption',
        content
    )
    
    # Line 430: ServerLevel level = (ServerLevel) this.isAlive() && this.level();
    content = re.sub(
        r'ServerLevel level = \(ServerLevel\) this\.isAlive\(\) && this\.level\(\);',
        r'if (this.isAlive() && this.level() instanceof ServerLevel level) {',
        content
    )
    
    # Line 729: ServerLevel level = (ServerLevel) (reason == ... ) && this.level();
    content = re.sub(
        r'ServerLevel level = \(ServerLevel\) \(reason == Entity\.RemovalReason\.KILLED \|\| reason == Entity\.RemovalReason\.DISCARDED\) && this\.level\(\);',
        r'if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel level) {',
        content
    )
    
    # Line 1339: if (source.getDirectEntity() instanceof AbstractArrow && abstractArrow.getPierceLevel() > 0)
    content = re.sub(
        r'if \(source\.getDirectEntity\(\) instanceof AbstractArrow && abstractArrow\.getPierceLevel\(\) > 0\)',
        r'if (source.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getPierceLevel() > 0)',
        content
    )
    
    # Line 1355-1356: Broken assignment
    content = re.sub(
        r'if \(damageBlocked > 0\.0F && !source\.is\(DamageTypeTags\.IS_PROJECTILE\) && source\.getDirectEntity\(\) instanceof LivingEntity\)\s*\{',
        r'if (damageBlocked > 0.0F && !source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof LivingEntity livingEntity) {',
        content
    )
    # Remove the broken assignment line
    content = re.sub(
        r'LivingEntity livingEntity = \(LivingEntity\) damageBlocked > 0\.0F && !source\.is\(DamageTypeTags\.IS_PROJECTILE\) && source\.getDirectEntity\(\);',
        r'',
        content
    )
    
    # Line 1357: this.blockUsingItem(this.level(), ...) -> this.blockUsingItem(serverLevel, ...)
    content = re.sub(
        r'this\.blockUsingItem\(this\.level\(\), livingEntity, source, damage, damageBlocked >= damage\);',
        r'this.blockUsingItem((ServerLevel) this.level(), livingEntity, source, damage, damageBlocked >= damage);',
        content
    )
    
    # Line 1377: this.setLastHurtByMob(livingSource); -> livingSource not defined
    content = re.sub(
        r'this\.setLastHurtByMob\(livingSource\);',
        r'this.setLastHurtByMob((LivingEntity) source.getEntity());',
        content
    )
    
    # Line 1386-1388: Wolf pattern
    content = re.sub(
        r'} else if \(sourceEntity instanceof Wolf && wolf\.isTame\(\)\) \{',
        r'} else if (sourceEntity instanceof Wolf wolf && wolf.isTame()) {',
        content
    )
    
    # Line 1568: level has private access in Entity - use this.level()
    content = re.sub(
        r'EnchantmentHelper\.modifyKnockback\(level, this\.getWeaponItem\(\), target, damageSource, knockback\)',
        r'EnchantmentHelper.modifyKnockback(this.level(), this.getWeaponItem(), target, damageSource, knockback)',
        content
    )
    
    # Line 1991: ServerPlayer serverPlayer = (ServerPlayer) absorbedDamage > 0.0F && ...;
    content = re.sub(
        r'ServerPlayer serverPlayer = \(ServerPlayer\) absorbedDamage > 0\.0F && absorbedDamage < 3\.4028235E37F && source\.getEntity\(\);',
        r'if (absorbedDamage > 0.0F && absorbedDamage < 3.4028235E37F && source.getEntity() instanceof ServerPlayer serverPlayer) {',
        content
    )
    
    # Line 2758: LivingEntity livingTarget = (LivingEntity) knockback > 0.0F && target;
    content = re.sub(
        r'LivingEntity livingTarget = \(LivingEntity\) knockback > 0\.0F && target;',
        r'if (knockback > 0.0F && target instanceof LivingEntity livingTarget) {',
        content
    )
    
    # Line 2923, 2926, 2944: serverLevel variable not defined
    # These are inside the if block from line 2758, so serverLevel should be available
    # But the original code structure is broken, need to check context
    
    return content

def fix_player(content):
    """Fix specific Player.java issues."""
    # Line 250: abstractBedBlock
    content = re.sub(
        r'this\.getInBlockState\(\)\.getBlock\(\) instanceof AbstractBedBlock && !abstractBedBlock\.getBedRule',
        r'this.getInBlockState().getBlock() instanceof AbstractBedBlock abstractBedBlock && !abstractBedBlock.getBedRule',
        content
    )
    
    # Line 530: ServerLevel level = (ServerLevel) !this.isSpectator() && this.level();
    content = re.sub(
        r'ServerLevel level = \(ServerLevel\) !this\.isSpectator\(\) && this\.level\(\);',
        r'if (!this.isSpectator() && this.level() instanceof ServerLevel level) {',
        content
    )
    
    # Line 833: LivingEntity livingEntity = (LivingEntity) !itemStack.isEmpty() && entity;
    content = re.sub(
        r'LivingEntity livingEntity = \(LivingEntity\) !itemStack\.isEmpty\(\) && entity;',
        r'if (!itemStack.isEmpty() && entity instanceof LivingEntity livingEntity) {',
        content
    )
    
    # Line 1015: projectile
    content = re.sub(
        r'entity instanceof Projectile && projectile\.deflect',
        r'entity instanceof Projectile projectile && projectile.deflect',
        content
    )
    
    # Line 1149: serverPlayer.connection -> need to check context
    # Line 1170: armorStand
    content = re.sub(
        r'nearby instanceof ArmorStand && armorStand\.isMarker\(\)',
        r'nearby instanceof ArmorStand armorStand && armorStand.isMarker()',
        content
    )
    
    return content

def fix_item_stack(content):
    """Fix specific ItemStack.java issues."""
    # Line 367-371: success variable
    content = re.sub(
        r'result instanceof InteractionResult\.Success && success\.wasItemInteraction\(\)',
        r'result instanceof InteractionResult.Success success && success.wasItemInteraction()',
        content
    )
    content = re.sub(
        r'ItemStack transformTo = success\.heldItemTransformedTo\(\) == null \? this : success\.heldItemTransformedTo\(\);',
        r'ItemStack transformTo = success.heldItemTransformedTo() == null ? this : success.heldItemTransformedTo();',
        content
    )
    content = re.sub(
        r'return success\.heldItemTransformedTo\(resultItemStack\);',
        r'return success.heldItemTransformedTo(resultItemStack);',
        content
    )
    
    # Line 387: InteractionResult.Success success = (InteractionResult.Success) isInstantlyUsed && result;
    content = re.sub(
        r'InteractionResult\.Success success = \(InteractionResult\.Success\) isInstantlyUsed && result;',
        r'if (isInstantlyUsed && result instanceof InteractionResult.Success success) {',
        content
    )
    
    # Line 501: owner instanceof ServerPlayer ? player : null -> player not defined
    content = re.sub(
        r'owner instanceof ServerPlayer \? player\s*:\s*null',
        r'owner instanceof ServerPlayer player ? player : null',
        content
    )
    
    return content

def fix_item(content):
    """Fix specific Item.java issues."""
    # Line 185: player variable
    content = re.sub(
        r'user instanceof Player && player\.getAbilities\(\)\.instabuild',
        r'user instanceof Player player && player.getAbilities().instabuild',
        content
    )
    return content

def fix_vec2(content):
    """Fix specific Vec2.java issues."""
    # Line 50: rhs instanceof Vec2 ? false : this.x == other.x && this.y == other.y;
    content = re.sub(
        r'return !\(rhs instanceof Vec2\) \? false : this\.x == other\.x && this\.y == other\.y;',
        r'return rhs instanceof Vec2 other && this.x == other.x && this.y == other.y;',
        content
    )
    return content

def fix_entity(content):
    """Fix specific Entity.java issues."""
    # Various patterns in Entity.java
    content = re.sub(
        r'this\.level\(\) instanceof ServerLevel && serverLevel\.',
        r'this.level() instanceof ServerLevel serverLevel && serverLevel.',
        content
    )
    content = re.sub(
        r'this\.level instanceof ServerLevel && serverLevel\.',
        r'this.level instanceof ServerLevel serverLevel && serverLevel.',
        content
    )
    content = re.sub(
        r'this instanceof Leashable && leashable\.',
        r'this instanceof Leashable leashable && leashable.',
        content
    )
    content = re.sub(
        r'this instanceof WaypointTransmitter && waypointTransmitter\.',
        r'this instanceof WaypointTransmitter waypointTransmitter && waypointTransmitter.',
        content
    )
    content = re.sub(
        r'this instanceof ServerPlayer && player\.',
        r'this instanceof ServerPlayer player && player.',
        content
    )
    content = re.sub(
        r'passenger instanceof ServerPlayer && player\.',
        r'passenger instanceof ServerPlayer player && player.',
        content
    )
    content = re.sub(
        r'getControllingPassenger\(\) instanceof Player && controller\.',
        r'getControllingPassenger() instanceof Player controller && controller.',
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
    content = fix_file_content(content, filepath)
    
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