<div align="center">

# XVOX

### A focused, offline-first music player for Android

[![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/13)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Current release](https://img.shields.io/badge/Release-v1.01.01-black?style=flat-square)](../../releases/tag/v1.01.01)

**XVOX v1.01.01** · Local music · Personal playback · No account required

</div>

---

## What XVOX is

XVOX is a polished local-music experience built for people who want their own library, responsive playback controls, immersive artwork, and extensive appearance controls without depending on a streaming catalogue.

The app is designed for **Android 13 and newer** and reads music stored on the device after the user grants media access.

## Highlights

- **Local-first library** — Browse and search music stored on the device.
- **Reliable background playback** — Media3 playback service, media-session controls, notification controls, and headset-friendly transport actions.
- **Now Playing experience** — Artwork-led presentation, queue navigation, lyrics, playback controls, and configurable layout behavior.
- **Mini Player** — Keep playback reachable while moving through the library and settings.
- **Queue, likes, playlists, and recents** — Manage what plays next and return to favourite music quickly.
- **Artwork-aware UI** — Per-track artwork loading, palette-aware presentation, supplied light/dark fallback covers, and local cache support.
- **Personal appearance** — Theme, accent, typography, layout, navigation, Mini Player, and widget customization.
- **Home-screen widget** — Playback actions and configurable widget presentation.
- **Audio controls** — Equalizer, crossfade, playback tuning, and supported sound options.

## Screenshots

The repository keeps screenshot assets in [`screenshots/`](screenshots/README.md).

Add real device captures there using the documented names and update this gallery when the images are available. Keeping images in-repository makes the README work on GitHub, in forks, and in offline documentation mirrors.

## Release downloads

The current release is published as **[XVOX v1.01.01](../../releases/tag/v1.01.01)**.

Each signed release asset contains:

| File | Purpose |
|---|---|
| `app-release.apk` | Installable release APK |
| `app-release.apk.idsig` | APK Signature Scheme v4 sidecar file |
| `SHA256SUMS.txt` | Integrity hashes for release assets |

> Keep the `.apk` and `.idsig` files together after extracting the release download. The APK remains the file users install; the `.idsig` sidecar preserves the v4 signing output.

## Build and release quality

Official release builds run through the repository's manual GitHub Actions workflow.

- **Release variant only** — `:app:assembleRelease`
- **R8 enabled** — code shrinking, optimization, and resource shrinking are active for release builds.
- **APK alignment** — release APKs are zip-aligned before signing.
- **All Android APK signature schemes** — v1, v2, v3, and v4 are explicitly enabled.
- **Artifact verification** — the workflow verifies the signed APK and requires the generated `.idsig` file.

### Signing security

Release signing material is intentionally never committed. The workflow reads these GitHub Actions secrets only on the runner:

- `XVOX_RELEASE_KEYSTORE_BASE64`
- `XVOX_RELEASE_STORE_PASSWORD`
- `XVOX_RELEASE_KEY_ALIAS`
- `XVOX_RELEASE_KEY_PASSWORD`

Do not commit, upload, or share a keystore, Base64 keystore text, key alias password, or store password.

## Technology

| Area | Implementation |
|---|---|
| Language | Kotlin / Java 17 |
| UI | Jetpack Compose + Material 3 |
| Playback | AndroidX Media3 / ExoPlayer / MediaSessionService |
| Preferences | DataStore Preferences |
| Artwork | Coil 3 |
| Metadata | Jaudiotagger |
| Minimum SDK | Android 13 / API 33 |
| Build optimization | R8 + resource shrinking |

## Project layout

```text
app/
├── src/main/java/com/xvox/music/
│   ├── core/          # Design system, shared UI, navigation, overlays
│   ├── features/      # Home, search, settings, setup, playlist flows
│   ├── player/        # Playback, Media3 session, Now Playing
│   ├── widget/        # Home-screen widget support
│   └── data/          # Preferences and local application state
├── build.gradle.kts   # Android app configuration
└── proguard-rules.pro # R8 keep rules

screenshots/           # README screenshot assets and naming guide
.github/workflows/     # Manual signed release workflow
```

## Contributing and issue reports

Before opening an issue, include:

- Android device and OS version
- Exact steps to reproduce
- Expected and actual behavior
- Screenshot or screen recording when possible
- Relevant, non-sensitive logs only

Never include signing keys, passwords, private library paths, or personal media files in an issue.

## License

**Proprietary — All Rights Reserved.**

Copyright © 2026 lxzrvi.

This repository is public for viewing and inspection. No permission is granted to copy, modify, distribute, or incorporate any part of XVOX into another product without prior written permission from the copyright holder. See [LICENSE](LICENSE).

## Contact

For enquiries, suggestions, or permission requests:

[thaparavi382@gmail.com](mailto:thaparavi382@gmail.com)
