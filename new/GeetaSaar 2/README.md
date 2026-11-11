# Geeta Saar — Android app (Java)

A beautiful Bhagavad Gita reader built in Java.

## Complete scripture edition
- All 18 chapters and the complete verse collection (700 numbered shlokas in the selected source edition).
- Sanskrit Devanagari original and IAST transliteration for each verse.
- English translation for each verse.
- Chapter-by-chapter reading, daily verse, saved verses and reflections.
- Complete Sanskrit and English JSON datasets are downloaded on first launch and cached privately on the device. Internet is needed once to fetch the complete content; the cached text is then available offline.
- If first-launch download fails, reconnect and relaunch the app.

## Design and functionality
- Midnight indigo, parchment and saffron aesthetic.
- Home, Chapters and Saved tabs.
- Bookmark/unbookmark individual verses.
- All verse cards show chapter/verse references, Sanskrit, transliteration and English meaning.

## Build
1. Extract the ZIP.
2. Open the `GeetaSaar` folder in Android Studio.
3. Use JDK 17 and Android SDK Platform 35.
4. Sync Gradle and run on an Android device/emulator.

## Content attribution
The complete Sanskrit and English verse datasets are sourced from the AdiyogiArts `gita-json` public GitHub repository:
https://github.com/AdiyogiArts/gita-json
The repository describes a 700-verse dataset with Sanskrit, transliteration and English translations. Please consult a trusted printed edition for scholarly or liturgical use; verse numbering and readings can vary slightly between editions.

This app is an educational reader, not an official religious publication.
