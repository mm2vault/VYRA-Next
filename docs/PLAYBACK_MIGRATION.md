# Playback engine migration audit

## Scope

VYRA-Next is a separate Android application with its own package ID and original Frosted Glass UI. The existing `mm2vault/VYRA` repository is read-only for this migration and must not be changed.

The goal is to reuse the existing playback behavior where technically and legally possible, not to copy the old screens or branding.

## Verified in the upstream VYRA source

- The main playback service is `MusicService`, a Media3 `MediaLibraryService` implementation.
- The service source is very large (8,679 lines at the time of this audit) and is not a standalone player class. It directly coordinates ExoPlayer, MediaSession, notifications, audio focus, audio effects, queue persistence, crossfade, stream recovery, and other application services.
- `PlayerConnection` is coupled to the upstream Room database, metadata models, queue types, and service binder.
- Background playback is implemented through the Media3 media-library service and its foreground media-playback notification.
- Equalizer behavior uses Android audio-effect APIs (Equalizer, BassBoost, LoudnessEnhancer, Virtualizer) and an upstream controller attached to MusicService.
- YouTube audio stream resolution is present. `YoutubeiStreamRepository` delegates to `YoutubeiResolver` from the separate `rukamori/morideobfuscator` Git submodule pinned by the old VYRA repository at `47468720f91769320b2a10bb56ebce73568b07f1`. The resolver submodule is GPL-3.0 and its Android library build currently requires Java 21 and compileSdk 37, plus coroutines, OkHttp, and QuickJS. The app-side repository also depends on the old app's YouTube auth/session state (cookies, visitor data, PO tokens, locale, network gatekeeper). It is not a simple public YouTube Data API integration.
- The upstream app is GPL-3.0. Its file-level copyright and license notices must be preserved in any copied or modified upstream source. This repository includes the upstream GPL-3.0 license text.

## Important integration constraints

1. Do not copy `MusicService.kt` alone. It references many upstream packages, Hilt bindings, database entities, preference keys, queue types, stream repositories, notification providers, and utility classes.
2. Do not copy upstream API tokens, local properties, signing keys, Discord IDs, updater settings, data-server URLs, or unrelated account integrations.
3. Keep the VYRA-Next application ID (`com.mm2vault.vyra`) and its original Compose UI. The old app ID and old launcher/UI resources must not be brought across.
4. Preserve GPL-3.0 notices and include corresponding source/license information for any derivative playback components that are distributed.
5. YouTube stream extraction is subject to upstream implementation compatibility and YouTube changes. It must be tested on-device; do not describe it as guaranteed or as an official YouTube API.
6. The YouTube resolver cannot be integrated by copying `YoutubeiStreamRepository.kt` alone. Its submodule, Java 21 / Android 37 toolchain, QuickJS runtime, and authentication/session providers must be brought over or replaced by an explicitly designed compatible adapter.
7. Do not report playback as fully verified until service binding, local file playback, notification controls, background/lock-screen playback, and stream resolution have been tested.

## Migration sequence

### Phase 1 — dependency and boundary extraction
- Inventory the upstream Media3 versions, stream resolver module, service dependencies, and minimal app startup/DI requirements.
- Define a small VYRA-Next-facing playback API so Compose screens never depend directly on upstream implementation classes.
- Separate the player service and playback domain from upstream database/UI features.

### Phase 2 — minimal working playback
- [x] Add an independent Media3 playback service and session.
- [x] Connect the Compose UI through a MediaController.
- [x] Add a Storage Access Framework audio picker and pass the selected URI to Media3.
- [ ] Verify local audio playback on a physical Android device.
- [ ] Add a real multi-item queue and verify previous/next navigation.
- [ ] Evaluate and integrate YouTube resolution only after its submodule and auth/session dependencies are understood.

### Phase 3 — Android integration
- [x] Register the media playback foreground service in the manifest.
- [ ] Verify notification, headset/Bluetooth controls, audio focus, process recreation, and background playback.
- [ ] Add equalizer controls only after the player session and audio session lifecycle are verified.

### Phase 4 — UI connection and release
- [x] Connect the Frosted Glass mini-player to MediaController playback state and metadata.
- [ ] Complete physical-device testing and update the test notes.
- [ ] Produce a release APK only after signing is configured securely.

## Current status

- The audit and upstream GPL-3.0 license have been committed.
- VYRA-Next has an independent Media3 `PlaybackService` and matching service manifest entry.
- The Compose mini-player is connected to a `MediaController`, reflects the service playing state, displays current media metadata, and sends playback/navigation commands.
- The local audio picker uses Android's document picker and loads the selected content URI into Media3. Persistable read permission is requested so the selected URI can remain accessible after process restarts where supported by the document provider.
- GitHub Actions run #20 successfully built and uploaded a debug APK before the local-audio picker change. The latest commit's CI result must be checked separately.
- No upstream YouTube resolver or legacy MusicService source has been copied into VYRA-Next yet.
- The old VYRA repository has not been modified.
