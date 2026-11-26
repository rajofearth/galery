---
status: superseded
---

# Stable expressive components first, isolated experimental opt-ins

Superseded by [ADR 0013](0013-galery-owned-ui.md). Kept for historical context.

For the first iteration we use the expressive components that are stable in 1.5.0: the `ToggleButton` family, `ButtonGroup`, `SplitButton`, `FloatingActionButtonMenu` and `ToggleFloatingActionButton`, `HorizontalFloatingToolbar` and `VerticalFloatingToolbar`, the flexible app bars, and `LinearWavyProgressIndicator` and `CircularWavyProgressIndicator`. The still-experimental `LoadingIndicator`, `ContainedLoadingIndicator`, `MaterialShapes`, and shape conversions (ADR-0007) are used only behind a narrow `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`, applied at file or function level and never globally.

## Considered Options

- **Adopting every experimental API throughout the app.** Rejected. API churn would turn each dependency bump into a migration project.
- **Avoiding expressive components entirely.** Rejected because that defeats the purpose of the chosen design system.

## Consequences

- The correct import is `androidx.compose.material3.ExperimentalMaterial3ExpressiveApi`. Older posts referencing `ExperimentalMaterial3Api` for expressive features are outdated.
- Component traps: `ButtonGroup` uses a scope (`clickableItem` or `customItem`) and requires an `overflowIndicator`. `FloatingActionButtonMenu` needs a `ToggleFloatingActionButton`. `LoadingIndicator` throws with fewer than two polygons. Wavy indicators take `progress: () -> Float`. `SplitButton` expects `SplitButtonDefaults.LeadingButton` and `TrailingButton`.
- Renames in older examples: `SplitButtonLayout` to `SplitButton`, `TonalToggleButton` to `FilledTonalToggleButton`, `RichTimePickerDialog` to `VibrantTimePickerDialog`, `TopSearchBar` to `AppBarWithSearch`, `TabRow` to `PrimaryTabRow` and `SecondaryTabRow`.
- Connected button group is a pattern (ToggleButtons in a `FlowRow` with `connectedLeadingButtonShapes`, `connectedMiddleButtonShapes`, and `connectedTrailingButtonShapes`), not a renamed composable. `SegmentedButton*` still exists.
- Claims that `BottomAppBar` was renamed to `DockedToolbar` did not verify against the current API, so we do not rely on them.

**Sources:** [Material 3 API package summary](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary) · [experimental API annotation](https://developer.android.com/reference/kotlin/androidx/compose/material3/ExperimentalMaterial3ExpressiveApi)
