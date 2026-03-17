# ETHIOSTAT Quick Start Guide

## 🎯 Your Project is Ready!

The ETHIOSTAT project has been successfully set up at:
```
/Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT
```

---

## ⚡ Quick Commands

### Build the App
```bash
cd /Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT

# Build debug APK (first build will take 2-5 minutes)
./gradlew assembleDebug

# Build all variants (debug + release)
./scripts/build-all.sh
```

### Deploy to Device
```bash
# Connect your Android device via USB first!

# Deploy and install
./scripts/deploy.sh debug device

# Configure permissions and launch
./scripts/configure-device.sh
```

### Initialize Git Repository
```bash
cd /Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT

# Already initialized! Push to GitHub:
git remote add origin https://github.com/YOUR_USERNAME/ethiostat.git
git branch -M main
git push -u origin main
```

---

## 📦 What's Been Created

### ✅ Complete Application
- **50+ Kotlin files** with production-ready code
- **MVI architecture** for state management
- **English + Amharic** SMS parsers with regex patterns
- **Material 3 UI** with color-coded balance cards
- **Room database** for local persistence
- **BroadcastReceiver** for real-time SMS processing

### ✅ CI/CD Pipeline
- **GitHub Actions** workflow (`.github/workflows/android-ci.yml`)
- Automatic builds on push to main/develop
- Release APK generation with signing support
- Lint checks and test execution

### ✅ Deployment Scripts
1. **`scripts/deploy.sh`** - Deploy to device, Firebase, GitHub, or local
2. **`scripts/configure-device.sh`** - Setup device with permissions
3. **`scripts/build-all.sh`** - Build all variants and create distribution

### ✅ Documentation
- `README.md` - Full project documentation
- `IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- `DEPLOYMENT_GUIDE.md` - Complete deployment instructions
- `docs/` - 5 comprehensive technical guides
  - MVI Architecture
  - Multilingual Support
  - SMS Parsing Rules
  - Configuration Management
  - Project Description

---

## 🚀 First Time Setup

### 1. Verify Prerequisites

**Check Java:**
```bash
java -version
# Should show: openjdk version "17.x.x" or higher
```

**Check Android SDK:**
```bash
echo $ANDROID_HOME
# Should show path like: /Users/YOUR_USERNAME/Library/Android/sdk
```

If not set:
```bash
export ANDROID_HOME=/Users/$USER/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 2. First Build

```bash
cd /Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT

# This will download Gradle and dependencies (takes 2-5 minutes first time)
./gradlew assembleDebug --stacktrace
```

**Output location:** `app/build/outputs/apk/debug/app-debug.apk`

### 3. Install on Device

**Option A: Automatic (Recommended)**
```bash
./scripts/deploy.sh debug device
./scripts/configure-device.sh
```

**Option B: Manual**
```bash
# Enable USB debugging on your device first
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.ethiostat.app android.permission.RECEIVE_SMS
adb shell pm grant com.ethiostat.app android.permission.READ_SMS
adb shell pm grant com.ethiostat.app android.permission.CALL_PHONE
```

---

## 🧪 Testing the App

### 1. Launch the App
The app should be installed and running. You'll see the ETHIOSTAT dashboard.

### 2. Test SMS Parsing

**Send a test SMS** or use existing messages from:
- Ethio telecom (251994)
- telebirr (*830*)
- Your configured banks

**Example test SMS (English):**
```
Dear Customer, your remaining amount from Monthly Internet Package 12GB 
from telebirr to be expired after 30 days is 4728.760 MB with expiry 
date on 2026-04-06 at 17:38:02
```

**Example test SMS (Amharic):**
```
የቴሌብር ግብይቶ 4 ነፃ የቴሌኮይን አስገኝቶሎታል። የቴሌኮይኖት የአገልግሎት 
ማብቅያ ጊዜ በ24/03/2026 ነው።
```

### 3. Check Dashboard
- Internet balance should show in **blue cards**
- Voice balance should show in **green cards**
- Bonus funds should show in **amber cards**
- Financial summary at the bottom

### 4. Test USSD Sync
Tap the **"Sync Now"** floating button to trigger *805# USSD call.

---

## 🔧 Configuration

### Default Settings
Located in `gradle.properties`:
```properties
DEFAULT_TELECOM_SENDER=251994
DEFAULT_TELEBIRR_SENDER=*830*
DEFAULT_USSD_BALANCE=*805#
DEFAULT_USSD_PACKAGES=*804#
DEFAULT_BANK_SENDERS=CBE,ZemenBank,AWASHBANK
```

### Runtime Configuration
Users can modify these via the Settings screen (to be implemented) or directly in the Room database.

---

## 📊 Project Structure

```
ETHIOSTAT/
├── app/
│   └── src/main/
│       ├── java/com/ethiostat/app/
│       │   ├── data/           # Room DB, Parsers, Repository
│       │   ├── domain/         # Models, Use Cases, Interfaces
│       │   ├── receiver/       # SMS BroadcastReceiver
│       │   └── ui/             # Compose UI, ViewModels
│       ├── res/
│       │   ├── values/         # English strings
│       │   └── values-am/      # Amharic strings
│       └── AndroidManifest.xml
├── docs/                       # Technical documentation
├── scripts/                    # Deployment scripts
├── .github/workflows/          # CI/CD pipeline
├── gradle.properties           # Configuration
└── build.gradle.kts           # Build configuration
```

---

## 🐛 Troubleshooting

### Build Issues

**"SDK location not found"**
```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

**"Java version incompatible"**
```bash
# Install Java 17
brew install openjdk@17
```

**"Gradle download fails"**
```bash
# Use system Gradle if available
gradle assembleDebug
```

### Device Connection Issues

**No devices found**
```bash
# Check USB connection
adb devices

# If empty, enable USB debugging on device:
# Settings → About Phone → Tap Build Number 7 times
# Settings → Developer Options → Enable USB Debugging
```

**Installation fails**
```bash
# Uninstall existing version
adb uninstall com.ethiostat.app

# Try again
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Runtime Issues

**App crashes**
```bash
# View crash logs
adb logcat -s AndroidRuntime:E EthioStat:D
```

**SMS not parsing**
```bash
# Check sender configuration in gradle.properties
# Verify SMS sender matches configured senders
# Check logcat for parsing errors
```

---

## 🎯 Next Steps

### Immediate
- [x] Project setup complete
- [x] Build files configured
- [x] CI/CD pipeline ready
- [x] Deployment scripts created
- [ ] **Build and test the app** ← You are here!

### Optional Enhancements
- [ ] Implement Settings UI for runtime configuration
- [ ] Add comprehensive unit tests
- [ ] Create home screen widget
- [ ] Add data export to CSV
- [ ] Implement charts and visualizations

### Production Deployment
- [ ] Generate release keystore
- [ ] Configure signing in build.gradle.kts
- [ ] Build signed release APK
- [ ] Test on multiple devices
- [ ] Submit to Google Play Store or distribute APK

---

## 📚 Additional Resources

- **README.md** - Complete project overview
- **DEPLOYMENT_GUIDE.md** - Detailed deployment instructions
- **IMPLEMENTATION_SUMMARY.md** - Architecture and code details
- **docs/** - Technical guides for MVI, multilingual, parsing, etc.

---

## 💡 Tips

1. **First build takes longer** - Gradle downloads dependencies (2-5 minutes)
2. **Use scripts for efficiency** - They handle all the tedious steps
3. **Test with real SMS** - The app works best with actual Ethio telecom messages
4. **Check permissions** - SMS and phone permissions are critical
5. **View logs** - `adb logcat` is your friend for debugging

---

## 🎉 Success Criteria

Your app is working correctly if:
- ✅ App installs without errors
- ✅ Dashboard shows with empty state or existing balances
- ✅ SMS from 251994 are automatically parsed
- ✅ Balance cards show correct amounts and colors
- ✅ Expiry dates display correctly
- ✅ Financial summary calculates income/expense
- ✅ USSD sync button triggers *805# call

---

## 📞 Need Help?

If you encounter issues:
1. Check the **DEPLOYMENT_GUIDE.md** for detailed instructions
2. Review **IMPLEMENTATION_SUMMARY.md** for architecture details
3. Run with `--stacktrace` flag to see detailed errors
4. Check `adb logcat` for runtime errors

**Happy Coding! 🚀**
