# RESET — Java Android project

A minimalist breathing app implemented in Java with the Android SDK.

## Working breathing animation
The breathing orb animates smoothly through each phase:
- **Inhale:** expands over the configured inhale duration.
- **Hold:** stays expanded for the configured hold duration.
- **Exhale:** contracts over the configured exhale duration.

## Editable breathing rhythm
Use the minus and plus controls beneath each phase to set inhale, hold and exhale independently from **1 to 30 seconds**. Changes to the currently active phase take effect immediately.

## Other features
- 4 / 4 / 6 second default breathing rhythm
- Soft vibration at session start and phase transitions
- Haptics on/off
- Pause session and cycle counter
- Dark forest-green and sage visual design

## Run
1. Extract the ZIP.
2. Open this `RESET-Java` folder in Android Studio (Ladybug or newer).
3. Allow Gradle sync to finish.
4. Run on an Android device or emulator (Android 8.0 / API 26+).

Java source: `app/src/main/java/com/reset/breathe/MainActivity.java`.

To create an APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
