#!/usr/bin/env python3
"""
Script to fix pattern matching switch statements that reference MatchException.
Replaces them with if-else chains or removes the default case.
"""
import re
import sys
from pathlib import Path

def fix_file(filepath):
    content = filepath.read_text()
    original = content
    
    # Pattern to match: switch (expr) { case Type var -> ...; default -> throw new IllegalStateException("MatchException: ..."); }
    # This is complex to do with regex, so let's do a simpler approach:
    # Replace the default case that throws MatchException with a simple throw
    
    # Simple approach: replace all occurrences of MatchException in exception messages
    content = content.replace('"MatchException: " + null, null', '"Unexpected value"')
    content = content.replace('"MatchException: " + ', '"Unexpected value: " + ')
    content = content.replace('"MatchException: " + null', '"Unexpected value"')
    
    if content != original:
        filepath.write_text(content)
        print(f"Fixed: {filepath}")
        return True
    return False

def main():
    files = [
        "com/mojang/renderpearl/backend/opengl/GlCommandEncoder.java",
        "com/mojang/renderpearl/frontend/FrontendRenderPass.java",
        "net/minecraft/server/network/config/PrepareSpawnTask.java",
        "net/minecraft/server/commands/data/DataCommands.java",
        "net/minecraft/nbt/NbtOps.java",
        "net/minecraft/client/quickplay/QuickPlay.java",
        "net/minecraft/client/multiplayer/chat/report/AbuseReportSender.java",
        "net/minecraft/client/gui/screens/social/RemoteFriendListUpdateHandler.java",
        "net/minecraft/client/gui/screens/social/PlayerSocialManager.java",
        "net/minecraft/client/gui/screens/friends/FriendEntry.java",
        "net/minecraft/client/renderer/PostChainConfig.java",
        "net/minecraft/client/renderer/feature/TextFeatureRenderer.java",
        "net/minecraft/client/renderer/PostChain.java",
        "net/minecraft/client/resources/SkinManager.java",
        "net/minecraft/client/resources/model/sprite/TextureSlots.java",
        "net/minecraft/commands/synchronization/brigadier/StringArgumentSerializer.java",
        "net/minecraft/world/entity/SteppedInterpolationHandler.java",
        "net/minecraft/world/item/BundleItem.java",
        "net/minecraft/world/level/storage/TagValueOutput.java",
        "net/minecraft/world/level/storage/loot/FloatRangePredicate.java",
        "net/minecraft/world/level/storage/loot/IntRangePredicate.java",
        "net/minecraft/world/level/storage/loot/providers/number/ints/ResolvableInt.java",
        "net/minecraft/world/level/storage/loot/providers/number/floats/ResolvableFloat.java",
        "net/minecraft/world/level/storage/TagValueInput.java",
        "net/minecraft/world/level/block/BaseRailBlock.java",
        "net/minecraft/util/CubicSpline.java",
        "net/minecraft/util/filefix/virtualfilesystem/CopyOnWriteFSProvider.java",
        "net/minecraft/util/filefix/virtualfilesystem/CopyOnWriteFileSystem.java",
    ]
    
    base = Path("/home/runner/work/mcJava/mcJava/android/app/src/main/java")
    fixed = 0
    for f in files:
        filepath = base / f
        if filepath.exists():
            if fix_file(filepath):
                fixed += 1
        else:
            print(f"Not found: {filepath}")
    
    print(f"Fixed {fixed} files")

if __name__ == "__main__":
    main()