---
status: proposed
---

# Wear OS deferred; use wear-compose-material3 if added

Wear OS is out of scope for v1. If a watch target is added, it gets its own module and `androidx.wear.compose:compose-material3:1.5.0`. It must never use the mobile `material3` artifact or the old `wear.compose:compose-material` (M2.5) library.

## Considered Options

- **Reusing the mobile Material 3 dependency on watches.** Rejected. Wear has different components and its own M3 Expressive implementation, and mixing the two libraries is unsupported.
- **Treating Wear as part of v1.** Rejected. It is a separate form factor with its own navigation, tiles, and testing story.

## Consequences

- The Wear path uses `TransformingLazyColumn`, `ScreenScaffold`, `EdgeButton`, and `Material3TileService` with three-slot `primaryLayout()` tiles.
- Keep Wear code in a separate Gradle module so the mobile app does not pay its dependency cost.
- The codelabs and `android/wear-os-samples` are the references when this decision is revisited.

**Sources:** [Wear Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/wear-compose-m3) · [Compose for Wear OS codelab](https://developer.android.com/codelabs/compose-for-wear-os)
