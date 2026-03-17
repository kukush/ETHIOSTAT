# ETHIOSTAT - Your Ethio telecom Balance at a Glance

## Overview

The **Ethio telecom SMS Summarizer** is an Android-based utility designed to parse and consolidate service messages from **Ethio telecom** (sender **251994**) and **telebirr** (sender **\*830\***) into a user-friendly dashboard. The application automates the tracking of complex, fragmented balances—such as **Monthly Internet Packages (e.g., 12GB or 4.8GB)**, **Monthly Voice bundles (e.g., 150 Min)**, and **Bonus Funds**—that are typically sent via lengthy text notifications.

## Problem Statement

Ethiopian telecom users receive complex, lengthy SMS notifications containing multiple balance packages in a single message. These messages include:
- Multiple internet packages (e.g., "Monthly Internet Package 12GB", "Weekly Youth 3.6GB")
- Voice minute balances with night bonuses
- Bonus funds and promotional offers
- Expiry dates in various formats
- Messages in both English and Amharic languages

Users struggle to:
- Extract precise balance information from lengthy texts
- Track multiple packages with different expiry dates
- Monitor financial transactions from telebirr
- Understand Amharic messages if they prefer English (and vice versa)

## Solution

ETHIOSTAT automates the extraction and visualization of:
- **Precise Metrics**: Extract data like **4728.760 MB** or **114 minute and 34 second** of voice time
- **Real-time Snapshots**: Provide a current view of all digital assets
- **Expiry Tracking**: Monitor expiry dates (e.g., **2026-04-06**) with countdown alerts
- **Financial Monitoring**: Categorize transactions from bank SMS and telebirr
- **Multilingual Support**: Parse and display messages in English and Amharic

## Key Features

### 1. Real-time SMS Parsing
- Extract Internet data (MB/GB) from complex multi-package messages
- Parse Voice minutes and seconds with night bonus packages
- Identify Bonus Funds and promotional rewards
- Support for both English and Amharic SMS messages

### 2. Balance Dashboard
- **Blue Cards** for Internet (MB) with circular progress indicators
- **Green Cards** for Voice (Minutes) showing remaining time
- **Amber Cards** for Bonus Funds (Birr) displaying financial balance
- Expiry countdown for each package

### 3. Financial Tracking
- Categorize income from bank deposits and telebirr receipts
- Track expenses for package purchases
- Daily, weekly, and monthly summaries
- Transaction history with income/expense indicators

### 4. USSD Sync
- One-tap sync via **\*805#** for balance check
- **\*804#** for package information
- Configurable USSD codes for different operators

### 5. Multilingual Interface
- Full support for English and Amharic
- Language switcher with flag icons
- Automatic SMS language detection
- Optional support for Oromiffa and Tigrinya

### 6. Configuration Management
- Configurable sender numbers (251994, \*830\*, bank names)
- Custom USSD codes
- Bank sender management (CBE, ZemenBank, AWASHBANK)
- Default values via BuildConfig, user overrides via Settings

## Privacy Commitment

**100% Local Storage, No Cloud Sync**

All data, including sensitive bank summaries and Ethio telecom balances, is stored exclusively on the device's internal memory using Room Persistence Library. The application:
- Never sends data to external servers
- Does not require internet connectivity
- Operates entirely offline
- Ensures complete user privacy

## Target Users

Ethiopian Ethio telecom subscribers who:
- Manage multiple internet and voice packages
- Use telebirr for financial transactions
- Receive SMS notifications in English or Amharic
- Want a simplified view of their balances and expenses
- Need expiry date tracking to avoid losing prepaid balances

## Example Use Cases

### Use Case 1: Multi-Package SMS
**Incoming SMS:**
```
Dear Customer, your remaining amount from Monthly Internet Package 12GB from 
telebirr to be expired after 30 days is 4728.760 MB with expiry date on 
2026-04-06 at 17:38:02; from Monthly voice 150 Min from telebirr to be 
expired after 30 days and 76 Min night package bonus valid for 30 days is 
114 minute and 34 second with expiry date on 2026-04-10 at 11:08:07
```

**ETHIOSTAT Result:**
- Internet Package: 4728.76 MB remaining, expires 2026-04-06
- Voice Package: 114 min 34 sec, expires 2026-04-10
- Night Bonus: 76 min, expires 2026-04-10

### Use Case 2: Amharic Telebirr Transaction
**Incoming SMS:**
```
በቴሌብር ስላደረጉት የገንዘብ ዝውውር እናመሰግናለን። የቴሌብር ግብይቶ 4 ነፃ የቴሌኮይን 
አስገኝቶሎታል። የቴሌኮይኖት የአገልግሎት ማቢቅያ ጊዜ በ24/03/2026 ነው።
```

**ETHIOSTAT Result:**
- Transaction: telebirr money transfer confirmed
- Reward: 4 free TeleCoins
- Expiry: 24/03/2026

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Local Database**: Room Persistence Library
- **Architecture**: MVI (Model-View-Intent)
- **Background Processing**: BroadcastReceiver, WorkManager
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Application Taglines

1. **"From SMS Chaos to Clear Insights"**
2. **"Your Ethio telecom Balance, Simplified"**
3. **"Never Miss an Expiry Date Again"**
4. **"Track Every Birr, Every MB, Every Minute"**
5. **"Smart Parsing, Smarter Spending"**
6. **"Local Storage, Total Privacy"**
7. **"One Dashboard, All Your Balances"**

## Success Metrics

- **Parsing Accuracy**: 95%+ success rate for English and Amharic SMS
- **Performance**: Process SMS in <500ms
- **User Experience**: One-screen dashboard showing all balances
- **Privacy**: Zero network calls, 100% offline operation
- **Flexibility**: Support for 3+ SMS languages
- **Reliability**: Handle 10+ packages in a single SMS message
