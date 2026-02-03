# HydroTracker

<p align="center">
  <img src="screenshots/icon.png" alt="HydroTracker Logo" width="240" height="240">
</p>

<p align="center">
  <strong>A modern, intelligent water intake tracking application</strong><br>
  No Ads, No Subscription, No Nonsense
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#installation">Installation</a> •
  <a href="#technical-details">Technical Details</a> •
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2026+-green.svg" alt="API Level">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blue.svg" alt="Kotlin Version">
  <img src="https://img.shields.io/badge/License-GPL%20v3-orange.svg" alt="License">
  <img src="https://img.shields.io/badge/Compose-BOM%202025.07.00-purple.svg" alt="Compose Version">
</p>

<p align="center">
  ![Downloads](https://img.shields.io/github/downloads/Econ01/HydroTracker/total)

</p>

---

## Features

### Core Functionality
- **Daily Water Tracking** - Log intake with 7  pre-defined container presets (100ml to 1L) or custom amounts (1-5000ml)
- **Multiple Beverage Types** - Track 9 different beverages with science-based hydration multipliers (Water, Coffee, Tea, Sports Drinks, Milk, Juice, and more)
- **Smart Goal Calculation** - Personalized daily hydration goals based on gender, age, weight, activity level, and international standards (EFSA or IOM)
- **Real-Time Progress Tracking** - Visual progress indicators with percentage tracking and goal achievement notifications

### Analytics & History
- **Comprehensive Statistics** - Daily totals, averages, largest intakes, and time-based insights
- **Multiple View Modes** - Weekly bar charts, monthly heatmaps, and yearly activity calendars
- **Streak Tracking** - Monitor consecutive days of goal achievement
- **Success Metrics** - Track total liters consumed, success rate percentages, and goals met
- **Historical Navigation** - Browse past weeks, months, and years with interactive visualizations

### Health Connect Integration
- **Health Platform Sync** - Read and write hydration data to Android Health Connect
- **Multi-App Support** - Import data from Samsung Health, Google Fit, Fitbit, Garmin, Strava, and other health apps
- **Bidirectional Sync** - Export HydroTracker data to other health apps via Health Connect
- **External Entry Management** - Identify and manage entries from different sources

### Home Screen Widgets
- **HydroProgress Widget (4x1)** - Full-width progress bar with detailed statistics
- **HydroCompact Widget (2x1)** - Minimal circular progress indicator
- **HydroLarge Widget (4x2)** - Interactive widget with quick-add buttons for instant logging
- **Dynamic Theming** - Material You dynamic colors and automatic dark mode support
- **Real-Time Updates** - Automatic synchronization with app data

### Smart Notifications
- **Context-Aware Reminders** - Intelligent notifications that respect your sleep schedule
- **Customizable Styles** - Choose from Gentle & Caring, Motivating & Energetic, or Simple & Clean
- **Adaptive Scheduling** - Automatic reminder intervals based on your wake/sleep times
- **Goal-Based Logic** - Stops reminders once daily goal is achieved
- **Boot Persistence** - Notifications automatically resume after device restart

### Personalization & Themes
- **Material 3 Expressive Design** - Latest Material Design with smooth spring-based animations
- **Dark Mode Options** - System default, always light, or always dark with optional AMOLED/pure black mode
- **Dynamic Colors** - Material You theming from wallpaper colors (Android 12+)
- **Custom Color Themes** - HydroTracker Blue water-themed palette
- **Display Preferences** - Configurable week start day (Sunday or Monday)

### User Profile Management
- **Comprehensive Profile** - Name, gender, age group, weight, activity level, and profile photo
- **Schedule Settings** - Custom wake-up and sleep times for intelligent reminder scheduling
- **Activity Levels** - Five levels from Sedentary to Very Active with automatic goal adjustments
- **Multiple Age Groups** - Age-appropriate recommendations for 18-30, 31-50, 51-60, and 60+ age ranges
- **Gender-Inclusive Options** - Male, Female, and Prefer not to say with appropriate calculations

### Entry Management
- **Quick Logging** - One-tap logging with preset containers or custom amounts
- **Entry Editing** - Edit existing entries with full beverage type and amount control
- **Swipe Gestures** - Swipe left to delete, swipe right to edit entries
- **External Entry Indicators** - Clear warnings for entries imported from Health Connect

### Scientific Foundation
- **Evidence-Based Multipliers** - Hydration effectiveness based on peer-reviewed Beverage Hydration Index (BHI) research
- **International Standards** - Support for EFSA (European) and IOM (US) hydration guidelines
- **Activity-Based Adjustments** - Multipliers from 1.0x to 1.5x based on activity level
- **Beverage Science** - Accurate hydration calculations for different beverage types (Sports Drinks 1.1x, Milk 1.5x, ORS 1.5x, Juice 1.3x)

---

## Screenshots

<p align="center">
  <img src="screenshots/onboarding.png" alt="Onboarding Flow" width="200">
  <img src="screenshots/home-screen.png" alt="Home Screen" width="200">
  <img src="screenshots/add-water.png" alt="Add Water" width="200">
  <img src="screenshots/analytics.png" alt="Analytics" width="200">
</p>

### Themes & Notifications
| Light Mode | Dark Mode | Notification |
|-----------|----------------|--------------|
| <img src="screenshots/home-screen.png" alt="Light Mode" width="180"> | <img src="screenshots/dark.png" alt="Dark Mode" width="180"> | <img src="screenshots/notification.png" alt="Notification" width="180"> |

---

## Installation

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.cemcakmak.hydrotracker">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">
  </a>
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80" style="opacity: 0.4; filter: grayscale(100%);">
</p>

<p align="center">
  <em>F-Droid release coming soon</em>
</p>

<p align="center">
  Or download the APK directly from the releases page:<br>
  <a href="https://github.com/Econ01/HydroTracker/releases">
    <img src="https://img.shields.io/github/v/release/Econ01/HydroTracker?label=Download&style=for-the-badge&logo=github&color=blue" alt="GitHub Release">
  </a>
</p>

### Alternative: Build It Yourself

#### Prerequisites
- **Android Studio** - Koala Feature Drop | 2024.1.2 or later
- **Android SDK** - API 26+ (Minimum), API 36 (Target)
- **Kotlin** - 2.0.21+
- **Gradle** - 8.12.0+
- **Java** - JDK 11 or higher

#### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Econ01/HydroTracker.git
   cd HydroTracker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

#### Windows Users
Use `gradlew.bat` instead of `./gradlew` for all commands above.

---

## Privacy & Security

### Data Protection
- **100% Local Storage** - All data stored locally using Room SQLite database
- **No Cloud Sync** - Your data never leaves your device
- **No Analytics** - Zero usage tracking or telemetry
- **No Account Required** - No sign-up, no login, no personal data collection
- **Offline First** - Full functionality without internet connection
- **Health Connect Privacy** - Optional integration, you control what data is shared

### Permissions Usage
- **Notifications** - Hydration reminders only (optional)
- **Exact Alarms** - Precise notification timing (optional)
- **Camera** - Profile photo capture (optional, stored locally only)
- **Health Connect** - Read/write hydration data to Health Connect platform (optional)
- **Post Notifications** - Required for Android 13+ to display reminders

---

## Technical Details

### Architecture & Stack
- **UI Framework** - Jetpack Compose with Material 3 Expressive APIs
- **Language** - Kotlin 2.0.21 with JVM Target 11
- **Database** - Room SQLite with Flow-based reactive queries
- **Async Operations** - Kotlin Coroutines and Flow
- **Navigation** - Jetpack Navigation Compose
- **Dependency Injection** - Manual dependency injection with factories

### Key Libraries & Components
- **Compose BOM** - 2025.07.00
- **Material 3** - Expressive APIs with spring-based animations
- **Room Database** - Version 2.6+ with KSP code generation
- **Health Connect** - androidx.health.connect.client
- **Navigation Compose** - Type-safe navigation
- **Lifecycle ViewModel** - State management with Compose integration

### App Architecture
- **MVVM Pattern** - ViewModel + Repository + Database layers
- **Repository Pattern** - Clean separation between data and UI layers
- **Database Entities** - WaterIntakeEntry and DailySummary tables
- **Data Models** - UserProfile, ContainerPreset, BeverageType, ThemePreferences

### Database Schema
```
WaterIntakeEntry
├── id (Primary Key)
├── amount (Int, in milliliters)
├── timestamp (Long, in milliseconds)
├── date (String, yyyy-MM-dd)
├── beverageType (String)
├── isExternal (Boolean)
└── externalSourceNote (String, nullable)

DailySummary
├── date (Primary Key, yyyy-MM-dd)
├── totalAmount (Int, in milliliters)
├── goalAmount (Int, in milliliters)
├── entryCount (Int)
└── lastUpdated (Long, in milliseconds)
```

### Features Implementation
- **Notifications** - AlarmManager with PendingIntent for exact scheduling
- **Widgets** - GlanceAppWidget for Material 3 home screen widgets
- **Health Connect** - HealthConnectClient with Hydration records API
- **Image Handling** - Camera and gallery integration with local file storage
- **Haptic Feedback** - HapticFeedbackType for tactile interactions
- **Theme System** - Dynamic color support with Material You

### Build Configuration
- **Min SDK** - 26 (Android 8.0 Oreo)
- **Target SDK** - 36 (Android 15)
- **Compile SDK** - 36
- **Version Code** - 18
- **Version Name** - 1.0.0

### Supported Devices
- Android 8.0 (API 26) and higher
- Phone and tablet form factors
- Portrait and landscape orientations
- Material You dynamic colors on Android 12+
- Health Connect on Android 9+ (API 28+)

---

## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE.md](app/src/main/assets/LICENSE.md) file for details.

### What this means:
- **Use** - Use this code for any purpose
- **Study** - Examine how it works
- **Share** - Distribute the app
- **Modify** - Make changes and improvements
- **Copyleft** - Derivative works must also be GPL v3.0

---

## Support

### Get Help
- **Bug Reports & Feature Requests** - [GitHub Issues](https://github.com/Econ01/HydroTracker/issues)

### Support Development

<p align="center">
  <a href="https://www.paypal.com/donate/?hosted_button_id=CQUZLNRM79CAU">
    <img src="screenshots/paypal-banner.png" alt="Donate with PayPal" height="50">
  </a>
  &nbsp;&nbsp;
  <a href="https://buymeacoffee.com/thegadgetgeek">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="50">
  </a>
</p>

---

## Acknowledgments

### Research & Science
Beverage hydration effectiveness based on peer-reviewed research:
- "A randomized trial to assess the potential of different beverages to affect hydration status" (American Journal of Clinical Nutrition)
- Beverage Hydration Index (BHI) methodology
- EFSA (European Food Safety Authority) hydration guidelines
- IOM (Institute of Medicine) dietary reference intakes

### Open Source
Built with modern Android development tools and libraries from the Android Open Source Project and JetBrains.

---

<p align="center">
  <strong>HydroTracker - Stay hydrated, stay healthy!</strong><br>
  <em>Developed by Ali Cem Çakmak</em>
</p>