---
status: accepted
---

# Research-only constraint: decisions must be build-verified later

These ADRs were produced from documentation, release notes, and sample repositories without an Android app and without Android Studio. Nothing here has been compiled or run. Every decision is therefore proposed and carries a validation step: reproduce it in a real Gradle project before promoting it to accepted.

## Considered Options

- **Treating the research as verified and writing implementation code directly.** Rejected. Version conflicts, renamed APIs, and stale third-party tutorials would surface as confusing build failures.
- **Deferring all documentation until an IDE exists.** Rejected. The research is the only artefact we have, and recording it now prevents rediscovery later.

## Consequences

- The authoritative checks were the AndroidX release notes and BOM mapping. Community posts were treated as conceptual input only.
- Revalidate before coding: the BOM to material3 version mapping, exact composable signatures, opt-in annotations, and the toolchain numbers in ADR-0003.
- No source files, Gradle files, or app modules exist yet. This repository currently holds decisions only.

**Sources:** [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
