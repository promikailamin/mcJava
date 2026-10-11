#!/usr/bin/env python3
"""
Re-apply manual fixes to Entity.java and other core files.
"""

import re

def fix_entity(content):
    fixes = [
        # Line 585: this.level() instanceof ServerLevel && this instanceof Leashable
        (r'this\.firstTick = false;\s+if\s*\(this\.level\(\)\s+instanceof\s+ServerLevel\s+&&\s+this\s+instanceof\s+Leashable\)',
         'this.firstTick = false;\n        if (this.level() instanceof ServerLevel serverLevel && this instanceof Leashable)'),
        
        # Line 641: lavaHurt
        (r'public void lavaHurt\(\)\s*\{\s*if\s*\(!this\.fireImmune\(\)\)\s*\{\s*if\s*\(this\.level\(\)\s+instanceof\s+ServerLevel\s+&&\s+this\.hurtServer\(serverLevel,',
         'public void lavaHurt() {\n        if (!this.fireImmune()) {\n           if (this.level() instanceof ServerLevel serverLevel && this.hurtServer(serverLevel,'),
        
        # Line 1362: debugEntityBlockIntersections
        (r'boolean debugEntityBlockIntersections = this\.level instanceof ServerLevel\s+&&\s+serverLevel\.getServer\(\)\.debugSubscribers\(\)\.hasAnySubscriberFor',
         'boolean debugEntityBlockIntersections = this.level instanceof ServerLevel serverLevel && serverLevel.getServer().debugSubscribers().hasAnySubscriberFor'),
        
        # Line 1981: hurtOrSimulate
        (r'return this\.level instanceof ServerLevel\s+\?\s*this\.hurtServer\(serverLevel,',
         'return this.level instanceof ServerLevel serverLevel ? this.hurtServer(serverLevel,'),
        
        # Lines 2412-2414, 2423: shearOffAllLeashConnections and dropAllLeashConnections
        (r'public boolean shearOffAllLeashConnections\(final @Nullable Player player\)\s*\{\s*boolean dropped = this\.dropAllLeashConnections\(player\);\s*if\s*\(dropped && this\.level\(\) instanceof ServerLevel\)\s*\{\s*ServerLevel serverLevel = \(ServerLevel\) dropped && this\.level\(\);',
         'public boolean shearOffAllLeashConnections(final @Nullable Player player) {\n       boolean dropped = this.dropAllLeashConnections(player);\n       if (dropped && this.level() instanceof ServerLevel serverLevel) {'),
        
        (r'public boolean dropAllLeashConnections\(final @Nullable Player player\)\s*\{\s*List<Leashable> leashables = Leashable\.leashableLeashedTo\(this\);\s*boolean dropped = !leashables\.isEmpty\(\);\s*if\s*\(this instanceof Leashable && leashableThis\.isLeashed\(\)\)',
         'public boolean dropAllLeashConnections(final @Nullable Player player) {\n       List<Leashable> leashables = Leashable.leashableLeashedTo(this);\n       boolean dropped = !leashables.isEmpty();\n       if (this instanceof Leashable leashableThis && leashableThis.isLeashed())'),
        
        # Line 3166: teleport
        (r'public @Nullable Entity teleport\(final TeleportTransition transition\)\s*\{\s*if\s*\(this\.level\(\) instanceof ServerLevel\s+&&\s+!this\.isRemoved\(\)\)\s*\{\s*ServerLevel newLevel = transition\.newLevel\(\);\s*boolean otherDimension = newLevel\.dimension\(\) != serverLevel\.dimension\(\);',
         'public @Nullable Entity teleport(final TeleportTransition transition) {\n       if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved()) {\n          ServerLevel newLevel = transition.newLevel();\n          boolean otherDimension = newLevel.dimension() != serverLevel.dimension();'),
        
        # Lines 3321-3323: removeAfterChangingDimensions
        (r'if\s*\(this instanceof WaypointTransmitter && this\.level instanceof ServerLevel\)\s*\{\s*ServerLevel serverLevel = \(ServerLevel\) this instanceof WaypointTransmitter && this\.level;',
         'if (this instanceof WaypointTransmitter waypointTransmitter && this.level instanceof ServerLevel serverLevel) {'),
        
        # Lines 3965-3972: levelCallback.onMove
        (r'this\.levelCallback\.onMove\(\);\s*if\s*\(!this\.firstTick && this\.level instanceof ServerLevel\s+&&\s+!this\.isRemoved\(\)\)\s*\{\s*if\s*\(this instanceof WaypointTransmitter && waypointTransmitter\.isTransmittingWaypoint\(\)\)\s*\{\s*serverLevel\.getWaypointManager\(\).updateWaypoint\(waypointTransmitter\);\s*\}\s*if\s*\(this instanceof ServerPlayer && player\.isReceivingWaypoints\(\) && player\.connection != null\)\s*\{\s*serverLevel\.getWaypointManager\(\).updatePlayer\(player\);',
         'this.levelCallback.onMove();\n          if (!this.firstTick && this.level instanceof ServerLevel serverLevel && !this.isRemoved()) {\n             if (this instanceof WaypointTransmitter waypointTransmitter && waypointTransmitter.isTransmittingWaypoint()) {\n                serverLevel.getWaypointManager().updateWaypoint(waypointTransmitter);\n             }\n\n             if (this instanceof ServerPlayer player && player.isReceivingWaypoints() && player.connection != null) {\n                serverLevel.getWaypointManager().updatePlayer(player);'),
        
        # Line 2675-2680: getHandHoldingItemAngle
        (r'public Vec3 getHandHoldingItemAngle\(final Item item\)\s*\{\s*if\s*\(!\(this instanceof Player\)\)\s*\{\s*return Vec3\.ZERO;\s*\}\s*else\s*\{\s*boolean itemOnlyInOffhand = player\.getOffhandItem\(\)\.is\(item\) && !player\.getMainHandItem\(\)\.is\(item\);',
         'public Vec3 getHandHoldingItemAngle(final Item item) {\n        if (!(this instanceof Player player)) {\n           return Vec3.ZERO;\n        } else {\n           boolean itemOnlyInOffhand = player.getOffhandItem().is(item) && !player.getMainHandItem().is(item);'),
        
        # Line 3336: canTeleport
        (r'if\s*\(passenger instanceof ServerPlayer && !player\.seenCredits\)',
         'if (passenger instanceof ServerPlayer player && !player.seenCredits)'),
        
        # Lines 4168, 4172: getKnownMovement, getKnownSpeed
        (r'public Vec3 getKnownMovement\(\)\s*\{\s*return this\.getControllingPassenger\(\) instanceof Player\s+&&\s+this\.isAlive\(\) \? controller\.getKnownMovement\(\) : this\.getDeltaMovement\(\);',
         'public Vec3 getKnownMovement() {\n        return this.getControllingPassenger() instanceof Player controller && this.isAlive() ? controller.getKnownMovement() : this.getDeltaMovement();'),
        
        (r'public Vec3 getKnownSpeed\(\)\s*\{\s*return this\.getControllingPassenger\(\) instanceof Player\s+&&\s+this\.isAlive\(\) \? controller\.getKnownSpeed\(\) : this\.lastKnownSpeed;',
         'public Vec3 getKnownSpeed() {\n       return this.getControllingPassenger() instanceof Player controller && this.isAlive() ? controller.getKnownSpeed() : this.lastKnownSpeed;'),
    ]
    
    for pattern, replacement in fixes:
        content = re.sub(pattern, replacement, content, flags=re.DOTALL | re.MULTILINE)
    
    return content

def fix_damage_source(content):
    fixes = [
        # Line 81: getMainHandItem
        (r'this\.causingEntity instanceof LivingEntity\s+\?\s*getMainHandItem\(\)',
         'this.causingEntity instanceof LivingEntity living ? living.getMainHandItem()'),
        
        # Line 107: isCreativePlayer
        (r'entity instanceof Player\s+&&\s*player\.getAbilities\(\)\.instabuild',
         'entity instanceof Player player && player.getAbilities().instabuild'),
    ]
    
    for pattern, replacement in fixes:
        content = re.sub(pattern, replacement, content)
    
    return content

def fix_block(content):
    fixes = [
        # Line 135: byItem
        (r'public static Block byItem\(final @Nullable Item item\)\s*\{\s*return item instanceof BlockItem\s+\?\s*getBlock\(\)\s*:\s*Blocks\.AIR;',
         'public static Block byItem(final @Nullable Item item) {\n       return item instanceof BlockItem blockItem ? blockItem.getBlock() : Blocks.AIR;'),
        
        # Lines 382, 390, 405: dropResources duplicate serverLevel
        (r'if\s*\(level instanceof ServerLevel serverLevel\)\s*\{\s*ServerLevel serverLevel = \(ServerLevel\) level;',
         'if (level instanceof ServerLevel serverLevel) {'),
        
        # Line 527: playerWillDestroy
        (r'if\s*\(state\.is\(BlockTags\.GUARDED_BY_PIGLINS\) && level instanceof ServerLevel\)\s*\{\s*ServerLevel serverLevel = \(ServerLevel\) state\.is\(BlockTags\.GUARDED_BY_PIGLINS\) && level;',
         'if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel serverLevel) {'),
        
        # Line 633: ShapePairKey equals
        (r'return o instanceof Block\.ShapePairKey\s+&&\s+this\.first == that\.first && this\.second == that\.second;',
         'return o instanceof Block.ShapePairKey that && this.first == that.first && this.second == that.second;'),
    ]
    
    for pattern, replacement in fixes:
        content = re.sub(pattern, replacement, content, flags=re.DOTALL | re.MULTILINE)
    
    return content

def fix_server_level(content):
    fixes = [
        # Line 876: mayInteract
        (r'return !\(entity instanceof Player\s+&&\s*\(this\.server\.isUnderSpawnProtection\(this, pos, player\)',
         'return !(entity instanceof Player player && (this.server.isUnderSpawnProtection(this, pos, player)'),
        
        # Lines 2017, 2050: onCreated, onTrackingStart
        (r'if\s*\(entity instanceof WaypointTransmitter\s+&&\s*waypoint\.isTransmittingWaypoint\(\)\)',
         'if (entity instanceof WaypointTransmitter waypoint && waypoint.isTransmittingWaypoint())'),
        
        # Line 2018: onDestroyed - already has explicit cast, just fix the condition
        (r'if\s*\(entity instanceof WaypointTransmitter\)\s*\{\s*WaypointTransmitter waypoint = \(WaypointTransmitter\) entity;',
         'if (entity instanceof WaypointTransmitter waypoint) {'),
    ]
    
    for pattern, replacement in fixes:
        content = re.sub(pattern, replacement, content, flags=re.DOTALL | re.MULTILINE)
    
    return content

def fix_collision_context(content):
    # Fix unreachable statement
    content = re.sub(
        r'static CollisionContext of\(final Entity entity\)\s*\{\s*if\s*\(entity instanceof AbstractMinecart\)\s*\{\s*AbstractMinecart minecart = \(AbstractMinecart\) entity;\s*return AbstractMinecart\.useExperimentalMovement\(minecart\.level\(\)\)\s*\?\s*new MinecartCollisionContext\(minecart, false\)\s*:\s*new EntityCollisionContext\(entity, false, false\);\s*\}\s*else\s*\{\s*return new EntityCollisionContext\(entity, false, false\);\s*\};\s*\}',
        '''static CollisionContext of(final Entity entity) {
      if (entity instanceof AbstractMinecart) {
          AbstractMinecart minecart = (AbstractMinecart) entity;
          return AbstractMinecart.useExperimentalMovement(minecart.level())
            ? new MinecartCollisionContext(minecart, false)
            : new EntityCollisionContext(entity, false, false);
      } else {
          return new EntityCollisionContext(entity, false, false);
      }
   }''',
        content,
        flags=re.DOTALL | re.MULTILINE
    )
    
    # Fix getMainHandItem
    content = re.sub(
        r'entity instanceof LivingEntity\s+\?\s*getMainHandItem\(\)',
        'entity instanceof LivingEntity living ? living.getMainHandItem()',
        content
    )
    
    return content

def fix_component(content):
    # Line 57: tryCollapseToString
    content = re.sub(
        r'this\.getContents\(\) instanceof PlainTextContents\s+&&\s+this\.getSiblings\(\)\.isEmpty\(\)\s+&&\s+this\.getStyle\(\)\.isEmpty\(\)\s+\?\s+text\.text\(\)',
        'this.getContents() instanceof PlainTextContents text && this.getSiblings().isEmpty() && this.getStyle().isEmpty() ? text.text()',
        content
    )
    return content

def fix_entity_type(content):
    # Line 236: updateCustomEntityTag
    content = re.sub(
        r'user instanceof Player\s+&&\s*server\.getPlayerList\(\\)\.isOp\(player\.nameAndId\(\)\)',
        'user instanceof Player player && server.getPlayerList().isOp(player.nameAndId())',
        content
    )
    return content

def fix_registry(content):
    # Line 51: safeCastToReference
    content = re.sub(
        r'holder instanceof Holder\.Reference<T>\s+\?\s*DataResult\.success\(reference\)',
        'holder instanceof Holder.Reference<T> reference ? DataResult.success(reference)',
        content
    )
    return content

def fix_direction(content):
    # Line 477: missing return
    content = re.sub(
        r'public Direction\.Plane getPlane\(\)\s*\{\s*if\s*\(this == X \|\| this == Z\)\s*\{\s*return Direction\.Plane\.HORIZONTAL;\s*\}\s*\}',
        '''public Direction.Plane getPlane() {
         if (this == X || this == Z) {
             return Direction.Plane.HORIZONTAL;
         } else {
             return Direction.Plane.VERTICAL;
         }
      }''',
        content,
        flags=re.DOTALL | re.MULTILINE
    )
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
        ('android/app/src/main/java/net/minecraft/world/entity/Entity.java', fix_entity),
        ('android/app/src/main/java/net/minecraft/world/damagesource/DamageSource.java', fix_damage_source),
        ('android/app/src/main/java/net/minecraft/world/level/block/Block.java', fix_block),
        ('android/app/src/main/java/net/minecraft/server/level/ServerLevel.java', fix_server_level),
        ('android/app/src/main/java/net/minecraft/world/phys/shapes/CollisionContext.java', fix_collision_context),
        ('android/app/src/main/java/net/minecraft/network/chat/Component.java', fix_component),
        ('android/app/src/main/java/net/minecraft/world/entity/EntityType.java', fix_entity_type),
        ('android/app/src/main/java/net/minecraft/core/Registry.java', fix_registry),
        ('android/app/src/main/java/net/minecraft/core/Direction.java', fix_direction),
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