---
status: accepted
---

# Toolchain baseline: Kotlin 2.3.20, AGP 9.5.2, JDK 17, SDK 37

The build uses Kotlin 2.3.20 with the Compose compiler plugin `org.jetbrains.kotlin.plugin.compose` at the same version, AGP 9.5.2, Gradle 9.6, JDK 17, `compileSdk` 37, `minSdk` 24, and `targetSdk` 37. AGP 9 compiles Kotlin, so `org.jetbrains.kotlin.android` is not applied. The earlier proposal of Kotlin 2.4.21 was not build-verified on this machine; 2.3.20 is the version proven by the other Kotlin projects here.

## Considered Options

- **Older AGP, Kotlin, or compileSdk.** Rejected. The November 2026 Compose release requires Kotlin 2.2+ and compileSdk 37.1 (AGP 9.2.0+). Mismatches produce sync errors rather than clear diagnostics.

## Consequences

- Since Kotlin 2.0 the Compose compiler ships with Kotlin, so there is no separate `kotlinCompilerExtensionVersion`.
- The AGP and Compose compiler versions live in the root `build.gradle.kts`, the JVM settings in `gradle.properties`, and Gradle in the wrapper properties. They are one coupled decision, not several independent ones.
- `minSdk` could be 21, but 24 is the practical template default and avoids edge cases in platform integrations.

## Validation

`:app:assembleDebug` and `:app:installDebug` pass with these versions on the connected device. AGP 9.5.2 cannot select the `android-37.2` platform that BOM 2026.11.01 needs, so ADR-0002 records the older BOM plus explicit Material 3 1.5.0.

**Sources:** [Compose compiler](https://developer.android.com/jetpack/compose/compiler) · [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes) · [Kotlin releases](https://kotlinlang.org/docs/releases.html)
