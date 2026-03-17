#!/bin/bash

# ETHIOSTAT Deployment Script
# This script builds and deploys the APK to connected devices or uploads to distribution

set -e

echo "========================================="
echo "  ETHIOSTAT Deployment Script"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BUILD_TYPE=${1:-debug}
DEPLOY_TARGET=${2:-device}

echo -e "${YELLOW}Build Type:${NC} $BUILD_TYPE"
echo -e "${YELLOW}Deploy Target:${NC} $DEPLOY_TARGET"
echo ""

# Clean previous builds
echo -e "${YELLOW}Cleaning previous builds...${NC}"
./gradlew clean

# Build APK
echo -e "${YELLOW}Building $BUILD_TYPE APK...${NC}"
if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease --stacktrace
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    ./gradlew assembleDebug --stacktrace
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}Error: APK not found at $APK_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful!${NC}"
echo -e "${YELLOW}APK Location:${NC} $APK_PATH"
echo ""

# Get APK size
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${YELLOW}APK Size:${NC} $APK_SIZE"
echo ""

# Deploy based on target
case $DEPLOY_TARGET in
    device)
        echo -e "${YELLOW}Deploying to connected device(s)...${NC}"
        
        # Check if adb is available
        if ! command -v adb &> /dev/null; then
            echo -e "${RED}Error: adb not found. Please install Android SDK Platform-Tools.${NC}"
            exit 1
        fi
        
        # Check for connected devices
        DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l | xargs)
        
        if [ "$DEVICE_COUNT" -eq 0 ]; then
            echo -e "${RED}Error: No devices connected. Please connect a device via USB or enable wireless debugging.${NC}"
            exit 1
        fi
        
        echo -e "${GREEN}Found $DEVICE_COUNT device(s)${NC}"
        echo ""
        
        # Install APK
        echo -e "${YELLOW}Installing APK...${NC}"
        adb install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Installation successful!${NC}"
            echo ""
            echo -e "${YELLOW}Launching app...${NC}"
            adb shell am start -n com.ethiostat.app/.ui.MainActivity
            echo -e "${GREEN}✓ App launched!${NC}"
        else
            echo -e "${RED}Error: Installation failed${NC}"
            exit 1
        fi
        ;;
        
    firebase)
        echo -e "${YELLOW}Deploying to Firebase App Distribution...${NC}"
        
        if ! command -v firebase &> /dev/null; then
            echo -e "${RED}Error: Firebase CLI not found. Install with: npm install -g firebase-tools${NC}"
            exit 1
        fi
        
        firebase appdistribution:distribute "$APK_PATH" \
            --app "$FIREBASE_APP_ID" \
            --release-notes "Automated build from deployment script" \
            --groups "testers"
        
        echo -e "${GREEN}✓ Deployed to Firebase!${NC}"
        ;;
        
    github)
        echo -e "${YELLOW}Creating GitHub release...${NC}"
        
        if ! command -v gh &> /dev/null; then
            echo -e "${RED}Error: GitHub CLI not found. Install from: https://cli.github.com/${NC}"
            exit 1
        fi
        
        VERSION=$(grep "versionName" app/build.gradle.kts | cut -d'"' -f2)
        RELEASE_TAG="v$VERSION"
        
        gh release create "$RELEASE_TAG" "$APK_PATH" \
            --title "ETHIOSTAT v$VERSION" \
            --notes "Automated release build"
        
        echo -e "${GREEN}✓ Released to GitHub!${NC}"
        ;;
        
    local)
        echo -e "${YELLOW}Copying APK to local distribution folder...${NC}"
        
        DIST_DIR="./dist"
        mkdir -p "$DIST_DIR"
        
        TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
        DIST_APK="$DIST_DIR/ethiostat-$BUILD_TYPE-$TIMESTAMP.apk"
        
        cp "$APK_PATH" "$DIST_APK"
        
        echo -e "${GREEN}✓ APK copied to: $DIST_APK${NC}"
        ;;
        
    *)
        echo -e "${RED}Error: Unknown deploy target: $DEPLOY_TARGET${NC}"
        echo "Valid targets: device, firebase, github, local"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  Deployment Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
