# Role & Project Scope
You are an expert Android engineer. Generate a native, production-grade Android APK Extractor app named "APK Extractor" with modern architecture, fluid animations, and Material You dynamic theming. The project must be initialized directly inside the current repository root with full mise-en-place tooling configuration.

---

## 1. Environment & Tooling Setup (.mise.toml)
Create a `.mise.toml` at the project root configured for Android builds:
- `java = "openjdk-17"`
- `gradle = "8.7"`

Generate a standard `gradlew` wrapper via `gradle wrapper --gradle-version 8.7` so builds can be executed both via `mise exec -- ./gradlew <task>` or direct `./gradlew`.

Provide clear instructions in `README.md` on:
1. Running `mise install` to provision Java and Gradle.
2. Building debug APK via `mise exec -- ./gradlew assembleDebug`.
3. Installing to a connected device or emulator via adb.

---

## 2. Technical Stack & Architecture
- **Language:** Kotlin (modern coroutines + Flow)
- **UI Framework:** Jetpack Compose with Material 3 (Dynamic Color / Monet engine support)
- **Architecture:** Single Activity, MVVM with clean separation:
  - `data/`: App extraction service, Package manager queries, SAF/MediaStore file storage repositories, DataStore preferences.
    - `ui/`: Compose UI, ViewModels, Themes, Navigation.
    - **SDK Targets:**
      - `minSdk = 26` (Android 8.0)
        - `targetSdk = 35`
          - `compileSdk = 35`
          - **Permissions:**
            - Declare `android.permission.QUERY_ALL_PACKAGES` in `AndroidManifest.xml` (needed for complete package discovery; document Play Store policy compliance guidance in README).
              - Storage handling via Storage Access Framework (SAF) DocumentFile / MediaStore API so no legacy dangerous storage permissions are required on modern Android.

              ---

              ## 3. Core Features & UX Requirements

              ### A. App List Screen
              - **Dual Category Filtering:** Segmented tabs or pill filters at the top to toggle between "User Installed" and "System Apps" (default to User Installed).
              - **Search & Jump:**
                - Top persistent search bar filtering apps by display name and package name in real-time.
                  - **Alphabetical Fast-Scroller:** An interactive A–Z sidebar with drag-to-scroll thumb feedback that quickly jumps the `LazyColumn` to matching app prefixes.
                  - **App List Item Card:**
                    - Clean Material 3 elevated card displaying: App Icon, App Label, Package Name, Version Name (`vX.X`), and APK size.
                      - Tapping an item: Triggers extraction immediately with smooth loading feedback.
                        - Long-press / Overflow action button (3 dots): Opens a bottom sheet with actions:
                            - **Extract APK**
                                - **Direct Share** (via standard Android share sheet)
                                    - **App Info** (opens Android System App Settings for that package)

                                    ### B. APK Extraction & File Storage
                                    - **Extraction Logic:** Locate `ApplicationInfo.publicSourceDir` (or `sourceDir`), copy the base `.apk` stream to the designated target directory using background coroutines (`Dispatchers.IO`).
                                    - **Default Location:** `Downloads/APK_Extractor/`.
                                    - **Naming Pattern:** `${appName}_v${versionName}.apk` (sanitizing illegal filesystem characters).
                                    - **Custom Location Picker:**
                                      - Setting screen/dialog with a SAF folder picker (`Intent.ACTION_OPEN_DOCUMENT_TREE`) allowing users to persist a custom output folder URI via `takePersistableUriPermission`.
                                      - **Extraction Feedback:**
                                        - Show a snackbar on completion: *"Extracted [AppName] to [Folder]"* with actionable buttons:
                                            - **Share:** Launches system share sheet with `FileProvider` URI.
                                                - **Open Folder:** Sends an intent to view the target folder in the system file manager.

                                                ### C. Visual Design & Theming
                                                - Fully responsive light and dark themes with dynamic theming enabled (`dynamicLightColorScheme` / `dynamicDarkColorScheme` on Android 12+), gracefully falling back to a custom Material 3 purple/indigo palette on older OS versions.
                                                - Polished micro-interactions, smooth elevation changes, and edge-to-edge layout support.

                                                ---

                                                ## 4. Deliverables
                                                1. Complete `.mise.toml` configuration.
                                                2. Root and module-level `build.gradle.kts` files using Kotlin DSL and version catalog (`libs.versions.toml`).
                                                3. Complete `AndroidManifest.xml` including `FileProvider` configuration for secure APK sharing.
                                                4. Fully implemented Kotlin source code across domain, data, and presentation layers.
                                                5. Well-documented `README.md` with build and setup steps.

