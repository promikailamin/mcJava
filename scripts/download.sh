#!/bin/bash
# Download script for Minecraft Java Edition and Fabric Loader

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration
MINECRAFT_VERSION="1.20.4"
FABRIC_LOADER_VERSION="0.15.11"
FABRIC_API_VERSION="0.91.0+1.20.4"
YARN_MAPPINGS_VERSION="1.20.4+build.1"

# Directories
MC_DIR="$PROJECT_ROOT/minecraft"
FABRIC_DIR="$PROJECT_ROOT/fabric"
TOOLS_DIR="$PROJECT_ROOT/tools"
DECOMPILER_DIR="$TOOLS_DIR/decompiler"
MAPPINGS_DIR="$TOOLS_DIR/mappings"

mkdir -p "$MC_DIR"/{client,server,assets,libraries}
mkdir -p "$FABRIC_DIR"/{loader,api,loom}
mkdir -p "$DECOMPILER_DIR"
mkdir -p "$MAPPINGS_DIR"

echo "=== mcJava Download Script ==="
echo "Minecraft version: $MINECRAFT_VERSION"
echo "Fabric Loader version: $FABRIC_LOADER_VERSION"
echo "Fabric API version: $FABRIC_API_VERSION"
echo "Yarn mappings version: $YARN_MAPPINGS_VERSION"
echo ""

# Check dependencies
check_deps() {
    for cmd in java curl jq unzip; do
        if ! command -v $cmd &> /dev/null; then
            echo "Error: $cmd is required but not installed"
            exit 1
        fi
    done
}

# Download Minecraft client
download_minecraft() {
    echo "Downloading Minecraft $MINECRAFT_VERSION client..."
    
    MANIFEST_URL="https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
    
    echo "Fetching version manifest..."
    VERSION_JSON=$(curl -s "$MANIFEST_URL" | jq -r ".versions[] | select(.id == \"$MINECRAFT_VERSION\") | .url")
    
    if [ -z "$VERSION_JSON" ] || [ "$VERSION_JSON" = "null" ]; then
        echo "Error: Could not find version $MINECRAFT_VERSION"
        return 1
    fi
    
    echo "Version info URL: $VERSION_JSON"
    VERSION_INFO=$(curl -s "$VERSION_JSON")
    CLIENT_URL=$(echo "$VERSION_INFO" | jq -r '.downloads.client.url')
    CLIENT_SHA1=$(echo "$VERSION_INFO" | jq -r '.downloads.client.sha1')
    
    echo "Downloading client from: $CLIENT_URL"
    curl -L -o "$MC_DIR/client/minecraft-$MINECRAFT_VERSION.jar" "$CLIENT_URL"
    
    # Verify SHA1
    DOWNLOADED_SHA1=$(sha1sum "$MC_DIR/client/minecraft-$MINECRAFT_VERSION.jar" | cut -d' ' -f1)
    if [ "$DOWNLOADED_SHA1" != "$CLIENT_SHA1" ]; then
        echo "Warning: SHA1 mismatch! Expected: $CLIENT_SHA1, Got: $DOWNLOADED_SHA1"
    else
        echo "SHA1 verified successfully"
    fi
    
    # Download libraries
    echo "Downloading libraries..."
    echo "$VERSION_INFO" | jq -r '.libraries[] | select(.downloads.artifact != null) | "\(.downloads.artifact.url) \(.downloads.artifact.path) \(.downloads.artifact.sha1)"' | while read url path sha1; do
        mkdir -p "$MC_DIR/libraries/$(dirname "$path")"
        echo "  Downloading $path..."
        curl -L -o "$MC_DIR/libraries/$path" "$url" || echo "  Failed to download $path"
    done
}

# Download Fabric Loader
download_fabric() {
    echo "Downloading Fabric Loader $FABRIC_LOADER_VERSION..."
    
    FABRIC_API_URL="https://meta.fabricmc.net/v2/versions/loader/$MINECRAFT_VERSION/$FABRIC_LOADER_VERSION/profile/json"
    
    echo "Fetching Fabric profile..."
    FABRIC_PROFILE=$(curl -s "$FABRIC_API_URL")
    
    LOADER_URL=$(echo "$FABRIC_PROFILE" | jq -r '.libraries[] | select(.name | contains("fabric-loader")) | .url' | head -1)
    
    if [ -n "$LOADER_URL" ] && [ "$LOADER_URL" != "null" ]; then
        echo "Downloading Fabric Loader from: $LOADER_URL"
        curl -L -o "$FABRIC_DIR/loader/fabric-loader-$FABRIC_LOADER_VERSION.jar" "$LOADER_URL"
    else
        echo "Warning: Could not find Fabric Loader download URL"
    fi
    
    # Download Fabric API
    echo "Downloading Fabric API..."
    FABRIC_API_URL="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$FABRIC_API_VERSION/fabric-api-$FABRIC_API_VERSION.jar"
    curl -L -o "$FABRIC_DIR/api/fabric-api-$FABRIC_API_VERSION.jar" "$FABRIC_API_URL" || echo "Warning: Could not download Fabric API"
    
    # Download Loom (Gradle plugin)
    echo "Downloading Loom Gradle plugin..."
    LOOM_VERSION="1.4-SNAPSHOT"
    LOOM_URL="https://maven.fabricmc.net/net/fabricmc/loom/gradle-loom/$LOOM_VERSION/gradle-loom-$LOOM_VERSION.jar"
    curl -L -o "$FABRIC_DIR/loom/gradle-loom-$LOOM_VERSION.jar" "$LOOM_URL" || echo "Warning: Could not download Loom"
}

# Download Yarn mappings
download_mappings() {
    echo "Downloading Yarn mappings $YARN_MAPPINGS_VERSION..."
    
    MAPPINGS_URL="https://maven.fabricmc.net/net/fabricmc/yarn/$YARN_MAPPINGS_VERSION/yarn-$YARN_MAPPINGS_VERSION.jar"
    curl -L -o "$MAPPINGS_DIR/yarn-$YARN_MAPPINGS_VERSION.jar" "$MAPPINGS_URL" || echo "Warning: Could not download Yarn mappings"
}

# Download decompilers
download_decompilers() {
    echo "Downloading decompilers..."
    
    FERNFLOWER_URL="https://github.com/fesh0r/fernflower/releases/download/2.0.1/fernflower.jar"
    curl -L -o "$DECOMPILER_DIR/fernflower.jar" "$FERNFLOWER_URL" || echo "Warning: Could not download Fernflower"
    
    VINEFLOWER_URL="https://github.com/Vineflower/vineflower/releases/latest/download/vineflower.jar"
    curl -L -o "$DECOMPILER_DIR/vineflower.jar" "$VINEFLOWER_URL" 2>/dev/null || echo "Vineflower not available, using Fernflower"
}

# Decompile Minecraft
decompile_minecraft() {
    echo "Decompiling Minecraft client..."
    
    cd "$DECOMPILER_DIR"
    
    # Use vineflower if available, otherwise fernflower
    if [ -f "vineflower.jar" ]; then
        java -jar vineflower.jar \
            --outputDir="$MC_DIR/client/src" \
            "$MC_DIR/client/minecraft-$MINECRAFT_VERSION.jar" || echo "Decompilation failed"
    elif [ -f "fernflower.jar" ]; then
        java -jar fernflower.jar \
            -dgs=1 -hdc=0 -rbr=0 -asc=1 -udv=0 \
            "$MC_DIR/client/minecraft-$MINECRAFT_VERSION.jar" \
            "$MC_DIR/client/src" || echo "Decompilation failed"
    else
        echo "No decompiler available"
        return 1
    fi
    
    echo "Decompilation complete. Source in $MC_DIR/client/src"
}

# Remap sources with TinyRemapper
remap_sources() {
    echo "Remapping sources with Yarn mappings..."
    
    # Download TinyRemapper if not present
    TINY_REMAPPER="$TOOLS_DIR/tinyremapper.jar"
    if [ ! -f "$TINY_REMAPPER" ]; then
        echo "Downloading TinyRemapper..."
        curl -L -o "$TINY_REMAPPER" "https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.11.0/tiny-remapper-0.11.0.jar"
    fi
    
    java -jar "$TINY_REMAPPER" \
        --input "$MC_DIR/client/src" \
        --output "$MC_DIR/client/src-mapped" \
        --mappings "$MAPPINGS_DIR/yarn-$YARN_MAPPINGS_VERSION.jar" \
        --side CLIENT || echo "Remapping failed"
    
    echo "Remapping complete. Mapped source in $MC_DIR/client/src-mapped"
}

# Copy to Android project
copy_to_android() {
    echo "Copying mapped sources to Android project..."
    
    ANDROID_SRC="$PROJECT_ROOT/android/app/src/main/java"
    mkdir -p "$ANDROID_SRC"
    
    # Copy mapped sources
    cp -r "$MC_DIR/client/src-mapped"/* "$ANDROID_SRC/" || echo "Copy failed"
    
    echo "Sources copied to $ANDROID_SRC"
}

# Main
main() {
    check_deps
    
    echo "Starting download and setup..."
    
    download_minecraft
    download_fabric
    download_mappings
    download_decompilers
    
    decompile_minecraft
    remap_sources
    copy_to_android
    
    echo ""
    echo "=== Download and Setup Complete ==="
    echo "Minecraft client: $MC_DIR/client/minecraft-$MINECRAFT_VERSION.jar"
    echo "Decompiled source: $MC_DIR/client/src"
    echo "Remapped source: $MC_DIR/client/src-mapped"
    echo "Fabric Loader: $FABRIC_DIR/loader/fabric-loader-$FABRIC_LOADER_VERSION.jar"
    echo "Fabric API: $FABRIC_DIR/api/fabric-api-$FABRIC_API_VERSION.jar"
    echo "Yarn Mappings: $MAPPINGS_DIR/yarn-$YARN_MAPPINGS_VERSION.jar"
    echo "Decompilers: $DECOMPILER_DIR/"
    echo ""
    echo "Next step: Run ./scripts/build.sh to build the Android APK"
}

main "$@"
