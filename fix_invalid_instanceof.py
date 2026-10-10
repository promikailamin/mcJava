#!/usr/bin/env python3
"""
Fix invalid instanceof patterns that were incorrectly converted from switch expressions.
These have the form: if (var instanceof CONSTANT1, CONSTANT2, ...)
Which is invalid Java syntax. They should be converted back to proper switch expressions
or if-else chains with || conditions.
"""

import re
from pathlib import Path


def fix_file(filepath: Path, replacements: list[tuple[str, str]]) -> bool:
    """Apply replacements to a file."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        print(f"Fixed: {filepath}")
        return True
    return False


def main():
    # Define fixes for each file
    fixes = {
        "net/minecraft/nbt/SnbtGrammar.java": [
            # if (this.base instanceof BINARY, HEX) -> if (this.base == BINARY || this.base == HEX)
            (r'if \(this\.base instanceof BINARY, HEX\)', 'if (this.base == BINARY || this.base == HEX)'),
        ],
        "net/minecraft/client/data/models/BlockModelGenerators.java": [
            # if (state instanceof INACTIVE, COOLDOWN) -> if (state == INACTIVE || state == COOLDOWN)
            (r'if \(state instanceof INACTIVE, COOLDOWN\)', 'if (state == INACTIVE || state == COOLDOWN)'),
            # else if (state instanceof WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION)
            (r'else if \(state instanceof WAITING_FOR_PLAYERS, ACTIVE, WAITING_FOR_REWARD_EJECTION\)', 
             'else if (state == WAITING_FOR_PLAYERS || state == ACTIVE || state == WAITING_FOR_REWARD_EJECTION)'),
        ],
        "net/minecraft/client/gui/screens/social/PlayerSocialManager.java": [
            # Component title = if (resultCode instanceof SUCCESS, UPGRADE_NEEDED, CONNECTION_ISSUE, TEMPORARY_UNAVAILABLE, GENERIC_ERROR) { null; } else { throw ... }
            # This was a switch expression, convert back to switch expression
            (r'Component title = \s*if \(resultCode instanceof SUCCESS, UPGRADE_NEEDED, CONNECTION_ISSUE, TEMPORARY_UNAVAILABLE, GENERIC_ERROR\) \{\s*null;\s*\}\s*else \{\s*throw new IllegalStateException\("Unexpected value"\)\s*\}',
             'Component title = switch (resultCode) {\n'
             '            case SUCCESS, UPGRADE_NEEDED, CONNECTION_ISSUE, TEMPORARY_UNAVAILABLE, GENERIC_ERROR -> null;\n'
             '            default -> throw new IllegalStateException("Unexpected value");\n'
             '        };'),
        ],
        "net/minecraft/client/gui/navigation/ScreenDirection.java": [
            (r'if \(this instanceof UP, DOWN\)', 'if (this == UP || this == DOWN)'),
            (r'else if \(this instanceof LEFT, RIGHT\)', 'else if (this == LEFT || this == RIGHT)'),
            (r'if \(this instanceof UP, LEFT\)', 'if (this == UP || this == LEFT)'),
            (r'else if \(this instanceof DOWN, RIGHT\)', 'else if (this == DOWN || this == RIGHT)'),
        ],
        "net/minecraft/core/Direction.java": [
            (r'if \(this instanceof X, Z\)', 'if (this == X || this == Z)'),
        ],
        "net/minecraft/world/item/BrushItem.java": [
            (r'if \(hitDirection instanceof DOWN, UP\)', 'if (hitDirection == DOWN || hitDirection == UP)'),
        ],
        "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate.java": [
            (r'if \(rotation instanceof COUNTERCLOCKWISE_90, CLOCKWISE_90\)', 'if (rotation == COUNTERCLOCKWISE_90 || rotation == CLOCKWISE_90)'),
        ],
        "net/minecraft/world/level/levelgen/structure/StructurePiece.java": [
            (r'if \(orientation instanceof NORTH, SOUTH\)', 'if (orientation == NORTH || orientation == SOUTH)'),
            (r'if \(orientation instanceof WEST, EAST\)', 'if (orientation == WEST || orientation == EAST)'),
        ],
        "net/minecraft/world/level/levelgen/densityfunction/generator/SimpleDensityFunction.java": [
            (r'if \(this instanceof BLEND_ALPHA, BLEND_OFFSET\)', 'if (this == BLEND_ALPHA || this == BLEND_OFFSET)'),
        ],
        "net/minecraft/world/level/block/BellBlock.java": [
            (r'if \(attachType instanceof SINGLE_WALL, DOUBLE_WALL\)', 'if (attachType == SINGLE_WALL || attachType == DOUBLE_WALL)'),
        ],
        "net/minecraft/world/level/block/SelectableSlotContainer.java": [
            (r'if \(hitDirection instanceof DOWN, UP\)', 'if (hitDirection == DOWN || hitDirection == UP)'),
        ],
        "net/minecraft/world/level/block/DoorBlock.java": [
            (r'if \(type instanceof LAND, AIR\)', 'if (type == LAND || type == AIR)'),
        ],
        "net/minecraft/world/level/block/CrafterBlock.java": [
            (r'Direction verticalDirection = \s*if \(nearestLookingDirection instanceof NORTH, SOUTH, WEST, EAST\)', 
             'Direction verticalDirection = switch (nearestLookingDirection) {\n'
             '            case NORTH, SOUTH, WEST, EAST -> Direction.DOWN;\n'
             '            default -> Direction.UP;\n'
             '        };'),
        ],
        "net/minecraft/client/gui/screens/recipebook/FurnaceRecipeBookComponent.java": [
            (r'if \(slot\.index instanceof 0, 1, 2\) \{\s*return true;\s*\}\s*else \{\s*false\s*\}',
             'return slot.index == 0 || slot.index == 1 || slot.index == 2;'),
        ],
    }

    base_path = Path("/home/runner/work/mcJava/mcJava/android/app/src/main/java")
    fixed_count = 0

    for rel_path, replacements in fixes.items():
        filepath = base_path / rel_path
        if filepath.exists():
            if fix_file(filepath, replacements):
                fixed_count += 1
        else:
            print(f"File not found: {filepath}")

    print(f"\nFixed {fixed_count} files")


if __name__ == '__main__':
    main()