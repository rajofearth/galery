---
status: proposed
---

# Compose BOM 2026.11.01 resolves Material 3 1.5.0 stable

Dependencies are governed by the Compose BOM at `2026.11.01`, which resolves `androidx.compose.material3:material3:1.5.0` (stable, released 18 November 2026). Individual Compose artifacts are declared without versions. This is the first stable Material 3 release that contains the Expressive component set, so older alphas are unnecessary.

## Considered Options

- **BOM 2026.09.00 with material3 1.4.0.** Rejected. Version 1.4.0 stable removes the Expressive APIs. This mismatch is the most common reason tutorials report that the components do not exist.
- **The 1.5.0 alpha and beta tracks.** Rejected. Most 2025 guides used these, but names changed repeatedly across alphas. Reference repos still pinning them must be read as conceptual, not copied.
- **Manual version pinning per artifact.** Rejected because it invites version skew between `material3`, `ui`, and `foundation`.

## Consequences

- If we are ever forced onto the older BOM, add the Expressive artifact explicitly: `implementation("androidx.compose.material3:material3:1.5.0")`.
- `material3:1.5.0` pulls `material3-ripple` transitively, so no direct dependency is needed.
- `1.6.0-alpha01` is only required for newer additions such as `HorizontalSharedAxisNavigationTransitions` and `FadeThroughNavigationTransitions` (ADR-0005). Do not mix alphas into a stable app casually.
- Re-check the release notes before coding. This decision is proposed precisely because no Gradle sync has resolved it yet.

**Sources:** [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
