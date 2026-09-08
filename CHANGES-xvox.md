# XVOX — revision 3 (source only)

Branch: **xvox**. Base commit: `8dba0f043a19a59abffdb1ed95011d293c861d9c`.

## Startup, tap response and Recents

- Removed the hard-coded five-second startup wait. Loading no longer constructs/scans the Home screen underneath the startup animation before the setup decision is known.
- Song tap feedback no longer delays the playback callback. Cards give a short press pulse, without loading or retaining the artwork's dominant background colour.
- Playback starts with the chosen track and a small successor window. Large timelines are materialized in cancellable batches instead of serializing the whole library on the tap path. Identity/index caches avoid repeated scans; an explicit source change invalidates stale background expansion.
- Repeated taps on the active song resume/acknowledge it instead of seeking back to zero. Idle/error sessions are prepared before retrying.
- The Recently Played **carousel** stays mounted during updates. Its old transition replaced the LazyRow with an inert card and could leave it stuck after cancellation. There is no such static transition state now.
- Appearance preview removed. Home has a compact layout-only schematic, with proportionally drawn grid rows/section order—not a second full Home screen.
- Now Playing uses a three-slot, song-identity-anchored artwork pager. Shuffle/reorder does not animate the current cover to a different numerical queue index.

## XvoxMix

- A 5 / 10 bands selector, frequency-aware preset/custom-curve conversion and persistence.
- Removed whole-preset automatic attenuation, so raising a band no longer silently lowers the master signal.
- Boost Protection is an explicit manual gain reduction. Its effect appears in the contour. Existing stored protection values are preserved; the unset/default value is now 0 dB.
- Stereo-linked, short look-ahead peak limiting replaces continuous heavy saturation. Its delay is drained at end of stream. Fixed EQ filters and gain ramps remain; filter histories are not reset by slider movement.
- Low-level noise reduction and a Soften Highs control. The preview reflects the selected bands, manual protection, high-frequency shaping and noise setting.
- 3D preview is shown only while enabled. Depth, speed, balance, app volume and output ceiling affect it. The headphone processor keeps more low-frequency energy centred while applying directional/rear cues and reflections.

## Crossfade and its display

- Smart blending analyses bounded, local RMS/energy envelopes of the outgoing tail and incoming intro. It selects calmer hand-off points and, when credible, compatible beat positions. Playback never waits for analysis.
- Bass hand-off / clash control avoids having both low-frequency rhythms at full strength throughout the overlap. Queue mutations defer analysis to avoid decoder churn during large queue loading or dragging.
- Duration, smart mode, beat option and clash strength affect the schematic preview.
- The first/manual track does not get an intro zone. An intro zone is recorded only after a real fade-in. Only the subdued progress track is tinted, and a small **Crossfading** pill is shown while mixing. Extra intro/tail labels and song-name progress rows are removed.
- This reduces collisions; it does **not** guarantee musically perfect transitions for every recording, and does not force time-stretching/pitch changes or skip an intro.

## Widget playback and editor

- Widget actions start the playback service directly, not an Activity-bound controller. Foreground promotion precedes audio-focus acquisition, the MediaSession is explicitly registered, gain/duck state is restored, and focus retries are bounded.
- The service survives closing an active playback UI. It buffers/restores a filtered queue, can resume without an Activity, and respects explicit Stop/new-source requests. A normal wake-lock permission supports screen-off playback. Legacy widget broadcasts forward to the new service path.
- Widgets have **no progress bar** and no position-only redraw loop.
- 1–6 columns × 1–6 rows provide 36 size previews; actual supported cells depend on the launcher.
- General: margin X/Y, padding X/Y, horizontal/vertical alignment, shape radius, border width/colour, transparency and theme/custom colour.
- Cover: auto/left/right/top/bottom/hidden, requested size, independent or matching corners, cover border, full-cover background and shade. Full-cover mode still allows editing text and controls.
- Text: independent song/artist/logo visibility, placement, alignment, font family, font size, text/background/border colours, border width and radius.
- Buttons: per-button or all-button editing; auto/left/centre/right/hidden, order, size, inner padding, icon colour, background colour, border width/colour/radius, and optional captions with size/colour.
- Dynamic RemoteViews use small Android-8-compatible child layouts and clear old children before reapply. Previews inflate the same non-interactive layouts. Very small/overcrowded configurations clamp element dimensions; use a larger widget for more visible content.
- Swiping away the UI and Android **Force stop** are different. Android can disable/cancel widgets or background starts for a force-stopped/restricted app; the app cannot override that policy.

## Lyrics and library recovery

- Lyrics settings appear immediately below Home in Settings and in Now Playing's three-dot box.
- Global timing advance/delay, current-line size, other-line size, independent top/bottom fade areas, and Soft Fade / Slide / Focus Zoom entry styles.
- The preview uses placeholder lines only. The actual artwork/full-screen lyrics share the same preferences and apply the timing offset to lyric seeking. Now Playing gets finer position updates for smoother lyric timing.
- Hidden Songs is below About. Songs removed with **Remove from XVOX** can be restored individually or together. Likes and playlist membership are retained.
- Device-deleted files are not recoverable here; unavailable files are labelled accordingly, and other library filters still apply after restoring.
- How To Use expanded to 31 topics. About explains the local library, DSP, privacy and platform limits without a bit-perfect-output claim.

## Validation / manual run

**No Gradle build, Kotlin compilation, test execution, device/audio test, workflow dispatch, commit or push was performed during source preparation.**

Performed: static Kotlin parsing, Android resource-reference checks, XML/manifest checks, dynamic widget-slot validation, whitespace checks, archive byte verification and patch applicability checks. Static checks do not replace compilation or device testing.

There are **50 regression test methods provided as source**, including EQ mode/level behavior, limiter history, lyrics offsets, widget customization bounds and energy/blend properties. They have not been executed for this revision.

### Highest-priority device checks

1. Cold launch with a large library; tap songs quickly and switch sources while the remaining queue loads. Sound should start without waiting for the whole timeline. Stop/new-source selection must cancel stale expansion.
2. At the bottom of Home, swipe Recent cards during new-song updates, fling, interrupt and reverse. The carousel must remain interactive.
3. Shuffle in Now Playing while keeping the current song/position; verify the current cover stays anchored.
4. With moderate system volume, test five/ten bands, presets, heavy boosts, manual protection, noise reduction and high softening. Check mono/stereo and different sample rates, as well as file endings.
5. Test energy/beat/clash settings with similar/different rhythms, ambient/quiet intros, repeat one/all, short songs and seeks. Verify first-track cue zones and the Crossfading pill.
6. Stop playback, swipe the Activity away, then press the widget Play button. Also test pause/resume, next/previous/like, screen-off playback, exclusive focus held by another app, media permission denial and empty/excluded libraries.
7. Resize widgets, change every editor category and test full-cover mode. Check caption/icon contrast, margins, borders and overcrowded tiny sizes. No progress indicator should appear.
8. Import LRC/plain text, change positive/negative offset, font sizes, edge fades and the three styles. Confirm the same settings work in artwork/full-screen lyrics and the player options box.
9. Hide from Home/Liked/playlist/search, then restore from Hidden Songs; verify physically deleted or disconnected-storage files are not falsely reported as recovered.

Digital limiting and generic spatial processing do not guarantee speaker/hearing safety or a specific listener's 3D perception. Start with moderate volume.

## Apply to xvox

The ZIP is the complete source tree, excluding .git, build outputs, APKs and machine-local SDK files.
The separate patch applies on the base commit above:

```sh
git switch xvox
git apply --check /path/to/XVOX-xvox-revision3.patch
git apply /path/to/XVOX-xvox-revision3.patch
git add -A
git commit -m "Improve XVOX startup, playback, widgets, lyrics and library recovery"
git push origin xvox
```

Then run your build/workflow manually. Existing JDK/SDK/Gradle versions are unchanged.
