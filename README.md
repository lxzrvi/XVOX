
<div align="center">

# XVOX

### Your Music. Your Space. Your Sound.

A modern, offline-first Android music player built with **Kotlin and Jetpack Compose**. XVOX focuses on a premium listening experience with a clean AMOLED-friendly interface, customisation, powerful playback controls, and local music library support.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin)
![Version](https://img.shields.io/badge/Version-0.1.0-black?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android-blue?style=flat-square)

</div>

---

## ✨ Features

- 🎧 **Offline Playback** — Play music stored locally on your device.
- 📚 **Music Library** — Browse songs, favourites, and playlists.
- 🎨 **Premium UI** — Minimal, AMOLED-friendly design with custom themes.
- 🖼️ **Dynamic Artwork** — Album art with adaptive colour backgrounds.
- 🎤 **Lyrics Support** — View synced and unsynced lyrics.
- 🎚️ **Audio Effects** — Equalizer, presets, and supported sound effects.
- 🎵 **Queue Management** — Manage upcoming tracks and playback.
- 🪄 **Smooth Player UI** — Mini-player, Now Playing screen, and artwork transitions.
- ⚙️ **Customisation** — Personalise supported playback and appearance settings.

## 📸 Screenshots

<!-- Add your screenshots to the screenshots/ folder -->

<div align="center">

| Home | Now Playing | Library |
|:---:|:---:|:---:|
| ![Home](screenshots/home.png) | ![Now Playing](screenshots/now-playing.png) | ![Library](screenshots/library.png) |

</div>

> Replace the example image paths with your actual screenshots.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Playback:** AndroidX Media3 / ExoPlayer
- **Background Playback:** MediaSessionService
- **Preferences:** DataStore
- **Artwork Loading:** Coil 3
- **Metadata:** Jaudiotagger
- **Minimum Android:** 8.0 (API 26)

## 🚀 Getting Started

### Build from Source

```bash
git clone https://github.com/YOUR_USERNAME/XVOX.git
cd XVOX
./gradlew assembleDebug
```

Install the generated APK from `app/build/outputs/apk/debug/`.

**Requirements:** Android SDK and JDK 17 for building.

## 📋 Project Status

XVOX is currently under development. Features, design, and functionality may change between releases.

## 🗺️ Roadmap

- [ ] Further playback and library improvements
- [ ] More customisation options
- [ ] UI and performance refinements
- [ ] Additional audio features

## 🔐 Permissions & Privacy

XVOX is designed around local music playback. Android permissions may be required to access audio files and provide playback functionality. See the app source code for the exact permissions and behaviour.

## 📄 Licence

No licence has been specified yet. Unless a licence is added to the repository, standard copyright restrictions apply and reuse or redistribution is not automatically permitted.

## 🤝 Contributing

Suggestions, bug reports, and improvements are welcome. Open an issue or submit a pull request.

## 💖 Support

If you enjoy XVOX, consider starring the repository ⭐

---

<div align="center">

**Made with ❤️ for music lovers.**

</div>
