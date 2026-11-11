# Vinyl Player (Android / Java)

A retro-inspired local music player built with native Android Java APIs.

## Features
- Vinyl record UI with rotation while audio is playing
- Select one or multiple local audio files using Android's document picker
- Play / pause, previous / next
- Seek bar with elapsed and total time; drag to seek
- Volume slider
- Shuffle and repeat-one modes
- Foreground playback service and notification actions (previous / play-pause / next)
- Queue count and current track display

## Open and run
1. Extract the ZIP.
2. Open the `VinylPlayer` folder in Android Studio.
3. Allow Gradle sync to finish (Android Gradle Plugin 8.7.3; compile SDK 35).
4. Run on an Android device or emulator.
5. Tap **ADD MUSIC** and choose one or more audio files.

## Notes
- The app uses Android's Storage Access Framework, so it does not request broad storage access.
- Notification permission is requested on Android 13+.
- Audio playback uses Android `MediaPlayer` in a foreground service. The project is intentionally dependency-light.
- For publishing, test on your target devices and review current Android foreground-service and notification requirements.
