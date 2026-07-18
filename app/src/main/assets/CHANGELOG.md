[1.1]
Added
### Settings
- Complete Settings Hub with dedicated sub-screens for Appearance, Display & Locale, Hydration & Health, Notifications, and more
#### About
- Image cropping screen for profile photos in both settings and onboarding
#### Appearance
- Label types for navigation bar:
  1. Always shown
  2. Selected only
  3. Off
- Scroll edge effects:
  1. Blurred
  2. Scrim
  3. Transparent
- Custom font selection:
  1. Google Sans Flex (Default)
  2. System Default
  3. Outfit
  4. DM Sans
  5. JetBrains Mono
- Auto-hiding navigation bar and FAB on scroll
- Extended colour palette with success and warning semantic colours
- Haptic feedback toggle
- Optional subtle borders around cards in AMOLED mode
- Font warning for Xiaomi group devices (HyperOS/MIUI) where custom fonts don't work as intended
- Beverage Colours toggle under Appearance ▸ Home screen (default off)
- When enabled, the home carousel, daily entry list, beverage selectors and custom entry dialogue take on each beverage's colour
#### Display & Locale
- Multi-language and localization support
- Multiple date format options:
  1. dd MMM YYYY
  2. MMM dd, YYYY
  3. YYYY-MM-DD
  4. System format
- Multiple week-start options (Monday-Sunday)
- Multiple container volume unit options:
  1. Metric
  2. British Imperial System
  3. US Customary System:
     - US fluid ounces
#### Quick Add Customization
- Custom beverage type creation and management
- Custom container icons
#### Notifications
- Detailed notification examples
- Three different types of notifications:
  1. Gentle
  2. Energetic
  3. Simple
##### Reminder Schedule
- Detailed explanation on how the notification system works
- Ability to choose day end:
  1. Sleep time (Default)
  2. Midnight
- Ability to choose notification mode:
  1. Automatic (Default)
  2. Custom interval
- Progress-centric reminder notifications that show your daily goal progress
- Quick-add actions directly from the reminder notification
- Optional daily hydration fun facts
#### Data Management
- Export your entries as CSV or JSON
- Import CSV or JSON backups with automatic duplicate detection
- Delete entries before a chosen date
- Delete local data only, Health Connect data only, or everything
#### Support Development
- Support Development page
- Rate on Google Play and share buttons
#### About
- About page
- Contributors
- Automatic update checker
- Third-part licenses
#### Developer Options
- Developer options only visible to developers
### Miscellaneous
- Custom haptic feedback engine with multi-tier OEM-aware fallback
- Backdrop blur visual effects module
### Widget
- Complete rebuild of the home-screen widget using Jetpack Glance
- Large widget now shows a progress ring and up to three quick-add cards
- Material You dynamic colours on Android 12+ with a fallback HydroTracker water-themed palette
- Widget refreshes automatically whenever you add, edit or delete an entry
- New Widget settings under Appearance for choosing dynamic colours, transparent background, and pure black or white surfaces
- Customizable quick-add buttons: pin your own container and beverage combos or let the widget pick your most-used ones
- Quick-add cards automatically suggest your most common beverage and container combinations
- Generated picker previews on Android 15; static preview fallback on older devices
### Home
- FAB menu with three options:
  1. Add a custom entry
  2. Add a new container type
  3. Add a new beverage type
### History
- Animated goal-met badge in the history screen
- Staggered bar animations for weekly, monthly, and yearly charts
- Ability to add, remove, and edit past entries from History page
### Statistics
- New Statistics tab with all-time insights:
  - Overview: current streak, daily average, total intake, success rate and days tracked
  - Beverage breakdown doughnut chart with dedicated colours for each beverage type
  - Container usage card showing your most-used containers

Changed
- Switched default font to Google Sans Flex
- Migrated navigation to Navigation 3 with predictive back gesture and scrim/blur animation
- Switched to single Scaffold architecture
- Replaced old Material icons with Material Symbols throughout
- Overhauled History screen UI with new chart designs and period transition animations
- Redesigned daily entry list with tonal elevation and animations
- Overhauled notification settings UI with updated time pickers
- Overhaul home page with container carousel, new beverage selector and more
- Update every sheet and dialogue UI
- Switched to squircle shapes throughout the app
- Smoother tab switch animations with animated top and bottom bars
- Larger FAB icon with a rotation animation
- Replaced in-app snackbars with system toast messages
- Profile photo selection now uses the Android Photo Picker — no storage permission required
- Overhauled the notification system with goal-aware, progress-centric reminders
- Improved Health Connect sync with faster imports and a pull-to-refresh indicator on Home
- Redesigned the update available screen
- Updated edit and delete entry dialogues
- Updated libraries
- Replaced the Compact and Progress widgets with the redesigned Large widget

Fixed
- Fixed FAB visibility and animation glitches
- Fixed "Sleep time" day end so the day ends at your sleep time instead of your wake-up time; existing entries and summaries are corrected automatically
- Fixed widget quick-add actions not updating the widget straight away

[1.0.6.1]
• Small preparations for F-Droid release

[1.0.6]
Added
• Widgets and notifications now respect the system time configuration
• Add manual restore button to the HealthConnect page. Now you can retrieve your old entries if you delete and re-install the app
Fixed
• Fix Health Connect read pagination — now reads all pages instead of only the first page
• Fix restored entry date assignment to use user-day (wake-up time) instead of calendar day
• Fix duplicate detection during import/restore:
• Now queries the stored date field directly instead of recomputing from timestamp
• Compares effective hydration amounts instead of raw amounts
• Added fast-path exact match via healthConnectRecordId
• Fix healthConnectRecordId storage mismatch between write and restore paths
• Fix History screen only showing last 30 days — now displays all historical data
• Fix Health Connect Data screen only showing last 30 days — now displays all entries

[1.0.5]
Added
• Now you can remove and re-order beverages

[1.0.4]
Added
• Ability to add custom containers
• Ability to edit preset containers
• Ability to reset default containers

[1.0.3]
Added
• Add widgets

[1.0.1]
Added
• Add support for Medium and High Colour Contrast added with Android 16.1
• Add ability to change entry times
Updated
• Update Sources
• Update Privacy Policy
Fixed
• Fix dynamic colour problems

[1.0.0]
Added
• Add different beverage types

[0.9.10]
Added
• Add support for devices up to Android 8

[0.9.9]
Added
• Upgraded Libraries
• Update Room version
Fixed
• Bug Fixes

[0.9.7]
Added
• Add Health Connect support 🥲
Fixed
• Reduce padding in profile page
• Fixed notification permission is not automatically getting detected

[0.9.5]
Added
• Added privacy policy to the settings
• Sources section to settings
• Improve water intake amounts based on current research
• Add an option to choose from EFSA or IOM data
Fixed
• Fix: Age Group button opens the correct dialogue rather than gender dialogue

[0.9.4]
Fixed
• Fix notification problem

[0.9.3]
Added
• Bouncy animations to switches
• New pure black mode
Fixed
• Colours of the donation buttons are applied correctly now
• Reduce side padding on settings page

[0.9.1]
Added
• Dynamic version display in Settings screen
• Automatic version management from Git
Fixed
• Notification issues with app signing
• Settings timer reset problems

[0.9.0]
Added
• Initial release candidate
• Hydration tracking functionality
• Notification system
• Settings and profile management