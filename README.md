# NothingWidget

NothingWidget is a native Android home-screen widget project for Nothing OS. The goal is to show the next local solar event — sunrise or sunset — based on the user's current location, with a live progress indicator that counts down toward that event.

## Concept

The widget should answer one simple question at a glance:

> What's next: sunrise or sunset, and how far away is it?

It is intended to feel native on Nothing OS: minimal, high-contrast, glanceable, and visually aligned with Nothing's dot-matrix design language.

## Current scaffold

The repo now contains an initial Android/Kotlin project scaffold:

- Gradle Kotlin DSL root project.
- Android app module under `app/`.
- Basic launcher `MainActivity`.
- Standard Android App Widget provider.
- Widget XML layout and provider metadata.
- WorkManager-based periodic widget refresh skeleton.
- Placeholder solar-event repository using fixed sunrise/sunset times.
- Basic dark rounded widget styling.

## Planned widget behavior

Example states:

- Before sunrise: show `Sunrise` and the time of the next sunrise.
- After sunrise but before sunset: show `Sunset` and the time of the next sunset.
- After sunset: show the next day's `Sunrise`.

The progress indicator should represent how far the user is between the previous event and the next one. For example, halfway between sunrise and sunset, the progress should be roughly 50% complete.

## Technical direction

Likely implementation path:

- Kotlin.
- Standard Android App Widgets first; Jetpack Glance can be evaluated later.
- Android coarse location APIs for approximate location.
- Local astronomical calculation for sunrise/sunset rather than a network API.
- WorkManager/widget scheduling for battery-friendly refreshes.
- Optional Nothing-inspired dot-matrix animation once the functional widget is stable.

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/steffencucos/nothingwidget/
│       │   ├── MainActivity.kt
│       │   ├── solar/
│       │   │   ├── SolarEvent.kt
│       │   │   └── SolarEventRepository.kt
│       │   └── widget/
│       │       ├── SolarEventWidgetProvider.kt
│       │       └── SolarEventWidgetWorker.kt
│       └── res/
│           ├── drawable/
│           ├── layout/
│           ├── mipmap-anydpi-v26/
│           ├── values/
│           └── xml/
├── build.gradle.kts
└── settings.gradle.kts
```

## Next implementation steps

1. Add runtime location permission flow.
2. Store the last known coarse location.
3. Replace fixed sunrise/sunset placeholders with real solar calculations.
4. Add widget tap/refresh behavior.
5. Improve the widget visual language with Nothing-style typography/dots.
6. Add tests for event selection and progress calculation.

## Privacy

The widget should only need approximate location. Location should be used locally where possible and should not be stored or transmitted unless a future implementation explicitly documents why.

## License

No license has been selected yet.
