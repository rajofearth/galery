---
status: proposed
---

# Wrap the app in MaterialExpressiveTheme, not MaterialTheme

The root theme composable is `MaterialExpressiveTheme` (added in Material 3 1.5.0). It defaults to `expressiveLightColorScheme()` and `MotionScheme.expressive()`, and supplies expressive values through the familiar `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`, and `MaterialTheme.motionScheme` accessors. Nested `MaterialTheme(...)` calls are used only for per-screen overrides.

## Considered Options

- **MaterialTheme at the root.** Rejected. It defaults to baseline colour and `MotionScheme.standard()`, so the app would look like ordinary Material 3 and miss the expressive defaults.
- **A bespoke design-system theme.** Rejected for v1. Material 3 Expressive already provides the theming subsystems.

## Consequences

- Signature: `MaterialExpressiveTheme(colorScheme, motionScheme, shapes, typography, content)`, with every theme parameter nullable. Null at the root means expressive defaults. Null in a nested call inherits from the parent.
- Accidentally wrapping content in `MaterialTheme` is a visual bug and worth a lint-style review check.
- Dynamic colour selection and the dark-scheme fallback live inside this composable (ADR-0006).

**Sources:** [MaterialExpressiveTheme reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable) · [official theme sample](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#expressivelightcolorscheme)
