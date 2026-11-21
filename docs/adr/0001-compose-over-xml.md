---
status: proposed
---

# Use Jetpack Compose with Kotlin; no XML layouts

The greenfield app has no existing UI code. We will build all UI in Kotlin with Jetpack Compose and Material 3 components, rather than XML layouts with Views or ViewBinding. Material 3 Expressive is Compose-first: its spring motion, shape morphing, and component set live in `androidx.compose.material3`, not in the View-based Material Components library.

## Considered Options

- **XML with ViewBinding.** Mature and well documented, but the Expressive component set and motion theming are not available there. Adopting it would mean reimplementing the design system.
- **Compose Multiplatform.** Rejected for v1 because the target is Android and it adds toolchain complexity with no benefit here.

## Consequences

- UI is written as composable functions with `@Preview`. There are no layout XML files to maintain.
- Theming is centralised in a theme composable (see ADR-0004), and components come from the `material3` artifact (ADR-0002).
- When a build environment becomes available, the starting point is Android Studio's Empty Activity, the Compose template, not Empty Views Activity.

**Sources:** [Compose setup](https://developer.android.com/jetpack/compose/setup) · [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
