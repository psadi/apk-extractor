# APK Extractor 📦

A modern, production-grade native Android application for exploring installed apps and extracting base APKs with a smooth Material You (Material 3) user interface, live search, alphabetical fast-scrolling, and Storage Access Framework (SAF) integration.

---

## Features

- **Dynamic Material You Theming:** Native Monet dynamic color theming on Android 12+ (API 31–35) with fallback to an elegant Material 3 indigo/purple palette on earlier releases.
- **Dual Category Filtering:** Instant toggling between User Installed apps and System Apps with real-time app counts.
- **Real-Time Search & Fast-Scroller:**
  - Responsive search bar filtering by app label and package name.
  - Interactive vertical A–Z sidebar with touch/drag tracking, haptic feedback, and floating letter indicator.
- **Instant APK Extraction:**
  - One-tap APK extraction from `ApplicationInfo.publicSourceDir` directly to storage with smooth progress feedback.
  - Default extraction path: `Downloads/APK_Extractor/` via MediaStore API (no dangerous storage permissions required).
  - Custom directory picker via Storage Access Framework (`Intent.ACTION_OPEN_DOCUMENT_TREE`) with persistable URI permissions.
- **Action Bottom Sheet:**
  - Extract APK to storage.
  - Direct Share via standard Android share sheet (`content://` URI via `FileProvider`).
  - Open System App Settings for the target package.
- **Actionable Completion Feedback:**
  - Slide-in feedback banner offering direct "Share" and "Open Folder" actions.

---

## Tech Stack & Architecture

- **Language:** Kotlin 2.0+ (Coroutines + Flow)
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** Single Activity, MVVM (Model-View-ViewModel) with Unidirectional Data Flow
- **Data & Storage:** Storage Access Framework (SAF) DocumentFile, MediaStore API, Jetpack DataStore Preferences
- **SDK Targets:** `minSdk = 26` (Android 8.0) • `targetSdk = 35` (Android 15) • `compileSdk = 35`

---

## Getting Started & Build Instructions

This project is configured with [mise](https://mise.jdx.dev) via `.mise.toml` for hermetic and reproducible developer tooling.

### Prerequisites

Install `mise` on your development machine (Linux, macOS, WSL):
```bash
curl https://mise.run | sh
```

### 1. Provision Java and Gradle via Mise

From the project root:
```bash
mise install
```
This automatically sets up OpenJDK 17 and Gradle 8.7 as configured in `.mise.toml`.

### 2. Build Debug APK

Execute the build task using `mise` or direct `./gradlew`:
```bash
# Using mise:
mise exec -- ./gradlew assembleDebug

# Or directly via the Gradle wrapper:
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install to Connected Device / Emulator

Ensure your device has USB debugging enabled:
```bash
# Verify device connection
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Continuous Integration & Automated Releases

Automated builds and releases are managed via GitHub Actions ([`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml)):

- **Push & Pull Requests to `main`:**
  - Automatically compiles `assembleDebug` and `assembleRelease`.
  - Publishes the compiled APKs as downloadable workflow run artifacts (`apk-extractor-builds`).
- **Tag Pushes (`v*`):**
  - Triggers automated release publishing to **GitHub Releases**.
  - Attaches both `APK-Extractor-release.apk` and `APK-Extractor-debug.apk` directly to the release entry.
  - Automatically generates changelog notes from commit history.

To trigger a new release:
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

---

## Google Play Store Policy Compliance: `QUERY_ALL_PACKAGES`

This app declares the `android.permission.QUERY_ALL_PACKAGES` permission in `AndroidManifest.xml` to allow users to view, back up, and extract APKs for all installed applications on Android 11+ (API 30+).

### Google Play Submission Guidance
- **Permitted Core Use Case:** Google Play policy permits `QUERY_ALL_PACKAGES` for apps that require broad package visibility to provide core functionality, specifically categorized under **Device Search, Antivirus, File Management, or Backup & Restore tools**.
- **Declaration in Play Console:**
  1. In the Google Play Console, go to **Policy and Programs > App Content > Sensitive permissions and APIs**.
  2. Select **QUERY_ALL_PACKAGES**.
  3. Under justification, indicate that the app's primary core functionality is an APK backup, inspection, and extraction utility that requires discovering all installed packages on the device to fulfill user-requested backup tasks.

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
