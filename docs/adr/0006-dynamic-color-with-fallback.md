---
status: superseded
---

# Wallpaper-derived dynamic colour with an explicit fallback

Superseded by [ADR 0013](0013-galery-owned-ui.md). Kept for historical context.

Colour selection in the root theme uses `dynamicLightColorScheme(context)` or `dynamicDarkColorScheme(context)` on Android 12 and above (API 31) when dynamic colour is enabled. Otherwise it falls back to `darkColorScheme()` for dark mode and `expressiveLightColorScheme()` for light mode.

## Considered Options

- **A fixed brand palette only.** Rejected because it loses Material You personalisation and the wallpaper-derived tonal palette.
- **Dynamic colour with no fallback.** Rejected because dynamic colour does not exist below API 31.
- **expressiveDarkColorScheme().** Rejected as non-existent, despite being a common claim in third-party posts. Dark mode uses `darkColorScheme()`.

## Consequences

- The `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` check and the `when` chain belong in the theme composable, not in individual screens.
- Dynamic colour re-reads when the wallpaper changes. Use matched role pairs (`onPrimary` on `primary`, `onPrimaryContainer` on `primaryContainer`) for accessible contrast.
- If a brand palette is needed, generate it with Material Theme Builder and use it as the non-dynamic fallback instead of mixing custom colours into dynamic schemes.

**Sources:** [Material 3 theming guide](https://developer.android.com/develop/ui/compose/designsystems/material3) · [MaterialExpressiveTheme reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable)
