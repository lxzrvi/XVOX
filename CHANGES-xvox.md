# XVOX — xvox branch fixes

Base: `201428bfd541c0b52c15a1c7a7ce7568e741104f` on `xvox` (not `main`).

## Included changes

- Mini-player and keyboard clearance share a 12 dp gap above the visible navigation boundary.
- App-wide centered XvoxBox dialogs, contextual headers and an X close button; no bottom sheets for profile, playlists, queue, timer, song options or now-playing options.
- Single tap plays with immediate press feedback; the current song card gets a subtle artwork-dominant tint. Home cards have an options button; hold also opens options.
- Select command in song options across Home, Liked, playlists and Search; selection actions stay directly below the app header.
- Procedural mosaic layouts with 24 card treatments, refreshed on a fresh app launch and stable during browsing/rotation.
- Horizontal Home mode becomes a bounded, free-direction All Songs canvas (including diagonal drag). Recents has independent positioning and compact spacing.
- Recents Top/Bottom preference directly under Hide Recents.
- Refined settings, live layout/appearance/EQ/spatial/crossfade/filter previews and accessible sliders.
- Smooth software EQ, automatic boost compensation, adjustable boost-protection headroom, output ceiling and peak guard.
- Binaural-style orbit using ear delay, head shadow and gentle crossfeed; depth/speed controls. Best evaluated with headphones.
- Service-owned, preloaded dual-player crossfade. Current and next tracks overlap using equal-power envelopes; the next player's buffers and position survive handover. Pause/seek/queue changes are handled without the old self-cancelling zero-volume job.
- Custom duration (seconds/minutes) and size (KB/MB) filters. Virtual device-audio folder browser with checked = excluded, full-path matching and recursive exclusions.
- Settings/Search Back returns Home. Home Back shows the sad-face Yes/No stop confirmation. No/X does not stop playback.
- Directional tab slides; opaque isolated page surfaces prevent Settings showing through Home. Liked/playlist transitions are fade-only. Header glass opacity matches navigation/mini-player.
- Explicit XVOX small notification icon.
- Five regression-test source files (22 tests) included.

## Push / apply

The ZIP is the **complete current source tree**, without `.git`, APKs, build outputs or machine-local SDK files.

The separate `XVOX-xvox-fixes.patch` applies to the base commit above, including file removals/renames. On a clean matching checkout:

```sh
git switch xvox
git apply --check /path/to/XVOX-xvox-fixes.patch
git apply /path/to/XVOX-xvox-fixes.patch
git add -A
git commit -m "Fix XVOX dialogs, home gestures, playback blending and audio DSP"
git push origin xvox
```

If copying files manually, use the extracted source tree as a replacement rather than only merging it over old source files. Remove these obsolete paths (relative to `app/src/main/java/com/xvox/music/`):

- `features/home/SongOptionsSheet.kt` → `SongOptionsBox.kt`
- `player/nowplaying/components/NowPlayingOptionsSheet.kt` → `NowPlayingOptionsBox.kt`
- `shell/XvoxPlaylistPickerSheet.kt` → `XvoxPlaylistPickerBox.kt`
- `shell/XvoxQueueSheet.kt` → `XvoxQueueBox.kt`
- `shell/XvoxTimerSheet.kt` → `XvoxTimerBox.kt`
- `player/playback/PlaybackTrackTransitionHelper.kt` (removed)
- `player/playback/PlaybackVolumeFadeHelper.kt` (removed)

Do not keep both old and renamed files; several contain shared declarations.

## Manual run

The final source was **not rebuilt**, as requested. It is prepared for your manual push/run. No physical-device playback or gesture verification is claimed.

JDK 17 and Android SDK 36 are required. `compileSdk` is now 36, matching the existing target SDK; SDK 37 was not resolvable in the SDK catalog used here. Other pinned toolchain versions are preserved.

```sh
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

### Device checklist

1. Tap songs once in All Songs, Liked, playlists, Recent and Search; verify press feedback, playback and cover tint.
2. Open every box, scroll long content, show the keyboard and close via X/Back. Check small screens and landscape.
3. Select songs from options. Verify the toolbar remains under the header and bulk actions affect only selected songs.
4. Drag All Songs horizontally, vertically and diagonally in free-pan mode. Recents must not move with that gesture. Check the last vertical mosaic page for excess empty space.
5. Restart XVOX for a new mosaic; rotate/switch tabs without reshuffling it. Try Recents Top/Bottom and Hide.
6. Try light/dark/AMOLED, rapid tab switches, header transparency, IME and mini-player gaps, and each Back path. No/X in the exit box must keep music playing; Yes stops it.
7. With moderate device volume, sweep EQ bands/presets and headroom while listening. Check mono/stereo files and surround depth/balance with headphones.
8. Crossfade at 1/3/12 seconds: normal queue, repeat one/all, short files, pause/resume mid-blend, seek, skip, reorder, background playback and headset interruption. Confirm both tracks overlap and the incoming track continues.
9. Check custom filter boundaries, duplicate folder names on different volumes, recursive exclusions, empty libraries and restoring excluded folders.
10. Verify XVOX's small icon and playback controls in the notification/status bar.

Crossfade blends audio; it does not beat-match/time-stretch different songs. Spatial perception depends on the recording/headphones. Digital peak protection cannot guarantee that a physical speaker will not distort at excessive system volume.
