# mcJava Build Tools

This folder contains custom compiler fix scripts for the mcJava Android port. These scripts automatically fix common compilation issues in the decompiled Minecraft Java Edition 26.3 source code.

## Scripts Overview

### Pattern Matching Fixes
- `fix_all_patterns.py` - Comprehensive fix for pattern matching instanceof issues
- `fix_comprehensive_instanceof.py` - Fixes complex instanceof patterns
- `fix_invalid_instanceof.py` - Fixes invalid instanceof syntax
- `fix_missing_pattern_vars.py` - Adds missing pattern variables
- `fix_missing_pattern_vars2.py` - Additional pattern variable fixes
- `fix_remaining_patterns.py` - Fixes remaining pattern matching issues
- `fix_remaining_errors.py` - Fixes remaining compilation errors
- `fix_specific_files.py` - Fixes for specific known problematic files

### Switch Expression Fixes
- `rewrite_switches.py` - Rewrites pattern matching switch expressions to if-else chains
- `rewrite_switches_v2.py` through `rewrite_switches_v8.py` - Improved versions
- `rewrite_switches_regex.py` - Regex-based switch rewriting
- `rewrite_switches_opt.py` - Optimized switch rewriting
- `rewrite_all_switches.py` - Comprehensive switch rewriting

### Specialized Fixes
- `fix_living_entity.py` - Fixes for LivingEntity.java
- `fix_pattern_matching.py` through `fix_pattern_matching6.py` - Pattern matching fixes
- `fix_pattern_matching.py` - Basic pattern matching fixes
- `fix_patterns_v2.py` - Pattern fixes version 2
- `fix_complex_patterns.py` - Complex pattern fixes
- `fix_pattern_switches.py` - Pattern switch fixes
- `reapply_core_fixes.py` - Reapplies core fixes after updates

### Debug Scripts
- `debug_replace.py` - Debug replacement patterns
- `debug_switch.py` through `debug_switch4.py` - Debug switch rewriting
- `find_pattern_switches.py` - Finds pattern matching switches

## Usage

### Automated Build
```bash
./build.sh
```

### Manual Fix Application
```bash
# Fix pattern matching instanceof issues
python3 fix_all_patterns.py

# Fix switch expressions
python3 rewrite_switches_v8.py

# Fix remaining errors
python3 fix_remaining_errors.py
```

### Build Android Project
```bash
cd ../android
./gradlew :app:assembleDebug
```

## Known Issues Fixed

1. **Pattern Matching instanceof**: `expr instanceof Type && var.method()` → `expr instanceof Type var && var.method()`
2. **Switch Expressions**: Pattern matching switches on sealed types converted to if-else chains
3. **Missing Classes**: Stubs for TimeMarkerInfo, Biome.Precipitation, Biome.BiomeBuilder, etc.
4. **Method References**: Fixed broken method references (`this : :method` → `this::method`)
5. **Codec Issues**: Fixed RecordCodecBuilder group() arity limits
6. **StreamCodec**: Added missing STREAM_CODEC implementations

## Requirements

- Python 3.8+
- Android Studio / AGP 8.5.2
- JDK 21
- Gradle 9.1.0

## Notes

- These scripts modify source files in `../android/app/src/main/java/`
- Always run from the repository root
- Backup your changes before running scripts
- Some scripts may need to be run multiple times for full coverage