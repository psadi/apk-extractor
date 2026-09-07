# Privacy Policy for APK Extractor

**Last updated:** September 8, 2026

APK Extractor ("the App") is a free and open-source utility developed to assist users in inspecting installed applications and extracting/backing up base APK files on their Android devices.

We value your privacy. This privacy policy explains how our application handles user information.

---

## 1. Information Collection and Use

- **No Personal Data Collected:** APK Extractor does **not** collect, store, transmit, or share any personal identifiable information (PII), device identifiers, contact lists, location data, or network activity.
- **Local On-Device Processing:** All application scanning, icon rendering, package querying, and APK extraction processes occur **strictly locally on your device**.
- **No External Servers or Analytics:** The App does not contain third-party advertising SDKs, tracking beacons, or telemetry services. No data is sent to external servers or cloud services.

---

## 2. Permissions & Justification

APK Extractor requests only the minimum permissions necessary to deliver its core APK backup functionality:

- `android.permission.QUERY_ALL_PACKAGES`:
  - **Purpose:** Allows the App to discover installed applications on Android 11+ (API level 30 and higher) so that users can select, view details for, and extract APKs.
  - **Usage:** This information is queried dynamically on your device solely to display your list of installed apps. It is never logged or transmitted.
- **Storage Access Framework (SAF) / MediaStore:**
  - **Purpose:** Allows you to save extracted `.apk` files directly to your device's `Downloads/APK_Extractor/` folder or a user-selected custom directory.
  - **Usage:** The App only accesses the specific folder or destination authorized by you.

---

## 3. Third-Party Services & Links

The App contains optional links to external websites (such as the GitHub repository and developer sponsorship page). If you click these links, you will be directed to external sites governed by their respective privacy policies.

---

## 4. Children's Privacy

APK Extractor does not address anyone under the age of 13, and does not knowingly collect personally identifiable information from children.

---

## 5. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted to this page with an updated revision date.

---

## 6. Open Source & Contact

APK Extractor is open source and licensed under the Apache License 2.0. You can review the source code on GitHub:
- **Repository:** https://github.com/psadi/apk-extractor
- **Issues & Inquiries:** https://github.com/psadi/apk-extractor/issues
