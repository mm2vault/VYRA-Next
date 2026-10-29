# VYRA Next

A fresh Android music player project with an original Frosted Glass interface and an independent playback layer.

## Project status

- Original Jetpack Compose interface and separate app identity: in place.
- Media3 playback service shell: added; UI controller integration and real-track playback verification are still pending.
- Legacy VYRA engine: audited; its service is deeply coupled to the original database, dependency injection, queue models, YouTube resolver, and application features. It will be migrated in small verified slices rather than copied as a single file.
- YouTube stream resolution, background playback controls, and equalizer parity: not yet verified in VYRA Next.

## Requirements

- JDK 17
- Gradle 8.13+
- Android SDK 36

## Build

Open the project in Android Studio and sync Gradle. GitHub Actions runs a debug build on pushes and pull requests.

## Licensing

The original VYRA Next UI and app scaffolding are developed for this repository. The upstream playback project is GPL-3.0; the upstream license text is included in `LICENSE`. Any copied or modified upstream playback source must retain its original copyright/license headers, and distributed derivative source must comply with GPL-3.0.
