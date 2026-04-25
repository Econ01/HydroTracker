# Changelog

## [1.0.6]
### Added
- Widgets and notifications now respect the system time configuration
- Add manual restore button to the HealthConnect page. Now you can retrieve your old entries if you delete and re-install the app

### Fixed
- Fix Health Connect read pagination — now reads all pages instead of only the first page
- Fix restored entry date assignment to use user-day (wake-up time) instead of calendar day
- Fix duplicate detection during import/restore:
  - Now queries the stored `date` field directly instead of recomputing from timestamp
  - Compares effective hydration amounts instead of raw amounts
  - Added fast-path exact match via `healthConnectRecordId`
- Fix `healthConnectRecordId` storage mismatch between write and restore paths
- Fix History screen only showing last 30 days — now displays all historical data
- Fix Health Connect Data screen only showing last 30 days — now displays all entries

## [1.0.5]
### Added
- Now you can remove and re-order beverages

## [1.0.4]
### Added
- Ability to add custom containers
- Ability to edit preset containers
- Ability to reset default containers

## [1.0.3] - 2025.10.17
### Added
- Add widgets

## [1.0.1] - 2025.10.17
### Added
- Add support for Medium and High Color Contrast added with Android 16.1
- Add ability to change entry times

### Updated
- Update Sources
- Update Privacy Policy

### Fixed
- Fix dynamic color problems

## [1.0.0] - 2025.09.27
### Added
- Add different beverage types

## [0.9.10] - 2025.09.26
### Added
- Add support for devices up to Android 8

## [0.9.9] - 2025.09.25
### Added
- Upgraded Libraries
- Update Room version

### Fixed
- Bug Fixes

## [0.9.7] - 2025.09.21
### Added
- Add Health Connect support 🥲

### Fixed
- Reduce padding in profile page
- Fixed notification permisison is not automatically getting detected

## [0.9.5] - 2025.09.19
### Added
- Added privacy policy to the settings
- Sources section to settings
- Improve water intake amounts based on current research
- Add an option to choose from EFSA or IOM data

### Fixed
- Fix: Age Group button opens the correct dialog rather than gender dialog

## [0.9.4] - 2024-09-18
### Fixed
- Fix notification problem

## [0.9.3] - 
### Added
- Bouncy animations to swtiches
- New pure black mode

### Fixed
- Colors of the donation buttons are applied correctly now
- Reduce side padding on settings page

## [0.9.1] - 2024-09-16
### Added
- Dynamic version display in Settings screen
- Automatic version management from Git

### Fixed
- Notification issues with app signing
- Settings timer reset problems

## [0.9.0] - 2024-09-16
### Added
- Initial release candidate
- Hydration tracking functionality
- Notification system
- Settings and profile management