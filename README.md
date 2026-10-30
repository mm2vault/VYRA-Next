# VYRA Next

A fresh Android music player project with an original Frosted Glass interface and an independent Media3 playback layer.

## Project status

- Original Jetpack Compose interface and separate app identity: in place.
- Media3 playback service and Compose `MediaController` connection: implemented.
- Local audio file picker: implemented with Android's Storage Access Framework (`OpenDocument`); selected URI read permission is persisted when the provider supports it.
- Mini-player title/artist and play/pause/previous/next actions are connected to the active MediaController.
- Automated Android debug build: verified successfully on GitHub Actions before the local-audio picker change. The picker change is being rebuilt by CI now.
- Device playback, lock-screen/notification controls, headset/Bluetooth behavior, and process-restart behavior still require physical-device testing.
- YouTube stream resolution and equalizer parity are not yet integrated or verified.

## Requirements

- JDK 17
- Gradle 8.13+
- Android SDK 36

## Build

Open the project in Android Studio and sync Gradle. GitHub Actions runs a debug build on pushes and pull requests. The debug APK is uploaded as the `VYRA-Next-debug` workflow artifact.

## First playback test

1. Install the debug APK on an Android device.
2. Open VYRA and tap **Open audio**.
3. Choose an audio file from device storage.
4. Confirm the selected title appears in the mini-player and audio starts.
5. Test pause/resume and, after adding more than one queued item, previous/next controls.

The current picker loads one selected local file at a time; a browsable library and queue are future work.

## Licensing

The original VYRA Next UI and app scaffolding are developed for this repository. The upstream playback project is GPL-3.0; the upstream license text is included in `LICENSE`. Any copied or modified upstream playback source must retain its original copyright/license headers, and distributed derivative source must comply with GPL-3.0.


## Signed APK and in-app updates

- The app's **Update** action checks the latest public GitHub Release and looks for a direct `.apk` asset.
- When a newer release exists, VYRA downloads the APK with Android DownloadManager and opens Android's package installer. Android still requires the user to approve installation.
- Release APKs must always use the same package ID (`com.mm2vault.vyra`) and the same private signing key. Never regenerate or replace that key after publishing the first signed release.
- The signed release workflow expects these GitHub Actions repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- A debug APK is signed with Android's debug key and **cannot be updated in-place to the release-signed app**. For the first move from debug to release, uninstall the debug build once, then install the signed Release APK. Later signed releases update over the existing signed app.
- The GitHub Release contains a direct `app-release.apk` asset, not a ZIP containing the APK.
