---
status: accepted
---

# Use Galery's own UI on Compose

Galery owns the palette, type, shapes, glass, and motion for Photos, Albums, Search, selection, and the Viewer. Compose Foundation provides layout, text, input, and animation. Media3 handles playback and Coil handles decoding. The active build settings are in `app/build.gradle.kts`.

- Keep the fixed dark palette and type in `ui/theme/Theme.kt`. Use `GalleryDesign.kt` for shared shapes, control sizing, press motion, and springs.
- Apply sampled glass through `GlassBackdrop.kt`. Position samples in screen coordinates so the same material works in the main window, menus, and sheets.
- Use the floating dock in compact windows and the side rail in wide windows. Keep chrome inside system insets and let media draw behind it.
- Let photos and landscape video hide their chrome. Video playback has a dedicated play control. Paused video keeps its controls.
- Ship the charcoal photo-frame mark as an adaptive vector with a legacy vector fallback. Keep the universal signed APK under 2,000,000 bytes.

Measure the first visible frame separately from the first populated photo viewport. Aim for a populated viewport in about two seconds on the OnePlus test device. Check size on the actual signed artifact.

This decision supersedes ADRs 0002, 0004 through 0009, and 0012. The edge-to-edge principle remains part of this decision.
