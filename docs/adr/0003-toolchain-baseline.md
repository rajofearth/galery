---
status: proposed
---

# Toolchain baseline: Kotlin 2.4.21, AGP 9.5.x, JDK 17, SDK 37

The project baseline is Kotlin 2.4.21, the Compose compiler plugin `org.jetbrains.kotlin.plugin.compose` at the same version, AGP 9.5.x, Gradle 9.6+, JDK 17, `compileSdk` 37 (37.1 if the SDK requires the November 2026 Compose bump), `minSdk` 24, and `targetSdk` 37. These versions are compatible with the chosen BOM.

## Considered Options

- **Older AGP, Kotlin, or compileSdk.** Rejected. The November 2026 Compose release requires Kotlin 2.2+ and compileSdk 37.1 (AGP 9.2.0+). Mismatches produce sync errors rather than clear diagnostics.

## Consequences

- Since Kotlin 2.0 the Compose compiler ships with Kotlin, so there is no separate `kotlinCompilerExtensionVersion`.
- Keep the AGP, Gradle, JDK, and SDK numbers together in `gradle/libs.versions.toml`. They are one coupled decision, not four independent ones.
- `minSdk` could be 21, but 24 is the practical template default and avoids edge cases in platform integrations.

**Sources:** [Compose compiler](https://developer.android.com/jetpack/compose/compiler) · [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes) · [Kotlin releases](https://kotlinlang.org/docs/releases.html)
