# ETHIOSTAT - Ethio telecom Balance Tracker

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)

**ETHIOSTAT** is a powerful Android application that automatically parses Ethio telecom and telebirr SMS messages to provide users with a clear, real-time view of their internet, voice, and financial balances.

## 🚀 Quick Start

```bash
git clone https://github.com/yourusername/ethiostat.git
cd ethiostat
./gradlew assembleDebug
```

## 🌟 Key Features

- **Real-time SMS Parsing** - Automatically extracts balance information from Ethio telecom and telebirr SMS
- **Complete Package Parsing** - Identifies and tracks all your internet, voice, SMS, and bonus packages.
- **Beautiful Dashboard** - Color-coded cards for Internet, Voice, and Bonus packages
- **Transaction Manager** - Automatically tracks your banks and mobile money services on up to 6 simultaneous configurable sources. Clear / reset transactions by date as needed.
- **Multilingual Support** - Full support for English, Amharic, and Oromo.
- **Privacy First** - 100% offline, no internet connectivity required
- **USSD Integration** - One-tap sync via *804# for an integrated multi-package balance check

## 📚 Documentation

For complete documentation, please visit the `docs/` directory:

- **[📖 Complete Project Overview](docs/PROJECT_OVERVIEW.md)** - Full features, architecture, and usage guide
- **[🚀 Quick Start Guide](docs/QUICK_START.md)** - Step-by-step setup instructions
- **[🚢 Deployment Guide](docs/DEPLOYMENT_GUIDE.md)** - Build and deployment instructions
- **[⚙️ Implementation Summary](docs/IMPLEMENTATION_SUMMARY.md)** - Technical architecture details
- **[📋 Coding Standards](docs/CODING_STANDARDS.md)** - Development guidelines and conventions

## 🏗️ Architecture

ETHIOSTAT follows **Clean Architecture** with **MVI (Model-View-Intent)** pattern:
- **Presentation Layer** - Jetpack Compose UI with ViewModels
- **Domain Layer** - Use Cases and Repository Interfaces  
- **Data Layer** - Room Database and SMS Parsers

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow the [Coding Standards](docs/CODING_STANDARDS.md)
4. Submit a Pull Request

## 📄 License

MIT License - see LICENSE file for details.

---

**Built with ❤️ for Ethiopian telecom users**
