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
