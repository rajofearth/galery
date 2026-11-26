---
status: superseded
---

# Compose BOM 2026.08.00 with explicit Material 3 1.5.0

Superseded by [ADR 0013](0013-galery-owned-ui.md). Kept for historical context.

BOM `2026.11.01` maps `androidx.compose.material3:material3:1.5.0` (stable, released 18 November 2026), checked against the published BOM POM. The build uses BOM `2026.08.00` with an explicit `implementation("androidx.compose.material3:material3:1.5.0")`. Version 1.5.0 is the first stable Material 3 release with the Expressive component set, so older alphas are unnecessary.

## Considered Options

- **BOM 2026.09.00 with material3 1.4.0.** Rejected. Version 1.4.0 stable removes the Expressive APIs.
- **The 1.5.0 alpha and beta tracks.** Rejected. Names changed repeatedly across alphas. Reference repos still pinning them must be read as conceptual, not copied.
- **BOM 2026.11.01 with no explicit version.** Rejected. It pulls Compose 1.13.0, whose AAR metadata requires compileSdk 37.1 or later. AGP 9.5.2 selects `android-37.0` for `compileSdk = 37` and cannot select a minor-version platform, so the AAR metadata check fails the build.

## Consequences

- The single explicit Material 3 version is a deliberate exception to BOM-only versioning. Other Compose artifacts stay on the BOM.
- `material3:1.5.0` pulls `material3-ripple` transitively, so no direct dependency is needed.
- `1.6.0-alpha01` is only required for newer additions such as `HorizontalSharedAxisNavigationTransitions` (ADR-0005). Do not mix alphas into a stable app casually.

## Validation

`:app:assembleDebug` and `:app:installDebug` pass with these versions. `MaterialExpressiveTheme`, `MotionScheme.expressive()`, and the expanded shape slots resolve from `material3:1.5.0`.

**Sources:** [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
