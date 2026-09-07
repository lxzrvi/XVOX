# XVOX — revision 2 source update

Branch: **xvox**, not main. Base commit: `0e592ad599e40cbb040b4f3379af0a91fc12a2fe`.

## Home and layouts

- Horizontal mode uses the original native horizontal LazyRow again. Free/diagonal panning is removed.
- **Mosaic 1** restores the original tiling/card style; **Mosaic 2** keeps the newer varied layouts. One Size remains available. The old `mosaic` preference migrates to Mosaic 1.
- No disc-shaped/centre-hole covers in Mosaic 2. Artwork follows the card's corner geometry with an inset radius. Titles sit below, above, alongside or over artwork according to the tile proportions.
- All Songs has no circular three-dot buttons. Hold opens options; a single tap gives a short push before playback. Cover-colour tint transitions over 480 ms.
- Home previews render the same cards, header, sections, mini-player and navigation at real layout dimensions, uniformly scaled on both axes. They use the current library (labelled sample data only if empty) and can be opened larger.
- **Merge** brings Liked Songs and Playlists onto Home, after Recents by default. Reorder or hide sections in Home settings. In merge mode, the top-right pill contains only Refresh.
- All Songs pages, liked rows and playlist rows are emitted as separate lazy items. Cosmetic preference changes do not rebuild the playback queue.

## Audio and performance

- EQ preview appears only when EQ is enabled.
- Replaced changing-feedback-coefficient EQ with warm fixed-pole filters and smoothed per-band gains. Audio controls update in memory immediately; disk persistence is coalesced after interaction.
- Protection follows the **currently interpolated** EQ response, rather than releasing headroom early when disabling a boosted preset. A continuous soft knee replaces abrupt limiter gain jumps.
- Headphone orbit adds fractional ear delays, equal-power directional cues, rear pinna filtering and subtle early reflections, with smoothly ramped depth.
- Optional **Beat alignment** analyses bounded intro/tail windows locally on one background decoder. When confidence and tempos are compatible, the incoming first beat is aligned with an outgoing beat. It does not skip the intro or force tempo/pitch changes. Playback never waits for analysis; unsupported/uncertain tracks use the regular equal-power blend.
- Teal intro / amber tail zones appear on player, mini-player and widget progress indicators. The player shows both tracks' progress during an active blend and labels a beat-aligned blend only when alignment was actually applied.
- Preference streams are distinct by value; library JSON is decoded only when its own stored value changes. Recent-history updates no longer re-sort the full library.
- Artwork decoding/fetching have shared concurrency limits. Prefetching is bounded, sequential and cancellable, and fills the same artwork cache used by cards.

## Bluetooth / headset

- Detect actual audio output routes: A2DP, BLE, wired and USB headsets, with route-negotiation debounce and disconnect handling.
- Auto Play resumes an existing queue or resolves a permitted, filtered local library and last song when the player has no items. An existing app queue/source is retained when available.
- This works while Android allows the app/service to run; it does not bypass force-stop or background restrictions.

## Widgets

- Independent **Padding X / Padding Y** settings, with safe caps for tiny sizes.
- Backgrounds are drawn at the actual widget aspect ratio; they are no longer stretched from one fixed 320×180 shape. Artwork stays inset and uses matching inner corner radii.
- Separate tiny, narrow/tall, square, compact, horizontal and standard layouts. Controls adapt to available space rather than overlap. Small sizes prioritize playback controls.
- All six XML variants share the same IDs. Settings previews inflate the actual non-interactive RemoteViews, including 1×2, 1×3, 1×4 and wide variants.
- Progress-only updates are throttled and send a tiny strip bitmap instead of repeatedly decoding covers/rebuilding full widgets. Full updates are coalesced. Responsive launcher sizes and portrait/landscape bounds are respected.
- Widget buttons connect to the media session even without a live Activity/ViewModel; Like now updates the library preference.

## Queue and boxes

- Queue drag capture is owned by the LazyColumn, not by a recycled row. A floating row stays under the finger while proportional edge auto-scroll reorders the list. The original row remains as a placeholder.
- Removed Sleep Timer, Song Info, Playlist and Share shortcuts from the top of the now-playing options box; XvoxMix and playback settings remain.
- Add Songs and cover-picker boxes grow with content up to the available space (660 dp body cap), and stay compact for short lists. Long lists/grids remain scrollable; existing playlist covers are preselected.

## Validation status

**No app build, Kotlin compilation, Gradle test run, workflow dispatch or push was performed for this revision**, as requested for source-only work.

Performed: static Kotlin syntax parsing, local resource-reference checks, XML well-formedness/shared-widget-ID checks, whitespace checks, ZIP byte verification and patch applicability checks.

There are **38 regression test methods provided as source**, including beat fallback/alignment, original/new mosaics, EQ switching, section ordering and widget geometry. They have **not** been run on this revision. Static parsing is not a substitute for compilation or device testing.

### Manual device checks

1. Switch horizontal/vertical and Mosaic 1/2/One Size while audio plays. Confirm no diagonal grid panning, no disc holes/dots, smooth tint and no playback interruptions.
2. Test Merge, all section orders/hide options, Recents Top/Bottom, and normal liked/playlist navigation. Compare the preview to Home.
3. Sweep all EQ bands, presets, headroom, volume and depth while playing mono/stereo MP3, FLAC and WAV. Toggle heavily boosted EQ on/off. Start with moderate device volume.
4. Listen to headphone orbit at different speeds/depths. Generic spatial cues and digital protection cannot guarantee a particular listener's 3D perception or prevent physical-speaker distortion at excessive system volume.
5. Test 1/3/12-second crossfades with compatible steady beats, different tempos, silence/ambient intros, repeat one/all, short files, seek/skip and pause/resume. Confirm ordinary blending remains available when beat alignment cannot be trusted.
6. Connect/disconnect A2DP/BLE/wired headphones with Auto Play on/off, with a paused queue and an empty player. Verify exclusions and last-song selection.
7. Resize widgets in portrait/landscape and all shown shapes. Test X/Y padding, radius, artwork boundaries, controls, Like, and playback when the Activity is absent.
8. Hold a queue row at both scroll edges, drag beyond the visible rows, reverse direction, and release. Also test TalkBack move actions.
9. Check small/large playlist song and cover lists, large text, rotation and keyboard visibility. X and confirmation actions must remain usable.

## Apply / push manually

The archive is the complete updated source tree, without `.git`, APKs, build outputs or local SDK configuration.
The separate patch applies on the base commit above:

```sh
git switch xvox
git apply --check /path/to/XVOX-xvox-revision2.patch
git apply /path/to/XVOX-xvox-revision2.patch
git add -A
git commit -m "Refine XVOX home, EQ, crossfade, widgets and queue gestures"
git push origin xvox
```

Then run your build/workflow manually. JDK 17 / SDK 36 and the repository's existing pinned Gradle/dependencies are unchanged by this revision.
