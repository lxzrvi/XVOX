# XVOX revision 6 — working tree only (not built or pushed)

Branch: **xvox**. Uncommitted working-tree edits over `9a8cf26`; not compiled in this environment.

## What changed in this pass

### Launcher icon
- Adaptive foreground regenerated with a **smaller** mark (~27 % of the canvas, near the original look) in every density.

### Appearance (settings)
- **Background** group: Default / Midnight / Warm colour presets + one **custom image** picker (`theme_background`, `theme_background_image`).
- **Card transparency** slider (0–60 %) applied through the palette (`card`, `cardElevated`, `surface`) so every screen blends cards with the backdrop.
- Background photo/tint and card transparency reach Home, Liked, Playlists, Search and Settings through `TabSurface`; **Now Playing is exempted** with its own solid chrome (`ProvideXvoxNowPlayingChrome`).

### Accent
- **Red is the default**; Blue and White remain alternatives with the exact iOS-style pairs: red `#FF3B30` light / `#FF453A` dark·AMOLED, blue `#007AFF` light / `#0A84FF` dark·AMOLED. Legacy names fold into Red.

### XvoxSplit → no Home presence
- Removed from XvoxMix (Equalizer) settings and from Home entirely (section order, dedicated section, SPLIT mode routing, header heart no longer cycles through it). Reachable only via Now Playing › More › XvoxSplit.

### 3D sound
- Restructured into exactly: **Width, Depth, Position, Room, Reverb, Movement, HRTF/Spatial, Center Preservation**; every knob is wired end-to-end (engine smoothing, `LiveEqState`, preferences `surround_width`/`surround_position`/`room_amount`/`reverb_amount`/`hrtf`/`center_preservation`, ViewModel setters).

### Press / haptics
- `SongPress`: the pulse starts only after 28 ms and cancels on scroll slop — All Songs cards only “push” on a true tap.
- Recents tile shows the **whole square cover** while truly pressed (no cropped band), reverting on release.

### Now Playing
- Dominant backdrop colour now preserves the cover's true hue/saturation (HSL lightness band only).
- The old XvoxSplit pill is replaced by a **round crossfade toggle** (new `ic_xvox_crossfade`) at star/like size.
- The three utility icons **swipe left** to reveal XvoxMix / 3D sound / Lyrics — tap toggles on/off, long-press opens the options box.
- The sheet's top corners round progressively as it slides down (dismiss/close).

### Home / shell
- Header refresh/liked action pill is more transparent.
- Queue drag target is arithmetic from scroll offsets, so dragging **up** auto-scrolls exactly like down.

### Add to playlist = PIP popup
- Every “+ Add to playlist” trigger now opens a compact bottom-anchored **PIP-style popup** (`showMiniBox`) with a grabber handle instead of a full-screen editor — no navigation away from what you were doing. (A cross-app overlay bubble that floats above other apps needs `SYSTEM_ALERT_WINDOW` + a background overlay service and permission onboarding; not added in this pass.)

### Widget settings
- Existing dedicated per-element panels kept (Surface / Cover / Text / Buttons) with per-element sizes, negative margins/paddings, cover bleed and no separate text box.
- Buttons can now be **moved freely**: per-button Nudge X / Nudge Y (−48…48 dp, `offsetX`/`offsetY`, persisted and applied via `RemoteViews` layout margins).

### Exit / playlist covers
- Exit confirmation: sad face **only**, no outer circle.
- Playlist cover editor: any 1–4 picked covers applies; changing covers no longer locks after several picks.

# XVOX revision 5 — built source update

Branch: **xvox**. Working-tree edits only; nothing pushed.

## What changed in this pass

### Startup
- The hard-coded five-second wait is gone. `AppViewModel` now drives a **determinate** startup bar from real milestones (library read → sorted → artwork warm → player connected → split catalogue → Home laid out) with a slow creep between them, so it never looks frozen.
- `XvoxAppRoot` mounts the shell **underneath** the loading screen and only lifts it after three frames plus a settle, so Home is never revealed half-built.
- A 9-second ceiling releases the UI even if one stage never reports back. The loading screen can no longer hang.
- The startup screen now shows the real XVOX mark instead of a text logo.

### Mosaic / All Songs
- New shared exact-cover engine (`mergeToExactCover`): tiles are merged out of a full unit grid, so a page can never contain a gap, an overlap or an unused corner — **including the last page**. Verified exhaustively for every 4 × 1…8 grid and every tile count.
- **Mosaic 1** gained a real shape vocabulary: 1×1, 2×1, 1×2, 2×2, 3×1, 1×3, 3×2 and 4×1 — long, short and wide — instead of four presets, with ~399 distinct layouts per 400 seeds.
- Page counts are balanced so the final page is a real page, never a leftover strip. Paged (horizontal) mode fills every row; flowing (vertical) mode claims fewer rows rather than padding itself out.
- Grid jitter fixed: tile offsets moved to the layout phase (`Modifier.offset { }`), click lambdas hoisted per page, page geometry memoised.

### Touch feedback
- `xvoxSongPress` / `xvoxPressScale` now animate an `Animatable` read only inside `graphicsLayer`, driven from the pointer **Initial** pass. A press no longer recomposes the card, so the **first** tap always registers even while the library is still settling.

### Queue
- Rows carry a single **six-dot drag handle**; the old trailing icon is gone.
- Drag-to-edge auto-scroll fixed. The target index is computed arithmetically from a uniform row stride instead of a one-frame-stale layout snapshot, and the player is told about the move **once**, on release, instead of dozens of times per second. That removes both the repeated-song glitch and the failure to actually scroll.

### Accent on the playing song
- In **All Songs**, the card of the song that is currently playing shows its **title in the accent colour** (both the uniform card and the mosaic card, including titles drawn over artwork). Now Playing keeps its normal title colour.

### Settings
- Every accordion row is a **label and a chevron**. All descriptive subtitles removed; sections rewritten with short labels only (Appearance, Home, Playback, XvoxMix, Widget, Library filter, How to use, About, Don't kill app).
- **Back collapses** the open row before it ever leaves Settings; opening the tab always lands on the top of the list with everything collapsed.
- **Appearance**: every option is outlined, so unselected choices are visible; selection animates fill + border weight.
- **Playlists moved into Home settings**, where they belong, with a new **long-card height** option (Auto / 90–220 dp).
- **Home preview** is now the whole screen — header, every enabled section in order, the real mosaic geometry, playlists at their chosen style/height, bottom bar — laid out at the phone's real size and scaled to fit. No scrolling.
- **Lyrics** gained the same kind of full-screen preview.
- Preview panes no longer scroll; only the controls below them do.

### XvoxSplit
- Moved under **XvoxMix › 3D sound** as a spatial option (beat left / vocal right, then placed in 3D). Removed from Playback.
- **The "Android blocked something" failure is fixed.** The pipeline was extracted into `XvoxSplitPipeline`, so it no longer depends on a foreground service being allowed to start: if the service is refused — background-start restrictions, notification permission, OEM policy — the identical work runs in the app process instead, and the service also falls back on `startForeground` failure and on the FGS timeout.
- **The model download is fixed and independent.** It has its own button and runs with no service involved. Redirects are followed manually (the JDK client silently drops cross-host redirects, which is what stalled the GitHub release URL), transfers retry three times, and failures now report the real error instead of a generic message.

### Crossfade
- Two songs now read as one. The incoming track's level is matched to the outgoing tail across the blend and relaxed back to its own level by the end, so there is no step; its silent intro is skipped so the join has no dead air.

### Widget
- Settings reorganised into ordered groups that follow how the widget is built: size → surface → cover → text → buttons → install.
- **Negative margin and padding** for the cover, and negative nudges for cover text, so artwork and text can sit outside the box. Implemented with `setViewLayoutMargin` (API 31+), because Android clamps view padding at zero.

### Playlists / add-to-playlist
- A playlist the song is already in shows a **check**, not a plus, and tapping it **removes** the song.
- "Select playlist" keeps a light surface and a border in its disabled state; rows are outlined too.

### Recently played
- Each recent card carries a small round **source badge** in its left corner. Tapping it names the origin in the XVOX pill: *From Liked*, *From <playlist>*, *From All Songs*, *From XvoxSplit*.
- Origins are persisted alongside the recent list, and recents now resolve from **every** source — not only All Songs.

### Profile pictures and playlist covers
- Custom pictures **stack**: each one the user keeps sits beside the built-in avatars, the add button always stays, and every custom picture carries its own delete badge. Persisted across restarts.
- The destructive cleanup that deleted every profile image except the selected one — the reason a second picture could never be kept — now only reclaims true orphans.
- The same stacking gallery applies to playlist custom covers, and the setup screen uses the identical picker.

### Confirmations
- The sad-face circle is gone. One shared `XvoxConfirmBox` covers exit, song deletion and playlist deletion.
- "Delete from XVOX" now asks first and moves the song to **Deleted songs** (renamed from Hidden Songs).

### Icons
- Notification icons at every density are regenerated from the real XVOX mark: white, transparent, correctly padded for the status bar.
- Adaptive launcher icon now uses a properly scaled foreground (the old one drew the mark at ~27 % of the canvas) and gains a monochrome layer for themed icons.
- `drawable/xvox_mark.png` added for in-app use.

## Verification note

No Android SDK is available in this environment, so the project was not compiled here. The
highest-risk new logic — the mosaic exact-cover engine — was ported and exhaustively checked for
tile count, bounds, overlap and full coverage across every grid and count; unit tests were updated
to assert the same invariants, plus the new crossfade level-match/lead-in behaviour and the
negative widget offsets.

---

# XVOX revision 4 — built source update

Branch: **xvox**, based on `aae1e67966bf8a54f81b0f6357163fd16b0713fc`.

## Requested refinements

- Restored the original **five-second minimum loading period**. Home waits for the filtered library, initial artwork warm-up, split catalogue and playback connection. Sorting/grouping/warm-up run outside UI composition rather than constructing Home under the loading animation.
- Notification/status icons now use a monochrome, transparent rendering of the actual XVOX mark in the repository, not a generic letter X. Android/OEM settings still determine status-bar visibility and placement next to the clock.
- Mosaic 2 has balanced wide/square/portrait tiles, protected wide tiles, rebalanced page counts and no artificial short-page bottom rows. Small whole libraries shrink naturally; full pages fill their row budget.
- Playlists default to full-width stacked **Long cards**, retaining the original card height/style. **Original cards** remains available in a dedicated Playlists editor.
- Home, Playlists, Widgets, Lyrics, XvoxMix and Playback editors open in XvoxBox pages. Preview stays at the top; controls scroll below. Now Playing's three-dot menu uses the same boxed editor pages and Back navigation.
- Crossfading is announced through a persistent **XvoxP** popup and dismissed when mixing ends/pauses. No bottom Crossfading popup is rendered.
- Lyrics use stable maximum-size row geometry, transform-based text animation and drag-only browsing detection. Removed repeated end-of-scroll corrections. Edge fades are stronger and independently controlled. Five modes: Soft Fade, Slide, Focus Zoom, Glide and Spring. Preview uses the same renderer with placeholder lines only.
- Cover margin X/Y and padding X/Y controls were added to widgets. They also inset full-cover artwork. Larger covers use the actual available body height rather than a fixed 48 dp auto size. One-row widgets still cannot fit artwork taller than their physical space; use more rows or reduce spacing.
- Explicit app haptics are now restricted to navbar taps; song/playlist tap/long-press feedback is visual only.
- Mini-player progress is drawn at the card's top inside a single clipped inner-border path. It cannot draw above/outside the rounded mini-player border.

## XvoxSplit — real source separation

You chose actual vocal/instrument separation rather than a stereo approximation.

- Uses the verified **UVR MDX-Net 9482** neural model through ONNX Runtime Android 1.23.2. The APK does not bundle the weights: setup asks before downloading **29,704,738 bytes (~28.3 MiB)** from the pinned HTTPS release URL and verifies SHA-256.
- Local mono/stereo decoding, windowed-sinc resampling to 44.1 kHz, centred periodic-Hann STFT, ONNX inference, inverse STFT and overlap stitching. Vocals are predicted by the model; accompaniment is the original mixture residual. These are real inferred stems, not centre/side EQ.
- A prepared file stores separate mono accompaniment/vocal channels in an internal stem-pair WAV. Playback restores their level and moves them oppositely between left/right. The ordinary spatial effect is not stacked on top of this routing. Other selected audio processing still applies.
- Normal playback continues until up to two initial queued tracks are prepared. Then the queue continues in one background worker. Ready tracks switch at the preserved playback position; paused music is not forcibly unpaused.
- Foreground processing notification, explicit battery/storage consent, mobile-download opt-in, cancellable tasks, per-track failure handling, memory/storage checks and OS processing-timeout handling.
- Progress pill before the star: ready/total plus processing count. Tap for tasks; tap queued/working items to stop them. Long-press prepared items for Normal, XvoxSplit, Add/Save or deletion of the prepared copy.
- Three different manual track changes within four seconds disable processing/routing and restore normal playback. Prepared versions remain available. An explicit Use XvoxSplit action can resume after this guard.
- Saved versions appear in the **XvoxSplit** collection. Header cycle: Liked → XvoxSplit → Home, with a matching waveform icon. Merge supports the collection; Home visibility and the Now Playing pill can be hidden separately.
- Ready/saved/status lookups are indexed. Progress updates do not rebuild the full task catalogue. Unsaved cache is bounded to roughly 768 MiB, protecting current/next tracks; saved copies are not automatically evicted.

### Limits to understand

Separation can have artifacts/leakage, and accompaniment is not a drums-only stem. Stereo is folded to one mono channel per stem for the requested spatial routing. Processing can take minutes and may run slower than playback on some devices; a two-track buffer cannot guarantee unlimited uninterrupted prepared playback. Unready/evicted tracks fall back to normal. Cancellation may wait for the current native inference chunk to return.

Prepared audio uses about 10 MiB/minute, plus temporary decoded audio. Large/high-rate files can need substantial free storage. Android force-stop, background restrictions, thermal limits and foreground-service time limits still apply. The model download and processing do not upload your music. See `THIRD_PARTY/XVOXSPLIT.md` and the bundled third-party notices.

## Validation performed

After your explicit **Build + automated checks** approval:

- `:app:compileDebugKotlin` — successful.
- `:app:testDebugUnitTest` — **54 tests, zero failures/errors**. Includes FFT/STFT reconstruction, balanced mosaic coverage, existing DSP/lyrics/layout tests.
- `:app:assembleDebug` — successful; signed universal debug APK produced.
- Two real ONNX inference smoke checks on synthetic stereo inputs returned the expected finite `[1,4,2048,256]` output. These are contract/smoke checks, not a separation-quality benchmark or phone performance result.
- APK signing/alignment checks were run. ONNX Runtime was upgraded to 1.23.2 after checking ARM64 native ELF load alignment; its native libraries use 16 KiB-compatible alignment. Native libraries are compressed in the APK and extracted on install to keep download size manageable.
- Full lint was attempted but did **not complete** within this sandbox's resource/time limit; it was stopped. No clean-lint claim is made.
- No Android device/emulator was attached. Real-phone UI/gesture, battery, source-separation quality, cold widget playback and long-queue processing still need device testing.
- No GitHub push or workflow dispatch was performed for this revision.

## Deliverables / use

- `XVOX-xvox-revision4-debug.apk` — universal test build, locally debug-signed.
- `XVOX-xvox-revision4.zip` — complete source tree; no SDK, build output, credential or model weights.
- `XVOX-xvox-revision4.patch` — changes against the base commit above.

A prior APK may use a different signing key. If Android reports a signature mismatch, build with your existing key; uninstalling can erase local playlists/settings, so do not uninstall without a backup you trust.

### Manual device checklist

1. Cold launch: the loading time is not shortened, animation remains responsive, and Home appears after essential readiness.
2. Check Mosaic 2 at 4×3/4×4/4×5/4×6/4×8, including last pages and small libraries, for wide tiles and no empty bottom rows.
3. Switch original/long playlist cards and verify full-width stacking retains card height.
4. Open each settings editor and Now Playing submenu; scroll controls while preview stays pinned. Test lyrics fades and all five animations.
5. Confirm only navbar taps vibrate, and the mini-player progress stays within the top border.
6. Try XvoxSplit with two short local songs first, on Wi-Fi and at moderate volume. Confirm model consent/checksum, background progress, Normal→prepared switch, cancellation, rapid-skip auto-off, Save/Add and collection visibility.
7. Test with screen off, app backgrounded, limited storage, unavailable originals and different supported audio formats. Check that originals are not changed or removed.
8. Check the actual logo in notification/status UI, XvoxP crossfade lifecycle, larger widget cover settings and cold widget playback.

### Apply manually

```sh
git switch xvox
git apply --check /path/to/XVOX-xvox-revision4.patch
git apply /path/to/XVOX-xvox-revision4.patch
git add -A
git commit -m "Refine XVOX editors and layouts; add XvoxSplit background separation"
git push origin xvox
```
