
<div align="center">

# XVOX

### Your music. Your space. Your sound.

A modern, offline-first Android music player built with **Kotlin** and **Jetpack Compose** — designed around expressive artwork, thoughtful interactions, and deep audio customisation.

<br/>

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Offline First](https://img.shields.io/badge/Offline-First-FF453A?style=for-the-badge&logo=musicbrainz&logoColor=white)
![Version](https://img.shields.io/badge/Version-0.1.0-8B8B8B?style=for-the-badge)

<br/>

**A personal music library, reimagined.**

</div>

---

## ✦ About

**XVOX** is a local music player focused on giving your own music a premium listening experience. Browse your library, organise tracks into playlists, personalise the interface, explore lyrics, and shape the sound to your taste — without building the experience around a streaming service.

The interface is crafted with Jetpack Compose, with artwork-led surfaces, a dark AMOLED-friendly aesthetic, animated navigation, and custom playback tools.

## ✨ Highlights

<table>
<tr>
<td width="50%" valign="top">

### 🎵 Your library

- Browse locally stored songs
- Liked tracks and custom playlists
- Recently played music
- Artwork-aware song collections
- Search your music library
- Queue management and reordering

</td>
<td width="50%" valign="top">

### 🎨 Personalise the experience

- Dark, AMOLED-friendly visual design
- Custom accent colours
- Background presets and custom background image
- Adjustable card transparency
- Artwork-led colour styling
- Customisable Home sections and layouts

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🎚️ Shape your sound

- Equalizer and sound presets
- Custom EQ controls
- Bass, treble and other audio effects
- Stereo balance and spatial-audio controls
- Crossfade between tracks
- Playback options designed for everyday listening

</td>
<td width="50%" valign="top">

### 🎤 Lyrics & metadata

- Lyrics view with timed lyric support
- Local lyric files and supported embedded lyrics
- Track information and metadata reading
- Album artwork presentation
- A dedicated Now Playing experience

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🎧 Thoughtful playback

- Media3 / ExoPlayer playback engine
- Media session integration
- Play next and queue controls
- Mini-player and full player
- Playback state and recent-listening support

</td>
<td width="50%" valign="top">

### 📱 Everyday convenience

- Home-screen music widget
- Playlist and track actions
- Artwork caching and preloading
- Haptic touch feedback
- Local-first music experience

</td>
</tr>
</table>

> Feature availability may vary by build. Some advanced audio effects depend on device capabilities and the active audio route.

## 🧰 Built With

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary programming language |
| **Jetpack Compose** | Modern declarative UI |
| **Material 3** | UI foundations |
| **Media3 / ExoPlayer** | Audio playback |
| **MediaSessionService** | Background playback and media controls |
| **DataStore Preferences** | Persistent settings |
| **Coil 3** | Artwork and image loading |
| **Android MediaStore** | Local music library access |
| **Palette** | Artwork colour extraction |
| **Jaudiotagger** | Audio metadata support |
| **GitHub Actions** | Automated workflows |

## 📸 Screenshots

Add screenshots of the actual application to showcase its interface.

Create a `screenshots/` folder in the repository:

```text
screenshots/
├── home.png
├── now-playing.png
├── equalizer.png
└── lyrics.png
```

Then add them here:

<p align="center">
  <img src="screenshots/home.png" width="24%" alt="XVOX Home" />
  <img src="screenshots/now-playing.png" width="24%" alt="XVOX Now Playing" />
  <img src="screenshots/equalizer.png" width="24%" alt="XVOX Equalizer" />
  <img src="screenshots/lyrics.png" width="24%" alt="XVOX Lyrics" />
</p>

*Replace these example paths with screenshots that actually exist in your repository.*

## 🚀 Getting Started

### Requirements

- Android Studio
- JDK 17
- Android SDK 37 for the current project configuration
- Android 8.0 (API 26) or newer for the configured minimum SDK

### Clone the repository

Replace `YOUR_USERNAME` with your GitHub username.

```bash
git clone https://github.com/YOUR_USERNAME/XVOX.git
cd XVOX
```

### Build the project

Open the project in Android Studio and allow Gradle sync to complete.

Build the debug APK using:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The generated APK should be available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Permissions & Local Music

XVOX is designed for music stored on your device. Grant the relevant Android media permission when prompted so the app can discover and play your local audio files.

Your music library is the focus: enjoy your collection without depending on an online music catalogue.

## 🗂️ Project Structure

```text
XVOX/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/xvox/music/
│           │   ├── audio/
│           │   ├── artwork/
│           │   ├── core/
│           │   ├── data/
│           │   ├── features/
│           │   └── playback/
│           ├── assets/
│           └── AndroidManifest.xml
├── .github/
│   └── workflows/
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

*Package names and folders may vary as the project evolves.*

## 🛣️ Roadmap & Development

XVOX is under active development. Features, visual details, and internal architecture may change between revisions.

For the latest progress, check the repository's commits, issues, and releases.

## 🤝 Contributing

Ideas, bug reports, and improvements are welcome.

1. Fork the repository.
2. Create a branch for your changes.
3. Keep changes focused and well organised.
4. Test your changes where possible.
5. Submit a pull request with a clear description.

When reporting a bug, include your Android version, device model, steps to reproduce, and relevant logs. Remove personal information before sharing logs.

## 📄 License

No project-wide license file was identified in the supplied source archive. Until a license is added, do not assume that the project is open for redistribution or reuse.

Check individual third-party assets and their included license notices before redistributing them.

---

<div align="center">

### XVOX

**Made for the music you already love.**

Built with Kotlin · Designed for Android · Focused on your library

</div>
