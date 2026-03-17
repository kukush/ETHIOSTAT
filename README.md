# ETHIOSTAT - Ethio telecom Balance Tracker

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room-orange.svg)](https://developer.android.com/training/data-storage/room)

**ETHIOSTAT** is a powerful Android application that automatically parses Ethio telecom and telebirr SMS messages to provide users with a clear, real-time view of their internet, voice, and financial balances. Supporting both English and Amharic languages, the app transforms complex SMS notifications into beautiful, actionable insights.

---

## 🌟 Features

### ✅ **Real-time SMS Parsing**
- Automatically extracts balance information from Ethio telecom (251994) and telebirr (*830*) SMS
- Supports complex multi-package messages (Internet + Voice + Night Bonus)
- Parses both **English** and **Amharic** SMS messages
- Smart language detection with Unicode range analysis

### 📊 **Beautiful Dashboard**
- **Blue Cards** for Internet packages (MB/GB)
- **Green Cards** for Voice packages (Minutes)
- **Amber Cards** for Bonus Funds (Birr)
- Circular progress indicators showing usage percentage
- Expiry countdown with color-coded alerts

### 💰 **Financial Tracking**
- Automatic categorization of income and expenses
- Transaction history from telebirr and bank SMS
- Daily, weekly, monthly summaries
- Net balance calculation with visual indicators

### 🌐 **Multilingual Support**
- Full UI localization in English and Amharic
- Automatic SMS language detection
- User-selectable app language
- Extensible for Oromiffa and Tigrinya

### ⚙️ **Configurable Settings**
- Customizable sender numbers (telecom, telebirr, banks)
- Configurable USSD codes (*805#, *804#)
- BuildConfig defaults with in-app overrides
- Language parser toggles

### 🔒 **Privacy First**
- **100% offline** - No internet connectivity required
- All data stored locally using Room database
- No cloud sync or external data transmission
- Sensitive balance data never leaves your device

### 📞 **USSD Integration**
- One-tap sync via *805# for balance check
- *804# for package information
- Automatic SMS processing after USSD calls

---

## 🏗️ Architecture

ETHIOSTAT follows **Clean Architecture** principles with the **MVI (Model-View-Intent)** pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  - Jetpack Compose UI                   │
│  - ViewModels (MVI)                     │
│  - State Management                     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Domain Layer                    │
│  - Models (BalancePackage, Transaction) │
│  - Use Cases (ParseSms, SyncBalance)    │
│  - Repository Interfaces                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Data Layer                     │
│  - Room Database (Entities, DAOs)       │
│  - SMS Parsers (English, Amharic)       │
│  - Repository Implementation            │
└─────────────────────────────────────────┘
```

### Key Components

- **MVI Pattern**: Unidirectional data flow for predictable state management
- **Room Database**: Local persistence for balances, transactions, and configuration
- **Regex Parsers**: Language-specific SMS extraction engines
- **BroadcastReceiver**: Real-time SMS interception
- **Jetpack Compose**: Modern, declarative UI framework
- **Material 3**: Latest design system with dynamic theming

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Kotlin 1.9.0+
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ethiostat.git
   cd ethiostat
   ```

2. **Open in Android Studio**
   ```
   File → Open → Select project directory
   ```

3. **Configure default senders** (Optional)
   
   Edit `gradle.properties`:
   ```properties
   DEFAULT_TELECOM_SENDER=251994
   DEFAULT_TELEBIRR_SENDER=*830*
   DEFAULT_BANK_SENDERS=CBE,ZemenBank,AWASHBANK
   DEFAULT_USSD_BALANCE=*805#
   ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Grant Permissions**
   - SMS Read & Receive
   - Phone Call (for USSD)

---

## 📱 Usage

### Initial Setup

1. **Launch the app** - Grant SMS and Call permissions
2. **Sync balances** - Tap the floating "Sync Now" button
3. **View dashboard** - See all your packages and balances

### Reading SMS Automatically

The app automatically processes incoming SMS from:
- **Ethio telecom** (251994)
- **telebirr** (*830*)
- **Banks** (CBE, ZemenBank, etc.)

### Manual Sync

Tap the **Sync Now** button to trigger:
- USSD call to *805# for balance check
- Automatic SMS capture when response arrives

### Changing Language

1. Go to **Settings**
2. Select **Language**
3. Choose **English** or **አማርኛ** (Amharic)

---

## 📝 Example SMS Parsing

### English Multi-Package SMS

**Input:**
```
Dear Customer, your remaining amount from Monthly Internet Package 12GB 
from telebirr to be expired after 30 days is 4728.760 MB with expiry 
date on 2026-04-06 at 17:38:02; from Monthly voice 150 Min from 
telebirr to be expired after 30 days is 114 minute and 34 second with 
expiry date on 2026-04-10 at 11:08:07
```

**Extracted:**
- Internet: 4728.76 MB (expires 2026-04-06)
- Voice: 114 min 34 sec (expires 2026-04-10)

### Amharic telebirr Transaction

**Input:**
```
የቴሌብር ግብይቶ 4 ነፃ የቴሌኮይን አስገኝቶሎታል። የቴሌኮይኖት የአገልግሎት 
ማብቅያ ጊዜ በ24/03/2026 ነው።
```

**Extracted:**
- TeleCoin Reward: 4 coins
- Expiry: 2026-03-24

---

## 🗂️ Project Structure

```
app/
├── src/main/java/com/ethiostat/app/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/           # Room DAOs
│   │   │   ├── entity/        # Room entities
│   │   │   └── EthioStatDatabase.kt
│   │   ├── parser/
│   │   │   ├── EnglishSmsParser.kt
│   │   │   ├── AmharicSmsParser.kt
│   │   │   └── MultilingualSmsParser.kt
│   │   └── repository/
│   │       └── EthioStatRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/             # Domain models
│   │   ├── repository/        # Repository interfaces
│   │   └── usecase/           # Use cases
│   ├── receiver/
│   │   └── SmsReceiver.kt     # BroadcastReceiver
│   └── ui/
│       ├── components/        # Reusable Compose components
│       ├── dashboard/         # Dashboard screen & ViewModel
│       └── theme/             # Material 3 theme
├── docs/
│   ├── PROJECT_DESCRIPTION.md
│   ├── MVI_ARCHITECTURE.md
│   ├── MULTILINGUAL_GUIDE.md
│   ├── SMS_PARSING_RULES.md
│   └── CONFIGURATION_MANAGEMENT.md
└── gradle.properties          # Configuration defaults
```

---

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

**Coverage:**
- SMS Parser tests (English & Amharic)
- ViewModel state transition tests
- Use case tests
- Repository tests

### Example Test

```kotlin
@Test
fun `parse complex multi-package message`() {
    val sms = "Dear Customer, your remaining amount from..."
    val result = englishParser.parse(sms, "251994")
    
    assertEquals(2, result.packages.size)
    assertEquals(PackageType.INTERNET, result.packages[0].packageType)
}
```

---

## 🔧 Configuration

### BuildConfig (Deployment)

Set defaults in `gradle.properties`:

```properties
DEFAULT_TELECOM_SENDER=251994
DEFAULT_TELEBIRR_SENDER=*830*
DEFAULT_USSD_BALANCE=*805#
DEFAULT_USSD_PACKAGES=*804#
DEFAULT_BANK_SENDERS=CBE,ZemenBank,AWASHBANK
```

### Runtime Configuration

Users can override via **Settings** screen:
- Add/remove sender numbers
- Customize USSD codes
- Enable/disable language parsers

---

## 🎨 Design

### Color Palette

| Element | Color | Usage |
|---------|-------|-------|
| Internet | Blue (#2196F3) | Internet packages |
| Voice | Green (#4CAF50) | Voice packages |
| Bonus Funds | Amber (#FFC107) | Bonus funds & rewards |
| Success | Green (#66BB6A) | Income, success states |
| Error | Red (#F44336) | Expenses, errors |

### Typography

- **Headings**: Bold, system default
- **Body**: Regular, 14-16sp
- **Labels**: Medium, 12sp

---

## 🌍 Localization

### Supported Languages

- 🇬🇧 **English** - Full support
- 🇪🇹 **Amharic (አማርኛ)** - Full support
- 🇪🇹 **Oromiffa** - Planned
- 🇪🇹 **Tigrinya** - Planned

### Adding a Language

1. Create `values-{code}/strings.xml`
2. Implement `{Language}SmsParser.kt`
3. Add to `AppLanguage` enum
4. Update `MultilingualSmsParser`

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👨‍💻 Author

**Getahun Tesfaye**

---

## 🙏 Acknowledgments

- Ethiopian telecom users for inspiration
- Material Design team for beautiful components
- Jetpack Compose team for modern UI framework
- Room Persistence Library for reliable local storage

---

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Email: support@ethiostat.app

---

## 🔮 Roadmap

- [ ] Settings UI implementation
- [ ] Comprehensive unit tests
- [ ] Widget support for home screen
- [ ] Export data to CSV
- [ ] Charts and visualizations
- [ ] Dark mode enhancements
- [ ] Notification summaries
- [ ] ML-based parser improvements

---

**Built with ❤️ for Ethiopian telecom users**
