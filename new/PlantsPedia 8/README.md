# Plants Pedia (Java / Android Studio)

A native Android plant encyclopedia written in **Java** (no Kotlin source). It searches Wikipedia's public REST API and displays article summaries, descriptions and available thumbnail photographs. Selecting a result opens a detail view with a larger available image, summary and a link to the full Wikipedia article.

## Requirements
- Android Studio with Android SDK Platform 35
- JDK 17
- Gradle 8.9 (Android Gradle Plugin 8.7.3)
- Internet connection on the device

## Open and build
1. Extract this ZIP.
2. Open the `PlantsPedia` folder in Android Studio.
3. Allow Gradle to sync. If prompted, use Gradle 8.9 and JDK 17.
4. Connect an Android device or start an emulator.
5. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The resulting debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

## Features
- Plant-only search results: validates article descriptions and excerpts for explicit botanical terms, and rejects common non-botanical uses such as power plants and industrial plants.
- Displays available article thumbnails, descriptions and excerpts.
- Plant detail page with summary and image when available.
- Opens the full Wikipedia article in the browser.
- Uses Java and Android framework widgets; no third-party UI libraries required.

## Data and image licensing
This app retrieves content at runtime from the English Wikipedia REST API. It does not bundle the entire Wikipedia plant catalogue or download all plant photographs into the APK. Wikipedia content is continuously updated; the available search results depend on the query and Wikipedia's coverage. Article text is generally licensed under CC BY-SA; individual images can have different licenses. The app includes an attribution notice and links users to the original article. Before redistributing an app commercially, review the relevant Wikimedia reuse and attribution requirements and each image's license.

## Gradle compatibility
The project uses Android Gradle Plugin 8.7.3 with Gradle 8.9. This pairing avoids using Gradle 10 and is suitable for the project's Android plugin version. The project files use Groovy DSL (`build.gradle`, `settings.gradle`), not Kotlin DSL.
