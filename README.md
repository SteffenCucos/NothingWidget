# NothingWidget

[![Android CI](https://github.com/SteffenCucos/NothingWidget/actions/workflows/android.yml/badge.svg)](https://github.com/SteffenCucos/NothingWidget/actions/workflows/android.yml)

NothingWidget is a native Android home-screen widget project for Nothing OS. It shows the next local solar event — sunrise or sunset — based on the user's saved device location, with a progress indicator that advances toward that event.

## Concept

The widget answers one simple question at a glance:

> What's next: sunrise or sunset, and how far away is it?

The visual direction is minimal, high-contrast, glanceable, and loosely aligned with Nothing's monochrome/dot-matrix design language.

## Current status

The app now has a functional first implementation:

- Android/Kotlin app module.
- Launcher `MainActivity` with runtime location permission flow.
- Last-known device location retrieval using Google Play Services Location.
- Local storage of the latest latitude/longitude.
- Offline sunrise/sunset calculation using a NOAA-style solar event algorithm.
- Standard Android App Widget provider.
- Jetpack Glance widget provider.
- WorkManager-based periodic widget refresh.
- Tap-to-open-widget behavior.
- Nothing-style compact `1x1` layout with label, dot-matrix time, mini solar arc, and remaining time.
- Nothing-style wide `1x2` layout with left-side event details and right-side solar arc.
- GitHub Actions workflow that runs unit tests and assembles the debug APK on push/PR.

## Behavior

Example states:

- Before sunrise: show `Sunrise` and the time of the next sunrise.
- After sunrise but before sunset: show `Sunset` and the time of the next sunset.
- After sunset: show the next day's `Sunrise`.

The progress indicator represents how far the user is between the previous solar event and the next one. For example, halfway between sunrise and sunset, the progress should be roughly 50% complete.

When no location has been saved yet, the widget prompts the user to open the app and enable location.

## Widget formats

- `1x1`: compact square format for a quick glance. It prioritizes event type, dot-matrix time, and a miniature phase/progress arc.
- `1x2`: one-row wide format. It adds `NEXT`, remaining time, and a larger arc while staying short enough for a single launcher row.

Both the RemoteViews and Glance widget implementations advertise a `1x1` target size and resize horizontally into the wide `1x2` presentation.

## Project structure

```text
.
├── .github/workflows/android.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/steffencucos/nothingwidget/
│       │   ├── MainActivity.kt
│       │   ├── location/
│       │   │   ├── DeviceLocationProvider.kt
│       │   │   └── LocationStore.kt
│       │   ├── solar/
│       │   │   ├── SolarCalculator.kt
│       │   │   ├── SolarEvent.kt
│       │   │   └── SolarEventRepository.kt
│       │   └── widget/
│       │       ├── SolarEventWidgetProvider.kt
│       │       └── SolarEventWidgetWorker.kt
│       └── res/
│           ├── drawable/
│           ├── layout/
│           ├── values/
│           └── xml/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Build

Open the repository in Android Studio and run the `app` configuration, or assemble from the command line with Gradle installed:

```bash
gradle :app:assembleDebug
```

Run unit tests with:

```bash
gradle :app:testDebugUnitTest
```

A Gradle wrapper has not been committed yet.

## CI

GitHub Actions runs unit tests and assembles the debug APK on pushes to `main`, pull requests, and manual dispatch.

## Next implementation steps

1. Add a Gradle wrapper.
2. Add unit tests for event selection, edge cases, and progress calculation.
3. Add a widget configuration screen for manual/fallback location.
4. Add launcher preview assets for each widget size.
5. Tune spacing against real Nothing Launcher cell metrics after installing on-device.

## Privacy

The app uses location locally to calculate sunrise/sunset times. It stores the last known latitude/longitude in private app preferences and does not transmit location to a network API.

## License

No license has been selected yet.
