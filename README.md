# HydroTracker

<p align="center">
  <img src="screenshots/icon.png" alt="HydroTracker Logo" width="240" height="240">
</p>

<p align="center">
  <strong>A modern and private water intake tracking application</strong><br>
  No Ads, No Subscription, No Internet
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
  <img src="https://img.shields.io/github/downloads/Econ01/HydroTracker/total?color=blue&label=Downloads" alt="Downloads"/>
</p>


---

## Features

### Core Functionality
- Daily Water Tracking
- Multiple Beverage Types
- Smart Goal Calculation
- Real-Time Progress Tracking
- Intelligent Reminders

### Analytics & History
- **Comprehensive Statistics:** Daily totals, averages, largest intakes, and time-based insights
- **Multiple View Modes:** Weekly bar charts, monthly heatmaps, and yearly activity calendars
- **Streak Tracking:** Monitor consecutive days of goal achievement
- **Success Metrics:** Track total liters consumed, success rate percentages, and goals met
- **Historical Navigation:** Browse past weeks, months, and years with interactive visualizations

### Health Connect Integration
- **Health Platform Sync:** Read and write hydration data to Android Health Connect.
- **Multi-App Support:** Import data from Samsung Health, Google Fit, Fitbit, Garmin, Strava, and other health apps (*As long as they use Health Connect API*)
- **External Entry Management:** Identify and manage entries from different sources. The app can **read** messy data from other apps that you have

### Home Screen Widgets
Available widgets:
- HydroProgress Widget (4x1)
- HydroCompact Widget (2x1)
- HydroLarge Widget (4x2)

All of them supports:
- Dynamic Theming
- Real-Time Updates

### Smart Notifications
- **Context-Aware Reminders:** Intelligent notifications that respect your sleep schedule
- **Adaptive Scheduling:** Automatic reminder intervals based on your wake/sleep times
- **Goal-Based Logic:** Stops reminders once daily goal is achieved
- **Boot Persistence:** Notifications automatically resume after device restart

### Personalization & Themes
- Material 3 Expressive Design
- Dark Mode Options
- Dynamic Colors
- And some extra stuff 😏

### Scientific Foundation
- **Evidence-Based Multipliers:** Hydration effectiveness based on peer-reviewed Beverage Hydration Index (BHI) research
- **International Standards:** Support for EFSA (European) and IOM (US) hydration guidelines
- **Activity-Based Adjustments:** Multipliers from 1.0x to 1.5x based on activity level
- **Beverage Science:** Accurate hydration calculations for different beverage types (Sports Drinks 1.1x, Milk 1.5x, ORS 1.5x, Juice 1.3x)

You can find all of the source inside the **settings page** of the app or from [here](app/src/main/assets/sources.md).

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

*Current visuals of the app might differ from these screenshots.*

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

---

## Support

### Get Help
- **Bug Reports & Feature Requests** - [GitHub Issues](https://github.com/Econ01/HydroTracker/issues)

<div align="center">
  <h3>Support this Project</h3>
  <a href="https://www.buymeacoffee.com/thegadgetgeek">
    <img src="https://img.shields.io/badge/Buy%20Me%20A%20Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black" alt="Buy Me A Coffee" />
  </a>
  &nbsp;
  <a href="https://www.paypal.me/cmckmk">
    <img src="https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="PayPal" />
  </a>
</div>

---

## Acknowledgments

### Research & Science
Beverage hydration effectiveness based on peer-reviewed research:
- "A randomized trial to assess the potential of different beverages to affect hydration status" (American Journal of Clinical Nutrition)
- Beverage Hydration Index (BHI) methodology
- EFSA (European Food Safety Authority) hydration guidelines
- IOM (Institute of Medicine) dietary reference intakes

<p align="center">
  <img src="stats/overview.png" alt="Project Stats" width="800">
</p>


## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE.md](app/src/main/assets/LICENSE.md) file for details.

### What this means:
- **Use:** Use this code for any purpose
- **Study:** Examine how it works
- **Share:** Distribute the app
- **Modify:** Make changes and improvements
- **Copyleft:** Derivative works **must** also be GPL v3.0

<p align="center">
  <strong>HydroTracker - Stay hydrated, stay healthy!</strong><br>
  <em>Developed by Ali Cem Çakmak</em>
</p>