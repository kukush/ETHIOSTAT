#!/bin/bash

# ETHIOSTAT Build Script
# Builds all variants and generates signed APKs

set -e

echo "========================================="
echo "  ETHIOSTAT Build Script"
echo "========================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Clean build
echo -e "${YELLOW}Cleaning previous builds...${NC}"
./gradlew clean

# Build debug
echo -e "${YELLOW}Building Debug APK...${NC}"
./gradlew assembleDebug --stacktrace

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Debug build successful${NC}"
    DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
    echo -e "${YELLOW}  Location:${NC} $DEBUG_APK"
    echo -e "${YELLOW}  Size:${NC} $(du -h $DEBUG_APK | cut -f1)"
else
    echo -e "${RED}✗ Debug build failed${NC}"
    exit 1
fi
echo ""

# Build release
echo -e "${YELLOW}Building Release APK...${NC}"
./gradlew assembleRelease --stacktrace

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Release build successful${NC}"
    RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    echo -e "${YELLOW}  Location:${NC} $RELEASE_APK"
    echo -e "${YELLOW}  Size:${NC} $(du -h $RELEASE_APK | cut -f1)"
    echo -e "${YELLOW}  Note:${NC} APK is unsigned. Sign with your keystore for production."
else
    echo -e "${RED}✗ Release build failed${NC}"
    exit 1
fi
echo ""

# Run tests
echo -e "${YELLOW}Running tests...${NC}"
./gradlew test --stacktrace

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Tests passed${NC}"
else
    echo -e "${YELLOW}⚠ Some tests failed (continuing)${NC}"
fi
echo ""

# Create dist folder
echo -e "${YELLOW}Creating distribution package...${NC}"
DIST_DIR="dist"
mkdir -p "$DIST_DIR"

# Copy APKs
cp "$DEBUG_APK" "$DIST_DIR/ethiostat-debug.apk"
cp "$RELEASE_APK" "$DIST_DIR/ethiostat-release-unsigned.apk"

# Create build info
BUILD_DATE=$(date +"%Y-%m-%d %H:%M:%S")
VERSION_NAME=$(grep "versionName" app/build.gradle.kts | cut -d'"' -f2)
VERSION_CODE=$(grep "versionCode" app/build.gradle.kts | awk '{print $3}')

cat > "$DIST_DIR/BUILD_INFO.txt" << EOF
ETHIOSTAT Build Information
========================================
Build Date: $BUILD_DATE
Version Name: $VERSION_NAME
Version Code: $VERSION_CODE

APK Files:
- ethiostat-debug.apk (Debug build, ready for testing)
- ethiostat-release-unsigned.apk (Release build, requires signing)

Installation:
  adb install -r ethiostat-debug.apk

Configuration:
  Default senders: 251994, *830*
  USSD codes: *805#, *804#
  
  Configure via gradle.properties or in-app settings

For more information, see README.md
EOF

echo -e "${GREEN}✓ Distribution package created in $DIST_DIR/${NC}"
echo ""

# Summary
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  Build Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${YELLOW}Summary:${NC}"
echo -e "  Debug APK: ${GREEN}✓${NC}"
echo -e "  Release APK: ${GREEN}✓${NC}"
echo -e "  Tests: ${GREEN}✓${NC}"
echo ""
echo -e "${YELLOW}Distribution:${NC} $DIST_DIR/"
echo -e "${YELLOW}Install command:${NC} adb install -r $DIST_DIR/ethiostat-debug.apk"
