---
status: superseded
---

# Edge-to-edge and adaptive navigation from the start

Superseded by [ADR 0013](0013-galery-owned-ui.md). Kept for historical context.

The activity calls `enableEdgeToEdge()` and the root `Scaffold` applies its `innerPadding` to content. Navigation is designed for more than one window size: `NavigationBar` for compact screens, with `NavigationSuiteScaffold` and window size classes plus `ListDetailPaneScaffold` as the large-screen path.

## Considered Options

- **Phone-only layout with system-bar insets handled later.** Rejected. Retrofitting insets and navigation across screens is expensive, and Material 3 components already consume insets automatically.
- **A single NavigationDrawer for all sizes.** Rejected. It gives poor ergonomics on phones and an awkward transition on tablets.

## Consequences

- Forgetting `innerPadding` makes content hide behind system bars. Check for it in review.
- Material 3 Adaptive 1.2.0 is stable and adds L/XL breakpoints plus `reflow` and `levitate` pane strategies.
- `WideNavigationRail` and `ShortNavigationBar` are the Expressive large-screen variants. Plain `NavigationBar` remains the simplest starting point.
- The Reply and Now in Android samples are the references for adaptive structure.

**Sources:** [insets guide](https://developer.android.com/develop/ui/compose/layouts/insets) · [adaptive navigation guide](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation)
