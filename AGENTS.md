# AGENTS.md

This file provides guidance to AI agents working on the mcJava project.

## Project Overview

mcJava is a project to port Minecraft Java Edition with Fabric Loader to Android. The goal is to create a playable Minecraft Java Edition experience on Android devices using OpenGL ES.

## Repository Structure

```
/home/runner/work/mcJava/mcJava/
├── android/                 # Android project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/        # Java/Kotlin source code
│   │   │   ├── assets/      # Game assets
│   │   │   ├── jniLibs/     # Native libraries
│   │   │   └── res/         # Android resources
│   │   ├── build.gradle.kts
│   │   └── ...
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── gradle/wrapper/
├── minecraft/               # Minecraft Java Edition source
│   ├── client/              # Client source code
│   ├── server/              # Server source code
│   ├── assets/              # Game assets
│   └── libraries/           # Dependencies
├── fabric/                  # Fabric Loader and API
│   ├── loader/              # Fabric Loader source
│   ├── api/                 # Fabric API
│   └── loom/                # Loom Gradle plugin
├── tools/                   # Build and decompilation tools
│   ├── decompiler/          # Decompilation tools
│   ├── mappings/            # MCP/Yarn mappings
│   ├── gradle/              # Custom Gradle plugins
│   └── scripts/             # Utility scripts
├── docs/                    # Documentation
├── scripts/                 # Build and setup scripts
└── build/                   # Build output
```

## Development Guidelines

1. **Code Style**: Follow standard Java/Android conventions
2. **OpenGL**: Use OpenGL ES 3.0+ for rendering
3. **Threading**: Main thread for UI, separate threads for game logic
4. **Memory**: Be mindful of mobile memory constraints
5. **Performance**: Optimize for mobile GPUs

## Key Components

### Minecraft Client Port
- Port `net.minecraft.client` to Android
- Adapt LWJGL to Android OpenGL ES
- Handle touch input vs mouse/keyboard
- Manage Android lifecycle (pause/resume)

### Fabric Loader Integration
- Port Fabric Loader to Android
- Ensure mixin compatibility
- Handle mod loading on Android

### Android Integration
- Native activity or GameActivity
- EGL/OpenGL ES context management
- Asset loading from APK
- Input handling (touch, gamepad, keyboard)

## Build System

- Gradle with Kotlin DSL
- Custom plugins for decompilation
- NDK for native code
- ProGuard/R8 for optimization

## Testing

- Unit tests for core logic
- Instrumented tests for Android components
- Manual testing on devices/emulators

## Important Notes

- This is a complex porting project
- Many Minecraft internals need adaptation
- Legal: Only for educational/research purposes
- Requires owning Minecraft Java Edition