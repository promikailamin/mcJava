#!/usr/bin/env python3
"""
Fix remaining compilation errors from incorrect switch expression conversions.
"""

import re
from pathlib import Path


def fix_file(filepath: Path, replacements: list[tuple[str, str]]) -> bool:
    """Apply replacements to a file."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content, flags=re.MULTILINE | re.DOTALL)
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        print(f"Fixed: {filepath}")
        return True
    return False


def main():
    fixes = {
        "net/minecraft/client/data/models/BlockModelGenerators.java": [
            # Fix the generate lambda for createActiveRail - restore switch expression
            (r'\.with\(PropertyDispatch\.initial\(BlockStateProperties\.POWERED, BlockStateProperties\.RAIL_SHAPE_STRAIGHT\)\.generate\(\(powered, railShape\) -> \{\s*\{\s*throw new UnsupportedOperationException\("Fix you generator!"\)\s*\}\s*\}\)\)',
             '.with(PropertyDispatch.initial(BlockStateProperties.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT).generate((powered, railShape) -> {\n'
             '                   return switch (railShape) {\n'
             '                       case NORTH_SOUTH -> powered ? flatOn : flat;\n'
             '                       case EAST_WEST -> powered ? flatOn : flat;\n'
             '                       case ASCENDING_EAST -> powered ? risingNEOn : risingNE;\n'
             '                       case ASCENDING_WEST -> powered ? risingSWOn : risingSW;\n'
             '                       case ASCENDING_NORTH -> powered ? risingNEOn : risingNE;\n'
             '                       case ASCENDING_SOUTH -> powered ? risingSWOn : risingSW;\n'
             '                       default -> throw new IllegalStateException("Unexpected rail shape: " + railShape);\n'
             '                   };\n'
             '               }))'),
        ],
        "net/minecraft/commands/arguments/OperationArgument.java": [
            # Add missing semicolon after throw
            (r'(throw ERROR_INVALID_OPERATION\.create\(\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/commands/synchronization/ArgumentUtils.java": [
            # Add missing semicolon after result.addProperty
            (r'(result\.addProperty\("type", "unknown"\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/world/attribute/EnvironmentAttributeSystem.java": [
            # Fix computeValuePositional - restore switch expression
            (r'for \(EnvironmentAttributeLayer<Value> layer : this\.layers\) \{\s*result = \(Value\)\(            if \(layer instanceof EnvironmentAttributeLayer\.Constant<Value> constantLayer\) \{\s*constantLayer\.applyConstant\(result\);\s*\}\s*else if \(layer instanceof EnvironmentAttributeLayer\.TimeBased<Value> timeBasedLayer\) \{\s*timeBasedLayer\.applyTimeBased\(result, this\.cacheTickId\);\s*\}\s*else if \(layer instanceof EnvironmentAttributeLayer\.Positional<Value> positionalLayer\) \{\s*positionalLayer\.applyPositional\(\s*result, Objects\.requireNonNull\(pos\)\), biomeInterpolator\s*\);\s*\}\s*else \{\s*throw new IllegalArgumentException\("Unexpected layer type: " \+ layer\)\s*\}\);\s*\}',
             'for (EnvironmentAttributeLayer<Value> layer : this.layers) {\n'
             '            result = (Value) switch (layer) {\n'
             '                case EnvironmentAttributeLayer.Constant<Value> constantLayer -> constantLayer.applyConstant(result);\n'
             '                case EnvironmentAttributeLayer.TimeBased<Value> timeBasedLayer -> timeBasedLayer.applyTimeBased(result, this.cacheTickId);\n'
             '                case EnvironmentAttributeLayer.Positional<Value> positionalLayer -> positionalLayer.applyPositional(\n'
             '                    result, Objects.requireNonNull(pos), biomeInterpolator\n'
             '                );\n'
             '                default -> throw new IllegalArgumentException("Unexpected layer type: " + layer);\n'
             '            };\n'
             '        }'),
            # Fix computeValueNotPositional - restore switch expression
            (r'for \(EnvironmentAttributeLayer<Value> layer : this\.layers\) \{\s*result = \(Value\)\(            if \(layer instanceof EnvironmentAttributeLayer\.Constant<Value> constantLayer\) \{\s*constantLayer\.applyConstant\(result\);\s*\}\s*else if \(layer instanceof EnvironmentAttributeLayer\.TimeBased<Value> timeBasedLayer\) \{\s*timeBasedLayer\.applyTimeBased\(result, this\.cacheTickId\);\s*\}\s*else if \(layer instanceof EnvironmentAttributeLayer\.Positional<Value> ignored\) \{\s*result;\s*\}\s*else \{\s*throw new IllegalArgumentException\("Unexpected layer type: " \+ layer\)\s*\}\);\s*\}',
             'for (EnvironmentAttributeLayer<Value> layer : this.layers) {\n'
             '            result = (Value) switch (layer) {\n'
             '                case EnvironmentAttributeLayer.Constant<Value> constantLayer -> constantLayer.applyConstant(result);\n'
             '                case EnvironmentAttributeLayer.TimeBased<Value> timeBasedLayer -> timeBasedLayer.applyTimeBased(result, this.cacheTickId);\n'
             '                case EnvironmentAttributeLayer.Positional<Value> ignored -> result;\n'
             '                default -> throw new IllegalArgumentException("Unexpected layer type: " + layer);\n'
             '            };\n'
             '        }'),
        ],
        "net/minecraft/world/level/levelgen/material/rule/MaterialRule.java": [
            # Add missing semicolon
            (r'(throw new IllegalArgumentException\("Unexpected holder type: " \+ holder\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/world/level/levelgen/material/condition/MaterialCondition.java": [
            # Add missing semicolon
            (r'(throw new IllegalArgumentException\("Unexpected holder type: " \+ holder\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate.java": [
            # Fix getSize method - restore switch expression
            (r'public Vec3i getSize\(final Rotation rotation\) \{\s*if \(rotation == COUNTERCLOCKWISE_90 \|\| rotation == CLOCKWISE_90\) \{\s*return new Vec3i\(this\.size\.getZ\(\), this\.size\.getY\(\), this\.size\.getX\(\)\);\s*\}\s*else \{\s*this\.size\s*\}\s*\}',
             'public Vec3i getSize(final Rotation rotation) {\n'
             '        return switch (rotation) {\n'
             '            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());\n'
             '            default -> this.size;\n'
             '        };\n'
             '    }'),
        ],
        "net/minecraft/world/level/levelgen/structure/StructurePiece.java": [
            # Fix getWorldX method
            (r'protected int getWorldX\(final int x, final int z\) \{\s*Direction orientation = this\.getOrientation\(\);\s*if \(orientation == null\) \{\s*return x;\s*\}\s*if \(orientation == NORTH \|\| orientation == SOUTH\) \{\s*return this\.boundingBox\.minX\(\) \+ x;\s*\}\s*else \{\s*x\s*\}\s*\}',
             'protected int getWorldX(final int x, final int z) {\n'
             '        Direction orientation = this.getOrientation();\n'
             '        if (orientation == null) {\n'
             '            return x;\n'
             '        }\n'
             '        return switch (orientation) {\n'
             '            case NORTH, SOUTH -> this.boundingBox.minX() + x;\n'
             '            case WEST, EAST -> x;\n'
             '            default -> x;\n'
             '        };\n'
             '    }'),
            # Fix getWorldZ method
            (r'protected int getWorldZ\(final int x, final int z\) \{\s*Direction orientation = this\.getOrientation\(\);\s*if \(orientation == null\) \{\s*return z;\s*\}\s*if \(orientation == WEST \|\| orientation == EAST\) \{\s*return this\.boundingBox\.minZ\(\) \+ x;\s*\}\s*else \{\s*z\s*\}\s*\}',
             'protected int getWorldZ(final int x, final int z) {\n'
             '        Direction orientation = this.getOrientation();\n'
             '        if (orientation == null) {\n'
             '            return z;\n'
             '        }\n'
             '        return switch (orientation) {\n'
             '            case WEST, EAST -> this.boundingBox.minZ() + z;\n'
             '            case NORTH, SOUTH -> z;\n'
             '            default -> z;\n'
             '        };\n'
             '    }'),
        ],
        "net/minecraft/world/level/levelgen/densityfunction/DensityFunction.java": [
            # Add missing semicolon
            (r'(throw new IllegalArgumentException\("Unexpected holder type: " \+ holder\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/world/level/block/BellBlock.java": [
            # Fix isProperHit method - add return in else branch
            (r'if \(attachType == SINGLE_WALL \|\| attachType == DOUBLE_WALL\) \{\s*return facing\.getAxis\(\) != clickedDirection\.getAxis\(\);\s*\}\s*else \{\s*false\s*\}',
             'if (attachType == SINGLE_WALL || attachType == DOUBLE_WALL) {\n'
             '              return facing.getAxis() != clickedDirection.getAxis();\n'
             '          } else {\n'
             '              return false;\n'
             '          }'),
        ],
        "net/minecraft/util/CubicSpline.java": [
            # Add missing semicolons after throw statements
            (r'(throw new IllegalStateException\("Unexpected value"\))(\s*[^;])', r'\1;\2'),
        ],
        "net/minecraft/util/datafix/fixes/EntitySpawnerItemVariantComponentFix.java": [
            # Fix the input statement
            (r'input(\s*[^;])', r'return input;\1'),
        ],
        "net/minecraft/util/datafix/fixes/TextComponentHoverAndClickEventFix.java": [
            # Fix the dynamic statement
            (r'dynamic(\s*[^;])', r'return dynamic;\1'),
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