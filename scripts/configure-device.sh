#!/bin/bash

# ETHIOSTAT Device Configuration Script
# Configures Android device for testing with required permissions

set -e

echo "========================================="
echo "  ETHIOSTAT Device Setup"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PACKAGE_NAME="com.ethiostat.app"

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}Error: adb not found. Please install Android SDK Platform-Tools.${NC}"
    exit 1
fi

# Check for connected devices
echo -e "${YELLOW}Checking for connected devices...${NC}"
DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l | xargs)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}Error: No devices connected.${NC}"
    echo ""
    echo "Please connect a device via USB or enable wireless debugging:"
    echo "  1. Enable USB Debugging in Developer Options"
    echo "  2. Connect device via USB"
    echo "  3. Accept USB debugging prompt on device"
    echo ""
    echo "For wireless debugging (Android 11+):"
    echo "  1. Go to Developer Options > Wireless debugging"
    echo "  2. Enable Wireless debugging"
    echo "  3. Pair device using pairing code or QR code"
    exit 1
fi

echo -e "${GREEN}✓ Found $DEVICE_COUNT device(s)${NC}"
echo ""

# Show connected devices
echo -e "${YELLOW}Connected devices:${NC}"
adb devices -l
echo ""

# Check if app is installed
echo -e "${YELLOW}Checking if ETHIOSTAT is installed...${NC}"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo -e "${GREEN}✓ ETHIOSTAT is installed${NC}"
    
    # Get app version
    VERSION=$(adb shell dumpsys package "$PACKAGE_NAME" | grep versionName | head -1 | cut -d'=' -f2)
    echo -e "${YELLOW}Version:${NC} $VERSION"
else
    echo -e "${YELLOW}! ETHIOSTAT is not installed${NC}"
    echo "Install the app first using: ./scripts/deploy.sh debug device"
    exit 1
fi
echo ""

# Grant permissions
echo -e "${YELLOW}Granting required permissions...${NC}"

PERMISSIONS=(
    "android.permission.RECEIVE_SMS"
    "android.permission.READ_SMS"
    "android.permission.CALL_PHONE"
    "android.permission.READ_PHONE_STATE"
)

for PERMISSION in "${PERMISSIONS[@]}"; do
    echo -e "  Granting: $PERMISSION"
    adb shell pm grant "$PACKAGE_NAME" "$PERMISSION" 2>/dev/null || echo -e "${YELLOW}    Already granted or system permission${NC}"
done

echo -e "${GREEN}✓ Permissions granted${NC}"
echo ""

# Set as default SMS app (optional)
echo -e "${YELLOW}Would you like to set ETHIOSTAT as default SMS app? (y/N)${NC}"
read -r RESPONSE
if [[ "$RESPONSE" =~ ^[Yy]$ ]]; then
    adb shell cmd role add-role-holder android.app.role.SMS "$PACKAGE_NAME"
    echo -e "${GREEN}✓ Set as default SMS app${NC}"
fi
echo ""

# Clear app data (optional)
echo -e "${YELLOW}Would you like to clear app data? (y/N)${NC}"
read -r RESPONSE
if [[ "$RESPONSE" =~ ^[Yy]$ ]]; then
    adb shell pm clear "$PACKAGE_NAME"
    echo -e "${GREEN}✓ App data cleared${NC}"
fi
echo ""

# Device info
echo -e "${YELLOW}Device Information:${NC}"
echo -e "  Model: $(adb shell getprop ro.product.model)"
echo -e "  Android: $(adb shell getprop ro.build.version.release)"
echo -e "  SDK: $(adb shell getprop ro.build.version.sdk)"
echo ""

# Launch app
echo -e "${YELLOW}Launching ETHIOSTAT...${NC}"
adb shell am start -n "$PACKAGE_NAME/.ui.MainActivity"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ App launched successfully!${NC}"
else
    echo -e "${RED}Error: Failed to launch app${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  Device Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Open the app on your device"
echo "  2. Grant any additional permissions if prompted"
echo "  3. Send a test SMS from a configured sender (e.g., 251994)"
echo "  4. Check the dashboard for parsed balances"
echo ""
echo -e "${YELLOW}Debugging:${NC}"
echo "  View logs: adb logcat -s EthioStat"
echo "  View database: adb shell 'run-as $PACKAGE_NAME cat databases/ethiostat_database'"
