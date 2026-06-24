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

## Inspiration

BuidSpoofer was inspired by [klab7/PixelSpoof](https://github.com/klab7/PixelSpoof).
