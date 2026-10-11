# AGENTS.md - mcJava Android Port

## Project Overview
This is an experimental port of **Minecraft Java Edition 26.3** (with Fabric Loader 0.19.5) onto **Android / OpenGL ES 3.2**. The game logic is the real decompiled client. The desktop-native layer (LWJGL, SDL3, GLX/GLFW, OpenAL, etc.) is replaced by a pure-Java shim layer.

## Repository Structure
- `android/` - Android Gradle Project
  - `android/app/src/main/java/net/minecraft/` - Decompiled client (Minecraft 26.3)
  - `android/app/src/main/java/com/mojang/` - Decompiled Blaze3D / Realms / RenderPearl
  - `android/app/src/main/java/net/fabricmc/` - Fabric Loader sources (reference only, NOT compiled)
  - `android/app/src/main/java/org/lwjgl/` - Authored LWJGL shim (GL33C→GLES 3.2 bridge, SDL3 emulation, etc.)
  - `android/app/src/main/java/pro/minecraft/` - Authored Android host (main_activity, game_view, game_renderer, client_launcher, fabric_bootstrap)
- `matchexception/` - Custom MatchException class for Java 21 pattern matching on sealed types

## Build Requirements
- Android Studio / AGP 8.5.2
- `compileSdk 35`
- JDK 21 (sources use record patterns / pattern-matching switch)
- `multiDexEnabled true`

## Build Command
```bash
cd android
./gradlew :app:assembleDebug
```

## Current Issues (GitHub Issue #27)

### Primary Issue: MatchException Missing
The Android build fails because `java.lang.MatchException` is not available in Android's JDK. This class is required by the Java 21 compiler when compiling pattern matching switches on sealed types.

**Solution Implemented**: Custom `matchexception.jar` added via `--patch-module=java.base` and `-Xbootclasspath/a` in the JavaCompile task configuration.

### Secondary Issues: Decompiled Code Bugs
The decompiled source contains numerous bugs that prevent compilation:
1. **Incorrect pattern matching instanceof syntax**: Code uses `o instanceof Type` but references pattern variables that don't exist (e.g., `o instanceof Vec3i && ... vec3i.getX()`)
2. **Missing return statements**: Methods with incomplete control flow
3. **Pattern matching switch on sealed types**: 150+ files with switch expressions that may need conversion

## Fix Strategy

### 1. MatchException (DONE)
Custom JAR with `java.lang.MatchException` patched into `java.base` module via compiler arguments.

### 2. Pattern Matching instanceof Fixes (IN PROGRESS)
Fix incorrect instanceof patterns:
- `o instanceof Type && ... varName.method()` → `o instanceof Type varName && ... varName.method()`

### 3. Missing Return Statements
Add missing return statements in methods with incomplete control flow.

### 4. Pattern Matching Switch on Sealed Types
If compilation fails due to MatchException still, convert all pattern matching switches on sealed types to if-else chains using automated tooling (OpenRewrite/Error Prone scripts available in repo root).

## Excluded Source Subtrees
The following are excluded from compilation (JVM-only dependencies):
- `net/fabricmc/**` (needs Mixin/TinyRemapper/ASM)
- `com/mojang/renderpearl/backend/vulkan/**` (needs VMA/Vulkan natives)
- `net/minecraft/util/profiling/jfr/**` (needs jdk.jfr)
- `net/minecraft/server/gui/**` (javax.swing)
- `net/minecraft/util/monitoring/jmx/**` (javax.management)
- `com/mojang/blaze3d/**`, `net/minecraft/client/**`, `org/lwjgl/**`, etc.

## Development Workflow
1. Run `./gradlew :app:compileDebugJavaWithJavac` to see compilation errors
2. Fix errors in source files under `android/app/src/main/java/`
3. Repeat until clean compilation
4. Run full build `./gradlew :app:assembleDebug`

## Available Fix Scripts
Root directory contains Python scripts for automated fixes:
- `fix_pattern_matching*.py` - Fix pattern matching instanceof/switch issues
- `rewrite_switches*.py` - Convert switch expressions to if-else
- `find_pattern_switches.py` - Find pattern matching switches
- `auto_fix.sh` - Automated fix runner

## Coding Conventions
- **Authored code** (pro/minecraft/, org/lwjgl/): snake_case for files and members
- **Upstream code** (net/minecraft/, com.mojang/): Keep original camelCase for minimal diffs
- No emojis in code/comments unless explicitly requested

## Verification
- Run lint/typecheck if available (check for `npm run lint`, `./gradlew check`, etc.)
- Do NOT commit changes unless explicitly asked
- Keep temporary files inside repository workspace