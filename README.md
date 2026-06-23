# NothingWidget

NothingWidget is an Android home-screen widget project for Nothing OS. The goal is to show the next local solar event — sunrise or sunset — based on the user's current location, with a live progress animation that counts down toward that event.

## Concept

The widget should answer one simple question at a glance:

> What's next: sunrise or sunset, and how far away is it?

It is intended to feel native on Nothing OS: minimal, high-contrast, glanceable, and visually aligned with Nothing's dot-matrix design language.

## Planned features

- Detect the user's approximate location.
- Calculate today's sunrise and sunset times for that location.
- Display whichever event is next.
- Show a progress animation from the previous solar event to the next one.
- Update at a reasonable interval instead of constantly polling.
- Support light and dark widget appearances.
- Keep battery usage low.

## Widget behavior

Example states:

- Before sunrise: show `Sunrise` and the time of the next sunrise.
- After sunrise but before sunset: show `Sunset` and the time of the next sunset.
- After sunset: show the next day's `Sunrise`.

The progress indicator should represent how far the user is between the previous event and the next one. For example, halfway between sunrise and sunset, the animation should be roughly 50% complete.

## Technical direction

This repo is intended to become a native Android project. A likely implementation path:

- Kotlin
- Jetpack Glance or standard Android App Widgets
- Location APIs for coarse location
- A sunrise/sunset calculation library or local astronomical calculation
- WorkManager or widget update scheduling for periodic refreshes

## Privacy

The widget should only need approximate location. Location should be used locally where possible and should not be stored or transmitted unless a future implementation explicitly documents why.

## Project status

Initial project scaffold. Implementation is still in progress.

## License

No license has been selected yet.
