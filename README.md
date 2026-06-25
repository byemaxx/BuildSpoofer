# BuidSpoofer

BuidSpoofer is a modern LSPosed module for applying device identity templates to selected Android apps.

It can override common `Build` and system property values, advertise Pixel-specific system features, and manage reusable built-in or custom templates. The included profile currently targets the Pixel 10 Pro XL (`mustang`).

## Requirements

- Android 10 or newer
- An Xposed framework supporting modern libxposed API 101+

## Installation

1. Install the APK.
2. Enable **BuidSpoofer** in the Xposed manager.
3. Add the target applications to the module scope.
4. Select a template in BuidSpoofer.
5. Fully stop and restart the target applications.

## Spoofing Modes

- **Full Pixel Spoof**: Modifies almost all hardware and build properties, and enables Pixel-exclusive features (like Google Camera features, Next Generation Assistant, etc.). Best used for unlocking features in specific target applications.
- **Pixel OTA Minimal Spoof**: Only changes a few specific build strings (`FINGERPRINT`, `DESCRIPTION`, `ID`, `DISPLAY`, `INCREMENTAL`, `SECURITY_PATCH`). This is strictly intended for bypassing Google Play Services OTA update checks. When using this mode, limit your LSPosed scope to **only** `com.google.android.gms` and (optionally) `com.android.settings`. Do not include other apps.

## Technical Notes & Limitations

- **Native Properties**: The module currently hooks Java-level `android.os.Build` fields and `android.os.SystemProperties.get()` queries. It does **not** guarantee native property spoofing through C/C++ `__system_property_get()`. Apps heavily relying on native checks might still see your real device fingerprint.
- **SDK_INT Warning**: Do **not** spoof `SDK_INT` (Android API level) unless you specifically know why you need it. Changing it can break apps that use it to conditionally call newer/older Android APIs, leading to crashes.

## Inspiration

BuidSpoofer was inspired by [klab7/PixelSpoof](https://github.com/klab7/PixelSpoof).
