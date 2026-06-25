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
- **Custom Templates**: You can create custom templates and manually fill in only the specific fields you want to spoof (e.g., for minimal OTA spoofing). Properties left blank will fall back to your device's original values.
  - Standard fields such as `FINGERPRINT`, `MODEL`, `DEVICE`, `INCREMENTAL`, and `SECURITY_PATCH` are alias-style fields. They may affect multiple related system property keys, such as `ro.build.fingerprint` and partition-specific `*.build.fingerprint` keys.
  - Exact custom properties such as `ro.build.fingerprint` only affect that exact `SystemProperties` key.
  - For minimal spoofing, users should prefer exact `ro.*` custom properties or leave unrelated fields blank.

## Technical Notes & Limitations

- **Native Properties**: The module currently hooks Java-level `android.os.Build` fields and `android.os.SystemProperties.get()` queries. It does **not** guarantee native property spoofing through C/C++ `__system_property_get()`. Apps heavily relying on native checks might still see your real device fingerprint.
- **SDK_INT Warning**: Do **not** spoof `SDK_INT` (Android API level) unless you specifically know why you need it. Changing it can break apps that use it to conditionally call newer/older Android APIs, leading to crashes. The built-in full Pixel template includes `SDK_INT` spoofing and is intended for apps that need full Pixel-style spoofing. For safer or OTA-related use, users should create a custom template and leave `SDK_INT` blank.

## Inspiration

BuidSpoofer was inspired by [klab7/PixelSpoof](https://github.com/klab7/PixelSpoof).
