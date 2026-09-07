# Google Play Store Publication Guide: APK Extractor 🚀

This comprehensive guide provides everything required to publish **APK Extractor** to the Google Play Store, including store metadata, policy compliance declarations, signing, and step-by-step console instructions.

---

## 📋 Quick Pre-requisites Checklist

1. **Google Play Developer Account:** Active account ($25 one-time registration at [play.google.com/console](https://play.google.com/console)).
2. **Android App Bundle (`.aab`):** Generated file located at:
   ```
   app/build/outputs/bundle/release/app-release.aab
   ```
   *(Also automatically built and attached to every GitHub Release via our CI/CD workflow)*.
3. **Public Privacy Policy URL:**
   ```
   https://github.com/psadi/apk-extractor/blob/main/PRIVACY_POLICY.md
   ```
4. **App Graphics:**
   - App Icon: 512 × 512 PNG (32-bit with alpha, up to 1 MB)
   - Feature Graphic: 1024 × 500 JPG or PNG (up to 15 MB)
   - Phone Screenshots: Minimum 2 screenshots (16:9 or 9:16 aspect ratio, min 1080px)

---

## 🏷️ Store Listing Metadata (Copy & Paste)

### App Name (max 30 chars)
```text
APK Extractor - Backup & Share
```

### Short Description (max 80 chars)
```text
Fast, modern APK extractor with Material You design and instant backup.
```

### Full Description (max 4000 chars)
```text
APK Extractor is a clean, lightweight, and modern utility designed to inspect installed applications and effortlessly extract, back up, and share APK files on your Android device.

Built entirely with Kotlin and Jetpack Compose, APK Extractor delivers a fluid Material 3 (Material You) experience with dynamic theming that adapts to your device's wallpaper.

🌟 Key Features:

• Instant Extraction: Extract base APKs from any installed app directly to your device storage with a single tap.
• Dual Categorization: Easily toggle between User-installed apps and System apps with real-time app counts.
• Real-Time Search & Fast-Scroller: Quickly locate applications with instant letter-by-letter search and an interactive A–Z alphabetical fast-scroller.
• Storage Flexibility: Supports standard MediaStore downloads folder (Downloads/APK_Extractor/) as well as custom storage directories via Android's Storage Access Framework (SAF).
• Quick Actions Bottom Sheet:
  - Extract APK to storage
  - Share APK directly through your preferred messaging or cloud apps
  - View package details and jump directly to System App Settings
• Dark & Light Modes: Fully responsive edge-to-edge Material You theming with dynamic color palette on Android 12+.
• Privacy Focused: No ads, no analytics, no background tracking, and no external server communication. Everything is processed 100% locally on your device.
• 100% Open Source: Licensed under Apache License 2.0.

Permissions:
• QUERY_ALL_PACKAGES: Used strictly on-device to discover installed applications for the user to view and extract.
• Storage Access: Uses modern Scoped Storage and Storage Access Framework to securely save extracted APKs without broad file system access.
```

---

## 🔒 Google Play Policy Declaration: `QUERY_ALL_PACKAGES`

Google Play strictly regulates broad package visibility. Use the exact answers below when filling out the **Sensitive permissions and APIs > QUERY_ALL_PACKAGES** declaration in the Play Console:

### 1. Core Purpose Question
- **Category:** `Backup and Restore` / `File Management & Device Search`

### 2. Justification Statement (Copy & Paste into the text box):
```text
APK Extractor is a dedicated APK backup, inspection, and extraction utility. The application's core functionality requires identifying and enumerating all installed applications on the device so that the user can select, inspect package metadata, back up, and extract base APK packages to device storage or share them. Without broad package visibility (QUERY_ALL_PACKAGES), the app is unable to fulfill its primary advertised purpose of browsing and extracting installed apps. All queried package information is processed strictly locally in-memory on the device and is never stored, collected, or transmitted to any external server.
```

### 3. Demonstration Video Link
- Record a brief (30–60 second) screen video showing:
  1. Opening the app and viewing the list of installed applications.
  2. Searching for an app or switching to system apps.
  3. Tapping an app to extract the APK to storage.
- Upload this video as an unlisted YouTube video or public Google Drive link and paste the URL into the field requested by Google Play.

---

## 🛡️ Data Safety Section Questionnaire Answers

When completing the **Data Safety** questionnaire in Google Play Console:

1. **Does your app collect or share any user data?**
   - Answer: **No** (The app operates strictly on-device and does not collect or transmit user data).
2. **Does your app use encryption in transit?**
   - Answer: **N/A** (No data is transmitted over the internet).
3. **Can users request their data to be deleted?**
   - Answer: **Yes** (No user data is stored remotely; deleting the app removes all local data).

---

## 🔑 Generating a Production Upload Keystore

Google Play uses **Play App Signing**. You sign your App Bundle with an **upload key**, and Google re-signs it with the master distribution key.

To generate your production upload key on your machine or inside Termux:

```bash
keytool -genkeypair \
  -v \
  -keystore ~/apk-extractor-upload.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload \
  -storepass "YOUR_SECURE_PASSWORD" \
  -keypass "YOUR_SECURE_PASSWORD" \
  -dname "CN=Adithya PS, OU=Mobile, O=Individual, L=Bangalore, ST=KA, C=IN"
```

### Configuring Automated Signing via Gradle (Optional):
In `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "upload.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```
*(If you sign locally or use Google Play's internal app signing prompt, you can upload the bundle directly).*

---

## 🚀 Step-by-Step Play Console Submission Walkthrough

### Step 1: Create the Application
1. Log into [Google Play Console](https://play.google.com/console).
2. Click **Create app**.
3. Fill in:
   - **App name:** `APK Extractor - Backup & Share`
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
4. Accept declarations and click **Create app**.

### Step 2: Complete Dashboard Tasks (Policy & Content)
Go down the "Set up your app" dashboard checklist:
- **Privacy Policy:** Enter `https://github.com/psadi/apk-extractor/blob/main/PRIVACY_POLICY.md`
- **App access:** All functionality is available without special access.
- **Ads:** No, my app does not contain ads.
- **Content ratings:** Start questionnaire > Category: Utility/Tools > Answer "No" to violence, sexual content, offensive language, etc.
- **Target audience:** 18 and over (avoids stricter children's policy requirements).
- **News app:** No.
- **COVID-19 contact tracing:** No.
- **Data safety:** Complete as specified in the Data Safety section above.
- **Government apps:** No.
- **Sensitive permissions (`QUERY_ALL_PACKAGES`):** Paste justification and video link as specified above.

### Step 3: Main Store Listing
1. Go to **Grow > Store presence > Main store listing**.
2. Paste the **Short description** and **Full description** from above.
3. Upload:
   - **App icon** (512x512 PNG)
   - **Feature graphic** (1024x500 JPG/PNG)
   - **Screenshots** (At least 2 phone screenshots showing the app in action)
4. Click **Save**.

### Step 4: Create and Roll Out Release
1. In the left navigation, choose **Release > Production** (or **Testing > Internal testing** if you want to test first).
2. Click **Create new release**.
3. In the **App bundles** card, drag and drop `app-release.aab`.
4. Name the release: `1.0.0 (1)`.
5. Enter release notes:
   ```text
   Initial release of APK Extractor featuring Material You dynamic theming, live search, fast-scrolling, SAF integration, and one-tap APK backups.
   ```
6. Click **Next**, review summary, and click **Save & Start rollout to Production**!
