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
- YouTube audio stream resolution is present. `YoutubeiStreamRepository` delegates to `YoutubeiResolver` from the upstream `morideobfuscator` module and consumes upstream YouTube login/network/auth state. It is not a simple public YouTube Data API integration.
- The upstream app is GPL-3.0. Its file-level copyright and license notices must be preserved in any copied or modified upstream source. This repository includes the upstream GPL-3.0 license text.

## Important integration constraints

1. Do not copy `MusicService.kt` alone. It references many upstream packages, Hilt bindings, database entities, preference keys, queue types, stream repositories, notification providers, and utility classes.
2. Do not copy upstream API tokens, local properties, signing keys, Discord IDs, updater settings, data-server URLs, or unrelated account integrations.
3. Keep the VYRA-Next application ID (`com.mm2vault.vyra`) and its original Compose UI. The old app ID and old launcher/UI resources must not be brought across.
4. Preserve GPL-3.0 notices and include corresponding source/license information for any derivative playback components that are distributed.
5. YouTube stream extraction is subject to upstream implementation compatibility and YouTube changes. It must be tested on-device; do not describe it as guaranteed or as an official YouTube API.
6. Do not report playback as working until service binding, queue playback, notification controls, background/lock-screen playback, and stream resolution have been tested.

## Migration sequence

### Phase 1 — dependency and boundary extraction
- Inventory the upstream Media3 versions, stream resolver module, service dependencies, and minimal app startup/DI requirements.
- Define a small VYRA-Next-facing playback API so Compose screens never depend directly on upstream implementation classes.
- Separate the player service and playback domain from upstream database/UI features.

### Phase 2 — minimal working playback
- Bring over only the necessary GPL-3.0 playback and stream-resolution sources, with their original notices.
- Adapt package names only where required; retain copyright notices.
- Add the matching Media3 and networking dependencies.
- Implement a VYRA-Next playback controller/repository adapter.
- Verify local audio first, then YouTube stream resolution, then queue controls.

### Phase 3 — Android integration
- Register the media playback foreground service and media-session actions in the new manifest.
- Verify notification, headset/Bluetooth controls, audio focus, process recreation, and background playback.
- Add equalizer controls only after the player session and audio session lifecycle are verified.

### Phase 4 — UI connection and release
- Connect the existing original Frosted Glass mini-player/full player to the playback API.
- Add automated unit/build checks and device test notes.
- Produce a debug APK only after CI build succeeds; produce a release APK only after signing is configured securely.

## Current status

- The audit and upstream GPL-3.0 license have been committed.
- VYRA-Next now has an independent Media3 `PlaybackService` shell and matching service manifest entry.
- The current mini-player is still a visual prototype; it is not yet connected to a MediaController or a real track.
- No upstream playback source has been copied into VYRA-Next yet.
- The old VYRA repository has not been modified.
