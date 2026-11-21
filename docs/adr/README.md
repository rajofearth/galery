# Architecture Decision Records

Research-phase decisions for a Kotlin and Jetpack Compose app using Material 3 Expressive.

All decisions are `proposed`. This work was done without an Android app and without Android Studio, so nothing below has been compile-verified. Promote an ADR to `accepted` only after a real project builds against the pinned versions.

Research date: 21 November 2026.

| ADR | Decision |
| --- | --- |
| [0001](0001-compose-over-xml.md) | Use Jetpack Compose with Kotlin; no XML layouts |
| [0002](0002-compose-bom-and-material3-1-5.md) | Compose BOM 2026.11.01 resolves Material 3 1.5.0 stable |
| [0003](0003-toolchain-baseline.md) | Toolchain baseline: Kotlin 2.4.21, AGP 9.5.x, JDK 17, SDK 37 |
| [0004](0004-material-expressive-theme-at-root.md) | Wrap the app in `MaterialExpressiveTheme`, not `MaterialTheme` |
| [0005](0005-spring-motion-scheme.md) | Use spring-based `MotionScheme.expressive()` |
| [0006](0006-dynamic-color-with-fallback.md) | Wallpaper-derived dynamic colour with an explicit fallback |
| [0007](0007-emphasized-typography-and-expanded-shapes.md) | Emphasized type scale and expanded shape system |
| [0008](0008-expressive-component-set-and-opt-in-policy.md) | Stable expressive components first, isolated experimental opt-ins |
| [0009](0009-edge-to-edge-and-adaptive-navigation.md) | Edge-to-edge and adaptive navigation from the start |
| [0010](0010-wear-os-deferred.md) | Wear OS deferred; use `wear-compose-material3` if added |
| [0011](0011-research-phase-no-build-validation.md) | Research-only constraint: decisions must be build-verified later |
| [0012](0012-reference-apps-as-specification.md) | Use official sample apps as the executable specification |

Sources were taken from the AndroidX release notes and BOM mapping. Some community repos and tutorials still pin 1.5.0 alpha or beta, or 1.4.0. Their code is conceptual until adapted to the stable line.
