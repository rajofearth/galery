---
status: accepted
---

# Use spring-based MotionScheme.expressive()

Motion is themed, not hand-tuned per animation. Set `motionScheme = MotionScheme.expressive()` on the root theme (ADR-0004) and read tokens via `MaterialTheme.motionScheme.defaultSpatialSpec<Float>()`, `fastSpatialSpec()`, `defaultEffectsSpec()`, and so on. Expressive's lower damping gives overshoot for hero interactions. `MotionScheme.standard()` remains available for calmer surfaces.

## Considered Options

- **Duration-based tween and spring calls scattered in components.** Rejected. There is no single place to retune the app's feel, and interrupted animations feel jarring. Physics-based springs can be retargeted mid-flight.
- **LocalMotionScheme.** Rejected. It was deprecated and then removed. The replacement is `MaterialTheme.motionScheme`.
- **MotionScheme.standard() everywhere.** Rejected as the default. It is the baseline feel, not the expressive one.

## Consequences

- Six token families: spatial (position, size, shape; may overshoot) and effect (colour, alpha; critically damped), each at default, fast, and slow speeds.
- `1.6.0-alpha01` adds `HorizontalSharedAxisNavigationTransitions` and `FadeThroughNavigationTransitions`, including predictive-back transforms. These are deferred until a stable release or a deliberate alpha adoption.
- The motion blog and Androidify are the references for token usage.

**Sources:** [MotionScheme reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme) · [M3 Expressive motion theming](https://m3.material.io/blog/m3-expressive-motion-theming)
