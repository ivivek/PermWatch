#!/bin/bash

# Build script for PermWatch Android app
# Usage: ./build.sh [build|install|release|clean|bundle]

set -e

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/jbr" ]; then
        export JAVA_HOME="/opt/jbr"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
fi

# Set ANDROID_HOME if not already set
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    elif [ -d "/home/vivek/AndroidStudioFiles" ]; then
        export ANDROID_HOME="/home/vivek/AndroidStudioFiles"
    fi
fi

export ANDROID_SDK_ROOT="$ANDROID_HOME"

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Using ANDROID_HOME: $ANDROID_HOME"

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found. Run this script from the project root."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

ACTION=${1:-build}

case $ACTION in
    build)
        echo "Building debug APK..."
        ./gradlew assembleDevDebug
        echo ""
        echo "APK location: app/build/outputs/apk/dev/debug/app-dev-debug.apk"
        ;;
    release)
        echo "Building release APK..."
        ./gradlew assembleProdRelease
        echo ""
        echo "APK location: app/build/outputs/apk/prod/release/app-prod-release.apk"
        ;;
    install)
        echo "Building and installing debug APK..."
        ./gradlew installDevDebug
        echo ""
        echo "Launching app..."
        adb shell am start -n com.linetra.permwatch.dev/com.linetra.permwatch.MainActivity
        ;;
    clean)
        echo "Cleaning build artifacts..."
        ./gradlew clean
        ;;
    bundle)
        echo "Building release bundle (AAB)..."
        ./gradlew bundleProdRelease
        echo ""
        echo "Bundle location: app/build/outputs/bundle/prodRelease/app-prod-release.aab"
        ;;
    *)
        echo "Usage: $0 [build|install|release|clean|bundle]"
        echo ""
        echo "Commands:"
        echo "  build    - Build debug APK (default)"
        echo "  install  - Build, install to device, and launch"
        echo "  release  - Build release APK"
        echo "  bundle   - Build release AAB bundle"
        echo "  clean    - Clean build artifacts"
        exit 1
        ;;
esac

echo ""
echo "Done!"
