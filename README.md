# mcJava

Port of Minecraft Java Edition with Fabric Loader to Android.

## Overview

This project aims to bring Minecraft Java Edition to Android devices by porting the client code and integrating Fabric Loader for mod support. The port uses OpenGL ES for rendering and adapts the Java codebase to run on Android's ART runtime.

## Features

- Minecraft Java Edition client ported to Android
- Fabric Loader integration for mod support
- OpenGL ES 3.0+ rendering
- Touch input handling
- Android lifecycle management

## Project Structure

```
mcJava/
├── android/          # Android project
├── minecraft/        # Minecraft Java Edition source (decompiled)
├── fabric/           # Fabric Loader and API
├── tools/            # Build and decompilation tools
├── docs/             # Documentation
├── scripts/          # Build and setup scripts
└── build/            # Build output
```

## Requirements

- Android Studio / Gradle 8.x
- JDK 17+
- Android NDK (for native code)
- Minecraft Java Edition (owned legally)
- 4GB+ RAM for build process

## Setup

1. Clone this repository
2. Run the setup script: `./scripts/setup.sh`
3. Build the project: `./gradlew assembleDebug`

## Building

```bash
# Configure Gradle
./gradlew setupDecompWorkspace

# Build Android APK
./gradlew :android:app:assembleDebug
```

## Legal Notice

This project is for educational and research purposes only. You must own a legitimate copy of Minecraft Java Edition to use this port. This project is not affiliated with Mojang Studios, Microsoft, or FabricMC.

## License

See LICENSE file for details.