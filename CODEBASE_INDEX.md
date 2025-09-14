# Morsecall - Codebase Index

## Project Overview
**Morsecall** is an Android application built with Jetpack Compose that allows users to trigger ringtones through consecutive tap gestures. The app provides a morse code-inspired interface for activating ringtones with customizable sensitivity settings.

## Project Structure

```
Morsecall/
├── app/
│   ├── build.gradle.kts                 # App-level build configuration
│   ├── proguard-rules.pro               # ProGuard rules for release builds
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # App manifest with permissions and activities
│       │   ├── java/com/example/morsecall/
│       │   │   ├── MainActivity.kt      # Main activity with navigation and UI
│       │   │   ├── Constants.kt         # App constants and preferences name
│       │   │   ├── SettingsScreen.kt    # Settings screen with configuration options
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt         # Color definitions
│       │   │       ├── Theme.kt         # Material3 theme configuration
│       │   │       └── Type.kt          # Typography definitions
│       │   └── res/                     # Android resources
│       │       ├── drawable/            # Drawable resources
│       │       ├── mipmap-*/            # App icons for different densities
│       │       ├── values/              # String resources and themes
│       │       └── xml/                 # XML configuration files
│       ├── androidTest/                 # Android instrumentation tests
│       └── test/                        # Unit tests
├── build.gradle.kts                     # Project-level build configuration
├── gradle/
│   └── libs.versions.toml               # Version catalog for dependencies
├── gradle.properties                    # Gradle properties
├── settings.gradle.kts                  # Gradle settings
└── README.md                            # Project documentation
```

## Core Components

### 1. MainActivity.kt
**Location**: `app/src/main/java/com/example/morsecall/MainActivity.kt`

**Purpose**: Main entry point and UI controller for the application.

**Key Features**:
- **Navigation**: Implements Jetpack Navigation with two screens (Main and Settings)
- **Tap Detection**: Monitors consecutive taps within a 3-second window
- **Ringtone Management**: Plays/stops ringtones based on tap patterns
- **State Management**: Tracks app activation, tap counts, and ringtone status
- **Activity Logging**: Maintains a log of recent tap activities

**Key Functions**:
- `MainActivity.onCreate()`: Sets up navigation and Compose UI
- `MainScreen()`: Main UI composable with tap interface
- Tap counting logic with consecutive tap detection
- Ringtone playback control

### 2. SettingsScreen.kt
**Location**: `app/src/main/java/com/example/morsecall/SettingsScreen.kt`

**Purpose**: Configuration screen for customizing app behavior.

**Key Features**:
- **Ringtone Selection**: Allows users to pick custom ringtones
- **Tap Sensitivity**: Adjustable dot duration (100-500ms) with automatic dash calculation
- **Trigger Count**: Configurable number of consecutive taps needed (1-10)
- **Persistent Storage**: Uses SharedPreferences to save settings

**Key Functions**:
- `saveRingtoneUri()` / `loadRingtoneUri()`: Ringtone preference management
- `saveDotDuration()` / `loadDotDuration()`: Tap timing configuration
- `saveTapTriggerCount()` / `loadTapTriggerCount()`: Trigger threshold settings
- `getRingtoneTitle()`: Helper to display ringtone names

### 3. Constants.kt
**Location**: `app/src/main/java/com/example/morsecall/Constants.kt`

**Purpose**: Application-wide constants.

**Contents**:
- `PREFS_NAME`: SharedPreferences storage key ("morsecall_prefs")

### 4. Theme System
**Location**: `app/src/main/java/com/example/morsecall/ui/theme/`

**Components**:
- **Theme.kt**: Material3 theme with dynamic color support (Android 12+)
- **Color.kt**: Color palette definitions
- **Type.kt**: Typography configurations

## Application Architecture

### Navigation Structure
```
AppDestinations
├── MAIN_SCREEN ("main")     # Primary tap interface
└── SETTINGS_SCREEN ("settings")  # Configuration screen
```

### State Management
- **Local State**: Uses `remember` and `mutableStateOf` for UI state
- **Persistent State**: SharedPreferences for user settings
- **Navigation State**: Jetpack Navigation for screen management

### Data Flow
1. **User Interaction** → Tap detection in MainScreen
2. **State Update** → Consecutive tap counting
3. **Threshold Check** → Compare against configured trigger count
4. **Action Execution** → Play ringtone if threshold met
5. **Logging** → Update activity log

## Key Features

### 1. Tap-Based Ringtone Trigger
- Detects consecutive taps within 3-second windows
- Configurable trigger count (1-10 taps)
- Visual feedback with animated pulse effect
- Activity logging for tap history

### 2. Customizable Settings
- **Ringtone Selection**: Choose from system ringtones
- **Tap Sensitivity**: Adjust timing from "Very Fast" to "Very Slow"
- **Trigger Threshold**: Set number of consecutive taps required

### 3. Modern UI/UX
- **Material3 Design**: Follows latest Material Design guidelines
- **Dynamic Colors**: Supports Android 12+ dynamic theming
- **Responsive Layout**: Adapts to different screen sizes
- **Accessibility**: Proper content descriptions and semantic structure

### 4. State Persistence
- Settings saved across app sessions
- Ringtone preferences maintained
- Tap configuration preserved

## Build Configuration

### Dependencies (libs.versions.toml)
- **Android Gradle Plugin**: 8.11.1
- **Kotlin**: 2.0.21
- **Compose BOM**: 2024.09.00
- **Navigation Compose**: 2.5.3
- **Material3**: Latest from Compose BOM

### Target Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **Java Version**: 11

### Key Libraries
- `androidx.compose.ui`: UI framework
- `androidx.compose.material3`: Material Design components
- `androidx.navigation.compose`: Navigation system
- `androidx.activity.compose`: Compose integration
- `androidx.material.icons.extended`: Extended icon set

## Permissions & Manifest

### AndroidManifest.xml
- **Main Activity**: `MainActivity` as launcher activity
- **Theme**: `Theme.Morsecall` for consistent styling
- **Backup**: Enabled with custom rules
- **RTL Support**: Enabled for internationalization

## Testing Structure

### Test Directories
- **Unit Tests**: `src/test/java/com/` - Basic unit testing
- **Instrumentation Tests**: `src/androidTest/java/com/` - Android-specific testing
- **UI Tests**: Compose testing framework integration

## Development Notes

### Code Quality
- **Logging**: Comprehensive Log.d statements for debugging
- **Error Handling**: Try-catch blocks for ringtone operations
- **State Management**: Proper Compose state handling
- **Memory Management**: Proper ringtone lifecycle management

### Performance Considerations
- **Lazy Loading**: LazyColumn for activity log
- **State Optimization**: Minimal recomposition with proper state management
- **Resource Management**: Proper ringtone cleanup on app deactivation

## Future Enhancement Opportunities

1. **Morse Code Translation**: Convert tap patterns to actual morse code
2. **Custom Patterns**: Allow users to define custom tap sequences
3. **Haptic Feedback**: Add vibration patterns for tap confirmation
4. **Background Service**: Run tap detection in background
5. **Multiple Ringtones**: Support different ringtones for different patterns
6. **Export/Import**: Settings backup and restore functionality

## File Summary

| File | Purpose | Key Functionality |
|------|---------|-------------------|
| MainActivity.kt | Main UI and navigation | Tap detection, ringtone control, activity logging |
| SettingsScreen.kt | Configuration interface | Ringtone selection, sensitivity settings, persistence |
| Constants.kt | App constants | SharedPreferences key definition |
| Theme.kt | UI theming | Material3 theme with dynamic colors |
| AndroidManifest.xml | App configuration | Activity declarations, permissions |
| build.gradle.kts | Build configuration | Dependencies, compilation settings |

This codebase represents a well-structured Android application with modern Compose UI, proper state management, and user customization features. The architecture follows Android best practices with clear separation of concerns and maintainable code structure.
