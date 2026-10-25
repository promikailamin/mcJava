#!/bin/bash
# CoreServerMod Test Server Start Script
# Launches a dedicated Minecraft 26.2 Fabric server for testing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVER_DIR="$SCRIPT_DIR/test-server"
MODS_DIR="$SERVER_DIR/mods"
CONFIG_DIR="$SERVER_DIR/config"
WORLD_DIR="$SERVER_DIR/world"
LOGS_DIR="$SERVER_DIR/logs"

# Server configuration
MINECRAFT_VERSION="26.2"
FABRIC_LOADER_VERSION="0.19.5"
FABRIC_API_VERSION="0.161.0+26.2"
JAVA_VERSION="25"
SERVER_JAR="fabric-server-mc${MINECRAFT_VERSION}-loader${FABRIC_LOADER_VERSION}-launcher1.0.1.jar"
SERVER_URL="https://meta.fabricmc.net/v2/versions/loader/${MINECRAFT_VERSION}/${FABRIC_LOADER_VERSION}/1.0.1/server/jar"
FABRIC_API_URL="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/${FABRIC_API_VERSION}/fabric-api-${FABRIC_API_VERSION}.jar"

# Memory settings
HEAP_MIN="2G"
HEAP_MAX="4G"

echo "=== CoreServerMod Test Server ==="
echo "Server directory: $SERVER_DIR"
echo "Minecraft version: $MINECRAFT_VERSION"
echo "Fabric Loader: $FABRIC_LOADER_VERSION"

# Create directories
mkdir -p "$MODS_DIR" "$CONFIG_DIR" "$WORLD_DIR" "$LOGS_DIR"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install Java $JAVA_VERSION or later."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt "$JAVA_VERSION" ]; then
    echo "WARNING: Java version $JAVA_VER detected, recommended is $JAVA_VERSION+"
fi

# Download Fabric server JAR if not present
if [ ! -f "$SERVER_DIR/$SERVER_JAR" ]; then
    echo "Downloading Fabric server JAR..."
    cd "$SERVER_DIR"
    curl -L -o "$SERVER_JAR" "$SERVER_URL"
    echo "Download complete."
fi

# Download Fabric API if not present
FABRIC_API_JAR="fabric-api-${FABRIC_API_VERSION}.jar"
if [ ! -f "$MODS_DIR/$FABRIC_API_JAR" ]; then
    echo "Downloading Fabric API..."
    curl -L -o "$MODS_DIR/$FABRIC_API_JAR" "$FABRIC_API_URL"
    echo "Download complete."
fi

# Build mod if not present
MOD_JAR=$(find "$MODS_DIR" -name "CoreServerMod-*.jar" | head -1)
if [ -z "$MOD_JAR" ] || [ ! -f "$MOD_JAR" ]; then
    echo "CoreServerMod JAR not found. Building..."
    cd "$SCRIPT_DIR"
    ./build.sh
    MOD_JAR=$(find "$MODS_DIR" -name "CoreServerMod-*.jar" | head -1)
fi

# Check EULA
EULA_FILE="$SERVER_DIR/eula.txt"
if [ ! -f "$EULA_FILE" ]; then
    echo ""
    echo "=========================================="
    echo "MINECRAFT EULA AGREEMENT REQUIRED"
    echo "=========================================="
    echo "By starting this server, you agree to the Minecraft EULA:"
    echo "https://account.mojang.com/documents/minecraft_eula"
    echo ""
    read -p "Do you agree to the EULA? (yes/no): " EULA_AGREE
    if [ "$EULA_AGREE" = "yes" ] || [ "$EULA_AGREE" = "y" ]; then
        echo "eula=true" > "$EULA_FILE"
        echo "EULA accepted."
    else
        echo "EULA not accepted. Server cannot start."
        exit 1
    fi
fi

# Create server.properties if not present
PROPERTIES_FILE="$SERVER_DIR/server.properties"
if [ ! -f "$PROPERTIES_FILE" ]; then
    cat > "$PROPERTIES_FILE" << 'EOF'
# CoreServerMod Test Server Properties
server-ip=
server-port=25565
gamemode=survival
difficulty=normal
max-players=20
online-mode=false
allow-flight=false
view-distance=10
simulation-distance=10
motd=CoreServerMod Test Server
level-name=world
level-seed=
level-type=minecraft:normal
generate-structures=true
spawn-protection=0
max-tick-time=60000
EOF
    echo "Created default server.properties"
fi

# Start server
echo ""
echo "Starting CoreServerMod test server..."
echo "Logs will be written to: $LOGS_DIR/latest.log"
echo "Press Ctrl+C to stop the server"
echo ""

cd "$SERVER_DIR"
exec java -Xms${HEAP_MIN} -Xmx${HEAP_MAX} \
    -XX:+UseG1GC \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+DisableExplicitGC \
    -XX:+AlwaysPreTouch \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=8M \
    -XX:G1ReservePercent=20 \
    -XX:G1HeapWastePercent=5 \
    -XX:G1MixedGCCountTarget=4 \
    -XX:InitiatingHeapOccupancyPercent=15 \
    -XX:G1MixedGCLiveThresholdPercent=90 \
    -XX:G1RSetUpdatingPauseTimePercent=5 \
    -XX:SurvivorRatio=32 \
    -XX:+PerfDisableSharedMem \
    -XX:MaxTenuringThreshold=1 \
    -Dfml.readTimeout=120 \
    -jar "$SERVER_JAR" nogui 2>&1 | tee "$LOGS_DIR/latest.log"