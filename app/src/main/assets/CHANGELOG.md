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
     - US cups
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
### Home
- FAB menu with three options:
  1. Add a custom entry
  2. Add a new container type
  3. Add a new beverage type
### History
- Animated goal-met badge in the history screen
- Staggered bar animations for weekly, monthly, and yearly charts
- Ability to add, remove, and edit past entries from History page

Changed
- Switched default font to Google Sans Flex
- Migrated navigation to Navigation 3 with predictive back gesture and scrim/blur animation
- Switched to single Scaffold architecture
- Replaced old Material icons with Material Symbols throughout
- Overhauled History screen UI with new chart designs and period transition animations
- Redesigned daily entry list with tonal elevation and animations
- Overhauled notification settings UI with updated time pickers
- Overhaul home page with container carousel, new beverage selector and more

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