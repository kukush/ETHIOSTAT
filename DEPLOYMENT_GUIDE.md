# ETHIOSTAT Deployment Guide

This guide covers all deployment options for ETHIOSTAT, from local development to production distribution.

---

## 🚀 Quick Start

### Prerequisites

1. **Java Development Kit (JDK) 17**
   ```bash
   java -version  # Should show version 17 or higher
   ```

2. **Android SDK** (via Android Studio or command line tools)
   - Build Tools 34.0.0
   - Platform SDK 34

3. **Git** (for version control)

### Build Commands

#### Build Debug APK
```bash
cd /Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT
./gradlew assembleDebug
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release APK
```bash
./gradlew assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

#### Build All Variants
```bash
./scripts/build-all.sh
```

This creates a `dist/` folder with both APKs and build info.

---

## 📦 Deployment Scripts

### 1. Build All Variants
```bash
./scripts/build-all.sh
```

**Features:**
- Cleans previous builds
- Builds debug and release APKs
- Runs tests
- Creates distribution package in `dist/`
- Generates build info file

### 2. Deploy to Device
```bash
./scripts/deploy.sh debug device
```

**Options:**
- `debug|release` - Build type
- `device|firebase|github|local` - Deployment target

**Examples:**
```bash
# Deploy debug to connected device
./scripts/deploy.sh debug device

# Build release and copy to local dist folder
./scripts/deploy.sh release local

# Deploy to Firebase App Distribution
./scripts/deploy.sh debug firebase

# Create GitHub release
./scripts/deploy.sh release github
```

### 3. Configure Device
```bash
./scripts/configure-device.sh
```

**Features:**
- Checks for connected devices
- Grants all required permissions
- Optionally sets as default SMS app
- Optionally clears app data
- Launches the app

---

## 🔧 Manual Installation

### Via ADB (Android Debug Bridge)

1. **Enable USB Debugging** on your device:
   ```
   Settings → About Phone → Tap "Build Number" 7 times
   Settings → Developer Options → Enable "USB Debugging"
   ```

2. **Connect Device** via USB

3. **Install APK:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Grant Permissions:**
   ```bash
   adb shell pm grant com.ethiostat.app android.permission.RECEIVE_SMS
   adb shell pm grant com.ethiostat.app android.permission.READ_SMS
   adb shell pm grant com.ethiostat.app android.permission.CALL_PHONE
   ```

5. **Launch App:**
   ```bash
   adb shell am start -n com.ethiostat.app/.ui.MainActivity
   ```

---

## 🌐 GitHub Actions CI/CD

The project includes automated CI/CD via GitHub Actions.

### Workflow Triggers

- **Push to `main` or `develop`**: Builds debug APK and runs tests
- **Pull Request to `main`**: Full CI pipeline
- **Release Creation**: Builds signed release APK

### Setup Instructions

1. **Create Repository** on GitHub:
   ```bash
   cd /Users/getahuntesfaye/Documents/GitHub/ETHIOSTAT
   git remote add origin https://github.com/yourusername/ethiostat.git
   git push -u origin main
   ```

2. **Configure Secrets** (for release signing):
   - Go to: Repository → Settings → Secrets and variables → Actions
   - Add secrets:
     - `SIGNING_KEY` - Base64 encoded keystore
     - `KEY_ALIAS` - Keystore alias
     - `KEY_STORE_PASSWORD` - Keystore password
     - `KEY_PASSWORD` - Key password

3. **Create Release:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   
   Or via GitHub UI: Releases → Create a new release

### Artifacts

After each build, download artifacts from:
- **GitHub Actions** → Select workflow run → Artifacts section

---

## 🔐 Signing for Production

### Generate Keystore

```bash
keytool -genkey -v -keystore ethiostat-release.keystore \
  -alias ethiostat -keyalg RSA -keysize 2048 -validity 10000
```

### Sign APK

```bash
# Using jarsigner
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore ethiostat-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk ethiostat

# Optimize with zipalign
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/ethiostat-release.apk
```

### Configure in build.gradle.kts

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("ethiostat-release.keystore")
            storePassword = System.getenv("KEY_STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

---

## 📱 Distribution Channels

### 1. Google Play Store

1. Create a Google Play Developer account ($25 one-time fee)
2. Build signed release APK or AAB:
   ```bash
   ./gradlew bundleRelease
   ```
3. Upload to Play Console
4. Fill out store listing (screenshots, description, etc.)
5. Submit for review

### 2. Firebase App Distribution

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Deploy
firebase appdistribution:distribute \
  app/build/outputs/apk/debug/app-debug.apk \
  --app YOUR_FIREBASE_APP_ID \
  --groups testers
```

### 3. Direct APK Distribution

**Host on your server:**
```bash
# Upload to web server
scp app/build/outputs/apk/release/ethiostat-release.apk \
  user@yourserver.com:/var/www/downloads/
```

**Share via link:**
```
https://yourserver.com/downloads/ethiostat-release.apk
```

**Note:** Users must enable "Install from Unknown Sources" in Android settings.

---

## 🧪 Testing Builds

### Local Testing

```bash
# Install and test
./scripts/deploy.sh debug device
./scripts/configure-device.sh

# View logs
adb logcat -s EthioStat
```

### Beta Testing

1. **Internal Testing (5-10 users)**
   - Use Firebase App Distribution
   - Share APK directly

2. **Closed Beta (50-100 users)**
   - Use Google Play Internal Testing track
   - Invite testers via email

3. **Open Beta (Unlimited)**
   - Use Google Play Open Testing track
   - Public opt-in link

---

## 🔄 Update Flow

### Over-the-Air Updates

When publishing updates:

1. **Increment version in `app/build.gradle.kts`:**
   ```kotlin
   versionCode = 2  // Must increment
   versionName = "1.1.0"
   ```

2. **Build new APK:**
   ```bash
   ./scripts/build-all.sh
   ```

3. **Deploy:**
   - Google Play: Upload via Play Console
   - Firebase: Use deployment script
   - Direct: Replace APK on server

4. **Users update:**
   - Play Store: Automatic or manual update
   - Firebase: In-app notification
   - Direct: Manual download and install

---

## 🐛 Troubleshooting

### Build Fails

**"SDK location not found"**
```bash
# Create local.properties
echo "sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk" > local.properties
```

**"Gradle daemon disappeared"**
```bash
./gradlew --stop
./gradlew assembleDebug
```

### Installation Fails

**"INSTALL_FAILED_UPDATE_INCOMPATIBLE"**
```bash
# Uninstall existing app
adb uninstall com.ethiostat.app
# Reinstall
adb install app/build/outputs/apk/debug/app-debug.apk
```

**"Permission Denial"**
```bash
# Grant permissions manually
adb shell pm grant com.ethiostat.app android.permission.RECEIVE_SMS
```

### Runtime Issues

**App crashes on SMS receive**
```bash
# Check logs
adb logcat -s AndroidRuntime:E EthioStat:D
```

**Database errors**
```bash
# Clear app data
adb shell pm clear com.ethiostat.app
```

---

## 📊 Build Optimization

### Reduce APK Size

1. **Enable ProGuard/R8:**
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
       }
   }
   ```

2. **Use APK Splits:**
   ```kotlin
   android {
       splits {
           abi {
               isEnable = true
               reset()
               include("armeabi-v7a", "arm64-v8a")
           }
       }
   }
   ```

### Improve Build Speed

1. **Enable Gradle caching:**
   ```properties
   # gradle.properties
   org.gradle.caching=true
   org.gradle.parallel=true
   ```

2. **Use configuration cache:**
   ```bash
   ./gradlew assembleDebug --configuration-cache
   ```

---

## 🌍 Multi-Region Deployment

### Configuration by Region

Edit `gradle.properties` for different regions:

```properties
# Ethiopia
DEFAULT_TELECOM_SENDER=251994
DEFAULT_USSD_BALANCE=*805#

# Kenya (example)
DEFAULT_TELECOM_SENDER=SAFARICOM
DEFAULT_USSD_BALANCE=*544#
```

Build region-specific APKs:
```bash
./gradlew assembleEthiopiaDebug
./gradlew assembleKenyaDebug
```

---

## 📝 Deployment Checklist

### Pre-Release

- [ ] Update version code and name
- [ ] Test all SMS parsing scenarios
- [ ] Verify permissions work correctly
- [ ] Test on multiple Android versions
- [ ] Check APK size (<10MB recommended)
- [ ] Run lint checks: `./gradlew lint`
- [ ] Run all tests: `./gradlew test`
- [ ] Review ProGuard rules

### Release

- [ ] Build signed release APK
- [ ] Test signed APK on real device
- [ ] Generate release notes
- [ ] Create Git tag
- [ ] Upload to distribution channel
- [ ] Update documentation
- [ ] Notify beta testers

### Post-Release

- [ ] Monitor crash reports
- [ ] Track user feedback
- [ ] Plan next version
- [ ] Archive build artifacts

---

## 🔗 Quick Reference

| Action | Command |
|--------|---------|
| Build debug | `./gradlew assembleDebug` |
| Build release | `./gradlew assembleRelease` |
| Install on device | `adb install -r app-debug.apk` |
| View logs | `adb logcat -s EthioStat` |
| Clear app data | `adb shell pm clear com.ethiostat.app` |
| List devices | `adb devices` |
| Deploy to device | `./scripts/deploy.sh debug device` |
| Configure device | `./scripts/configure-device.sh` |
| Build all | `./scripts/build-all.sh` |

---

## 📞 Support

For deployment issues:
- Check [README.md](README.md) for general setup
- Review [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for architecture
- Open an issue on GitHub

**Happy Deploying! 🚀**
