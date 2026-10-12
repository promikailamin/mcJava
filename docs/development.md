# Development Guide

## Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34
- Android NDK 26+
- Gradle 8.5+
- 8GB+ RAM recommended

## Project Setup

### 1. Clone and Initialize
```bash
git clone <repo-url>
cd mcJava
```

### 2. Download Dependencies
```bash
./scripts/download.sh
```
This downloads:
- Minecraft 1.20.4 client JAR
- Fabric Loader 0.15.11
- Fabric API
- Yarn mappings
- Decompilers (Vineflower/Fernflower)
- TinyRemapper

### 3. Build Android Project
```bash
./scripts/build.sh
```

## Development Workflow

### Adding New Minecraft Classes
1. Decompiled sources are in `minecraft/client/src-mapped/`
2. Copy needed classes to `android/app/src/main/java/`
3. Adapt imports for Android (remove LWJGL, use OpenGL ES)
4. Handle threading differences

### Modifying Rendering
- Edit `WorldRenderer.java` for Java-side rendering
- Edit `mcjava_native.cpp` for native EGL management
- Shaders in `WorldRenderer.initShaders()`

### Adding Native Code
1. Add `.cpp`/`.h` files to `android/app/src/main/cpp/`
2. Update `CMakeLists.txt`
3. Add JNI methods to `MainActivity.java` and `MinecraftClient.java`

### Testing on Device
```bash
# Install debug APK
adb install android/app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s mcJava
```

## Code Style

### Java/Kotlin
- Follow Google Java Style Guide
- Use 4-space indentation
- Max line length: 100 chars

### C++
- Follow Google C++ Style Guide
- Use clang-format
- C++17 standard

### Gradle
- Kotlin DSL (`.kts` files)
- Version catalogs for dependencies

## Debugging

### Native Debugging
```bash
# Enable debug build
./gradlew :app:assembleDebug

# Use lldb
adb shell lldb -- /data/local/tmp/lldb-server platform --listen "*:1234" --server
```

### Java Debugging
- Use Android Studio debugger
- Attach to `com.mcjava.minecraft` process

### OpenGL Debugging
- Enable debug callback in `EGLContextManager`
- Use `adb shell setprop debug.gles.enabled 1`
- Use RenderDoc or GAPID for frame capture

## Performance Profiling

### CPU
```bash
# Simpleperf
adb shell simpleperf record -g -p $(pidof com.mcjava.minecraft) --duration 30
adb pull /data/perf.data
simpleperf report -i perf.data
```

### GPU
- Use Android GPU Inspector (AGI)
- Capture frame and analyze draw calls

### Memory
```bash
# Heap dump
adb shell am dumpheap com.mcjava.minecraft /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof
# Analyze with MAT or Android Studio Profiler
```

## Common Issues

### Build Failures
- Clean: `./gradlew clean`
- Delete `.gradle` and `build` directories
- Check JDK version matches `compileOptions`

### Native Crashes
- Check `adb logcat` for tombstone
- Enable `android:debuggable="true"` in manifest
- Use `ndk-stack` to symbolize crashes

### OpenGL Errors
- Check `glGetError()` after each call
- Validate shader compilation/linking
- Verify EGL context creation

### Out of Memory
- Enable largeHeap in manifest
- Reduce texture resolution
- Implement texture streaming
- Use `BitmapFactory.Options.inSampleSize`

## Contributing

1. Fork repository
2. Create feature branch
3. Make changes
4. Run tests: `./gradlew test`
5. Submit PR

## Legal

- Only for educational/research purposes
- Requires legitimate Minecraft Java Edition license
- Not affiliated with Mojang/Microsoft/FabricMC
