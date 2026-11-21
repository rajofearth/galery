---
status: accepted
---

# Compose BOM 2026.11.01 resolves Material 3 1.5.0 stable

BOM `2026.11.01` resolves `androidx.compose.material3:material3:1.5.0` (stable, released 18 November 2026), checked against the published BOM POM. This is the first stable Material 3 release that contains the Expressive component set, so older alphas are unnecessary. The build uses the older-BOM fallback recorded under Validation.

## Considered Options

- **BOM 2026.09.00 with material3 1.4.0.** Rejected. Version 1.4.0 stable removes the Expressive APIs. This mismatch is the most common reason tutorials report that the components do not exist.
- **The 1.5.0 alpha and beta tracks.** Rejected. Most 2025 guides used these, but names changed repeatedly across alphas. Reference repos still pinning them must be read as conceptual, not copied.
- **Manual version pinning per artifact.** Rejected because it invites version skew between `material3`, `ui`, and `foundation`.

## Consequences

- If we are ever forced onto the older BOM, add the Expressive artifact explicitly: `implementation("androidx.compose.material3:material3:1.5.0")`.
- `material3:1.5.0` pulls `material3-ripple` transitively, so no direct dependency is needed.
- `1.6.0-alpha01` is only required for newer additions such as `HorizontalSharedAxisNavigationTransitions` and `FadeThroughNavigationTransitions` (ADR-0005). Do not mix alphas into a stable app casually.
- The BOM POM was checked against the local Gradle cache and maps `material3` to `1.5.0`.

## Validation

`assembleDebug` passes with `material3:1.5.0` and the Expressive theming APIs from ADR-0004 and ADR-0005. The app builds with BOM `2026.08.00` and an explicit `implementation("androidx.compose.material3:material3:1.5.0")`, the fallback recorded above. BOM `2026.11.01` pulls Compose 1.13.0, whose AAR metadata requires compileSdk 37.1. AGP 9.5.2 selects `android-37.0` for `compileSdk = 37` and has no supported setting for 37.1, so the older BOM with the explicit version is the buildable choice on this machine.

**Sources:** [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
