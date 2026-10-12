# mcJava Architecture

## Overview

mcJava is a port of Minecraft Java Edition to Android, with Fabric Loader integration for mod support.

## Core Components

### 1. Android Application Layer
- **MainActivity**: Entry point, handles Android lifecycle
- **MinecraftClient**: Core game client, manages game loop and components
- **Native Bridge**: JNI interface for EGL/OpenGL ES context management

### 2. Game Engine
- **GameLoop**: Fixed-timestep game loop (60 FPS target)
- **WorldRenderer**: OpenGL ES 3.0 rendering engine
- **InputManager**: Touch, keyboard, and gamepad input handling
- **ResourceManager**: Asset loading from APK

### 3. Native Layer (C++)
- **EGL Context Management**: Creates and manages OpenGL ES context
- **Asset Manager**: Loads assets from Android AssetManager
- **Input Handler**: Processes native touch/key events

### 4. Minecraft Integration
- **Decompiled Sources**: Minecraft client code decompiled and remapped with Yarn mappings
- **Fabric Loader**: Mod loading platform integrated into Android build
- **Mixin Support**: Runtime bytecode modification for mods

## Data Flow

```
Android System Events
        ↓
MainActivity (Java)
        ↓
MinecraftClient (Java)
        ├── GameLoop → Update/Render
        ├── WorldRenderer → OpenGL ES calls
        ├── InputManager → Touch/Key events
        └── ResourceManager → Asset loading
        ↓
Native Bridge (JNI)
        ↓
EGL/OpenGL ES (Native C++)
        ↓
GPU Rendering
```

## Build Process

1. **Download**: Fetch Minecraft client, Fabric Loader, Yarn mappings
2. **Decompile**: Use Vineflower/Fernflower to decompile Minecraft JAR
3. **Remap**: Apply Yarn mappings using TinyRemapper
4. **Copy**: Move remapped sources to Android project
5. **Build**: Gradle builds Android APK with native libraries

## Threading Model

- **Main Thread**: Android UI, lifecycle callbacks
- **Game Thread**: Game loop (update + render)
- **Render Thread**: OpenGL ES commands (same as game thread in current impl)
- **Native Threads**: EGL context management

## Memory Management

- Java heap: Managed by ART
- Native heap: EGL resources, asset buffers
- Texture memory: GPU memory via OpenGL ES
- Large heap enabled for game assets

## OpenGL ES Requirements

- Version: OpenGL ES 3.0+
- Extensions: 
  - GL_EXT_debug_marker
  - GL_KHR_debug
  - GL_OES_vertex_array_object
  - GL_EXT_texture_filter_anisotropic

## Fabric Integration

- Fabric Loader runs on Android ART
- Mixin subsystem adapted for Android class loading
- Mod JARs loaded from assets or external storage
- Mod config stored in app private storage

## Known Limitations

1. No official Minecraft authentication (offline mode only)
2. Limited to OpenGL ES 3.0 features
3. Touch controls need improvement
4. Performance optimization ongoing
5. World saving/loading not fully implemented
