#!/usr/bin/env python3
"""
Comprehensive fix for remaining compilation errors.
Focuses on:
1. Pattern matching instanceof without pattern variable
2. Missing variable declarations in equals() and other methods
3. Type casting issues
"""

import re
from pathlib import Path


def fix_instanceof_patterns(content):
    """Fix instanceof patterns like 'obj instanceof Type && var.field' to 'obj instanceof Type var && var.field'"""
    
    # Pattern: return ... instanceof Type && varName.field ...
    def fix_return_instanceof(match):
        full = match.group(0)
        # Find the variable name used after &&
        var_match = re.search(r'&&\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\.', full)
        if var_match:
            var_name = var_match.group(1)
            # Find the instanceof part
            inst_match = re.search(r'(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', full)
            if inst_match:
                obj = inst_match.group(1)
                type_name = inst_match.group(2)
                if var_name not in ('this', obj, 'o', 'that', 'other'):
                    return re.sub(
                        rf'(\b{re.escape(obj)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        full
                    )
        return full
    
    # Apply to return statements
    content = re.sub(
        r'return\s+[^;]*instanceof\s+[A-Z][a-zA-Z0-9_<>]*\s+&&[^;]*;',
        fix_return_instanceof,
        content
    )
    
    # Pattern: if (obj instanceof Type && varName.field ...) {
    def fix_if_instanceof(match):
        full = match.group(0)
        var_match = re.search(r'&&\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\.', full)
        if var_match:
            var_name = var_match.group(1)
            inst_match = re.search(r'if\s*\((\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', full)
            if inst_match:
                obj = inst_match.group(1)
                type_name = inst_match.group(2)
                if var_name not in ('this', obj, 'o', 'that', 'other'):
                    return re.sub(
                        rf'(\b{re.escape(obj)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        full
                    )
        return full
    
    content = re.sub(
        r'if\s*\([^)]*instanceof\s+[A-Z][a-zA-Z0-9_<>]*\s+&&[^)]*\)',
        fix_if_instanceof,
        content
    )
    
    # Pattern: variable = obj instanceof Type && varName.field ...
    def fix_assign_instanceof(match):
        full = match.group(0)
        var_match = re.search(r'&&\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\.', full)
        if var_match:
            var_name = var_match.group(1)
            inst_match = re.search(r'=\s*(\w+)\s+instanceof\s+([A-Z][a-zA-Z0-9_<>]*)\s+&&', full)
            if inst_match:
                obj = inst_match.group(1)
                type_name = inst_match.group(2)
                if var_name not in ('this', obj, 'o', 'that', 'other'):
                    return re.sub(
                        rf'(\b{re.escape(obj)}\s+instanceof\s+{re.escape(type_name)})\s+&&',
                        rf'\1 {var_name} &&',
                        full
                    )
        return full
    
    content = re.sub(
        r'=\s*[^;]*instanceof\s+[A-Z][a-zA-Z0-9_<>]*\s+&&[^;]*;',
        fix_assign_instanceof,
        content
    )
    
    return content


def fix_specific_files(content, filepath):
    """Fix specific known issues in specific files"""
    
    # MobEffectInstance.java - fix equals() method
    if 'MobEffectInstance.java' in filepath:
        content = re.sub(
            r'this\.duration == that\.duration',
            'this.duration == other.duration',
            content
        )
        content = re.sub(
            r'this\.amplifier == that\.amplifier',
            'this.amplifier == other.amplifier',
            content
        )
        content = re.sub(
            r'this\.ambient == that\.ambient',
            'this.ambient == other.ambient',
            content
        )
        content = re.sub(
            r'this\.visible == that\.visible',
            'this.visible == other.visible',
            content
        )
        content = re.sub(
            r'this\.showIcon == that\.showIcon',
            'this.showIcon == other.showIcon',
            content
        )
        content = re.sub(
            r'this\.effect\.equals\(that\.effect\)',
            'this.effect.equals(other.effect)',
            content
        )
        # Fix the instanceof pattern
        content = re.sub(
            r'obj instanceof MobEffectInstance &&',
            'obj instanceof MobEffectInstance other &&',
            content
        )
    
    # AdvancementNode.java
    if 'AdvancementNode.java' in filepath:
        content = re.sub(
            r'obj instanceof AdvancementNode && this\.holder\.equals\(that\.holder\)',
            'obj instanceof AdvancementNode that && this.holder.equals(that.holder)',
            content
        )
    
    # MobEffectsPredicate.java
    if 'MobEffectsPredicate.java' in filepath:
        content = re.sub(
            r'entity instanceof LivingEntity && this\.matches\(living\.getActiveEffectsMap\(\)\)',
            'entity instanceof LivingEntity living && this.matches(living.getActiveEffectsMap())',
            content
        )
    
    # WorldDimensions.java - fix pattern matching in if statements
    if 'WorldDimensions.java' in filepath:
        content = re.sub(
            r'dimension\.generator\(\) instanceof MultiNoiseBiomeSource && !biomeSource\.stable',
            'dimension.generator() instanceof MultiNoiseBiomeSource biomeSource && !biomeSource.stable',
            content
        )
        content = re.sub(
            r'dimension\.generator\(\) instanceof NoiseBasedChunkGenerator && generator\.stable',
            'dimension.generator() instanceof NoiseBasedChunkGenerator generator && generator.stable',
            content
        )
        content = re.sub(
            r'generator\.getBiomeSource\(\) instanceof MultiNoiseBiomeSource && biomeSource\.stable',
            'generator.getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource && biomeSource.stable',
            content
        )
        content = re.sub(
            r'dimension\.generator\(\) instanceof NoiseBasedChunkGenerator && generator\.stable\(NoiseGeneratorSettings\.END\)',
            'dimension.generator() instanceof NoiseBasedChunkGenerator generator && generator.stable(NoiseGeneratorSettings.END)',
            content
        )
        content = re.sub(
            r'generator\.getBiomeSource\(\) instanceof TheEndBiomeSource',
            'generator.getBiomeSource() instanceof TheEndBiomeSource',
            content
        )
    
    # PotionContents.java
    if 'PotionContents.java' in filepath:
        content = re.sub(
            r'Player player = entity instanceof Player \? playerEntity\s*:\s*null',
            'Player player = entity instanceof Player playerEntity ? playerEntity : null',
            content
        )
    
    # HashedStack.java
    if 'HashedStack.java' in filepath:
        content = re.sub(
            r'hashedStack -> hashedStack instanceof HashedStack\.ActualItem \? Optional\.of\(actualItem\)\s*:\s*Optional\.empty\(\)',
            'hashedStack -> hashedStack instanceof HashedStack.ActualItem actualItem ? Optional.of(actualItem) : Optional.empty()',
            content
        )
    
    # ServerboundSetCommandMinecartPacket.java
    if 'ServerboundSetCommandMinecartPacket.java' in filepath:
        content = re.sub(
            r'return level\.getEntity\(this\.entity\) instanceof MinecartCommandBlock \? getCommandBlock\(\) : null;',
            'return level.getEntity(this.entity) instanceof MinecartCommandBlock mc ? getCommandBlock(level) : null;',
            content
        )
    
    # TestInstanceBlockEntity.java
    if 'TestInstanceBlockEntity.java' in filepath:
        content = re.sub(
            r'return this\.test\(\)\.map\(key -> Component\.literal\(key\.identifier\(\)\.toString\(\)\)\)\.orElse\(INVALID_TEST_NAME\);',
            'return this.test().map(key -> Component.literal(key.identifier().toString())).orElse(INVALID_TEST_NAME);',
            content
        )
        content = re.sub(
            r'return !saved\.isEmpty\(\) && this\.level instanceof ServerLevel \? export\(serverLevel, saved\.get\(\), feedbackOutput\)\s*:\s*false;',
            'return !saved.isEmpty() && this.level instanceof ServerLevel serverLevel ? export(serverLevel, saved.get(), feedbackOutput) : false;',
            content
        )
    
    # ProfilePublicKey.java - record equals() method
    if 'ProfilePublicKey.java' in filepath:
        content = re.sub(
            r': this\.expiresAt\.equals\(data\.expiresAt\) && this\.key\.equals\(data\.key\) && Arrays\.equals\(this\.keySignature, data\.keySignature\)',
            ': this.expiresAt.equals(other.expiresAt) && this.key.equals(other.key) && Arrays.equals(this.keySignature, other.keySignature)',
            content
        )
        content = re.sub(
            r'obj instanceof ProfilePublicKey &&',
            'obj instanceof ProfilePublicKey other &&',
            content
        )
    
    # Scoreboard.java
    if 'Scoreboard.java' in filepath:
        content = re.sub(
            r'\.forEach\(o -> operation\.accept\(this\.getOrCreatePlayerScore\(name, o, true\)\)\)',
            '.forEach(o -> operation.accept(this.getOrCreatePlayerScore(name, (Objective)o, true)))',
            content
        )
    
    # ChunkGenerator.java
    if 'ChunkGenerator.java' in filepath:
        content = re.sub(
            r'this\(biomeSource, biome -> biome\.value\(\)\.getGenerationSettings\(\)\);',
            'this(biomeSource, biome -> biome.value().generationSettings());',
            content
        )
    
    # RecipeManager.java
    if 'RecipeManager.java' in filepath:
        content = re.sub(
            r'recipe -> recipe instanceof SmithingRecipe \? additionIngredient\(\) : Optional\.empty\(\),',
            'recipe -> recipe instanceof SmithingRecipe sr ? sr.additionIngredient() : Optional.empty(),',
            content
        )
        content = re.sub(
            r'recipe -> recipe instanceof SmithingRecipe \? templateIngredient\(\) : Optional\.empty\(\),',
            'recipe -> recipe instanceof SmithingRecipe sr ? sr.templateIngredient() : Optional.empty(),',
            content
        )
    
    return content


def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    content = fix_instanceof_patterns(content)
    content = fix_specific_files(content, str(filepath))
    
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