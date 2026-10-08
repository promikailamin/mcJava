# Minecraft Java 26.3 — Android port (pro.minecraft)

An experimental port of **Minecraft Java Edition 26.3** (with the Fabric Loader 0.19.5
source tree kept for reference) onto **Android / OpenGL ES 3.2**.

The game logic is the real decompiled client. The desktop-native layer it depends on
(LWJGL, SDL3, GLX/GLFW-style windowing, OpenAL, stb, shaderc, SPIRV-Cross, VMA/Vulkan)
is replaced by a pure-Java **shim layer** that talks to Android's `SurfaceView` + EGL
and `AudioTrack`.

> Status: **scaffold**. The project is laid out so the full source set compiles and the
> game boots to window/GL initialization. Rendering beyond that (shader
> compilation, the framebuffer pipeline) still needs the native libraries described in
> [NDK roadmap](#ndk-roadmap).

---

## Layout

- `android/` — Android Gradle Project.
  - `android/app/src/main/java/net/minecraft/` — decompiled client (Minecraft 26.3).
  - `android/app/src/main/java/com/mojang/` — decompiled Blaze3D / Realms / RenderPearl.
  - `android/app/src/main/java/net/fabricmc/` — Fabric Loader 0.19.5 sources (reference only, **not compiled**).
  - `android/app/src/main/java/org/lwjgl/` — **authored** LWJGL shim (GL33C→GLES 3.2 bridge,
    SDL3 emulation, OpenAL emulation, stb, shaderc/spvc stubs, vulkan/vma stubs).
  - `android/app/src/main/java/pro/minecraft/` — **authored** Android host: `main_activity`,
    `host/game_view` (SurfaceView), `host/game_renderer` (EGL owner),
    `launch/client_launcher` (engine thread + boot), `launch/fabric_bootstrap`.
  - `android/app/src/main/resources/version.json` — version metadata consumed by
    `DetectedVersion` at boot.
- `mcsrc/` — the original Vineflower decompile workspace used to produce `net/` + `com/`.
- `fabric-loader-0.19.5.jar` / `vineflower-1.12.0.jar` — tool jar inputs.

House rule: everything **authored** for this Android port uses `snake_case` file and
member names (`game_view`, `client_launcher`, `set_swap_function`); upstream `net.minecraft`
/ `com.mojang` / LWJGL API names keep their original (camelCase) names so diffs stay small.

## Building

Requires: Android Studio / AGP 8.x, `compileSdk 35`, JDK 17.

```
cd android
./gradlew :app:assembleDebug
```

Notes:
- `multiDexEnabled true`; `desugar_jdk_libs 2.1.2` is on for `java.time`/collections.
- The following source subtrees are **excluded** from compilation because their JVM-only
  dependencies are not on Android (each has a small port edit that keeps callers compiling):
  - `net/fabricmc/**` (needs Mixin / TinyRemapper / ASM — a future step).
  - `com/mojang/renderpearl/backend/vulkan/**` (needs VMA/Vulkan natives).
  - `net/minecraft/util/profiling/jfr/{event,parse,serialize,stats}/**` + `JfrProfiler.java`
    (needs `jdk.jfr`; `JvmProfiler.INSTANCE` is hard-wired to the no-op profiler).
  - `net/minecraft/server/gui/**` (`javax.swing`); `net/minecraft/util/monitoring/jmx/**`
    (`javax.management`); `DedicatedServer` has the GUI/JMX hooks removed.
- Shims/stubs shipped in-tree replace desktop imports:
  - `javax/sound/sampled/AudioFormat.java` (data holder for `JOrbisAudioStream`).
  - `com/mojang/blaze3d/platform/MacosUtil.java` (Objective-C bridge removed).
  - `net/minecraft/client/multiplayer/resolver/ServerRedirectHandler.java` (SRV via JNDI
    disabled).
  - `net/minecraft/server/network/PlayerSafetyServiceTextFilter.java` (MSAL OAuth removed).

## Bundling the asset index

The launcher needs Minecraft's files. No client jars are bundled/modified; provide the
asset index from your own Mojang installation:

1. From a desktop install of 26.3, locate `assets/indexes/34.json`.
2. Create `android/app/src/main/assets/minecraft_assets.zip` containing at least
   `indexes/34.json` and the `objects/` hash tree, or simply every file under the
   desktop `assets/` folder.
3. On startup the port unpacks that zip into `getFilesDir()/minecraft/assets`
   (`asset_provider`), falling back to asset index `"34"` and empty directories.
4. If no zip is bundled, the game boots with no textures/translations — expected for a
   still-wiring renderer.

## What runs / what doesn't

- Boot sequence (mirrors `Main.java`, on a dedicated **Engine Thread**):
  `tryDetectVersion` → `NativeLibrariesBootstrap` → asset extraction → Tracy setup →
  datafixer warmup → `Bootstrap.bootStrap` → `ClientBootstrap.bootstrap` →
  `RenderSystem.initRenderThread` → `new Minecraft(config)` → `minecraft.run()`.
- The window is a `SurfaceView`; `game_renderer` owns an EGL ES 3.2 context on the engine
  thread; `SDL_GL_SwapWindow` fans into `eglSwapBuffers` + Minecraft's `onFramePresented`.
- `GL33C` implements the GL ARB surface over `GLES32` with handle-based "native" memory
  (`MemoryUtil` arenas — a single choke point a future JNI allocator can replace).
- **Not yet working:** real shader compilation. Shaderc (`glsl shader → SPIR-V`) and
  SPIRV-Cross (`SPIR-V → GLSL`) are stubbed — `Shaderc` compile calls throw
  `UnsupportedOperationException`, `Spvc` returns empty results. The GL ES pipeline
  (RenderPearl) cannot build shaders without them.

## NDK roadmap

The fixed GPU pipeline is Vulkan in RenderPearl with a GLES fallback; Minecraft compiles
sources through **shaderc** and reflects the output through **SPIRV-Cross**. Neither has an
Android binary out of the box:

1. Cross-compile `shaderc` and `SPIRV-Cross` (glslang + SPIRV-Tools) for `arm64-v8a`,
   expose them via JNI, and swap them in behind the existing stubbed interfaces
   (`org/lwjgl/util/shaderc/Shaderc`, `org/lwjgl/util/spvc/Spvc`). The game's call
   surface (exact signatures) is already mirrored there and is the source of truth.
2. Replace `MemoryUtil`'s Java arenas with `malloc`-backed handles (keep `Region[nalloc]`
   transparent aliasing), so `VK.create()` can initialize the Vulkan backend too.
3. Optionally finish Fabric: ship Mixin/TinyRemapper/ASM, implement `KnotClient.launch`,
   and re-enable `net/fabricmc/**`.
4. Wire audio buffers (`OpenAL` emulation already exists) to `AudioTrack`.

## Known compile-surface limits

- Cannot be built/verified on-device (no Android SDK here); the tree is kept in a
  blind-compile-clean state via import audits (all non-Android third-party imports are
  either satisfied by bundled deps, excluded, or shimmed).
- `NativeLibrariesBootstrap` requires non-null for: SDL/GL/AL/Freetype/stb library
  handles and shaderc/spvc `getLibrary()` (stubs return non-null). `VK.create()` throws
  and is caught (`vulkanLoaderAvailable=false`).