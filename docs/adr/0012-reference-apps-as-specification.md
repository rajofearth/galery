---
status: proposed
---

# Use official sample apps as the executable specification

Rather than inventing usage patterns from prose alone, we use real sample apps as the reference for idiomatic Material 3 Expressive code. Androidify covers M3E theming, motion, shape morphing, and CameraX, Media3, and Navigation 3 integration. Now in Android covers production architecture and adaptive layouts. Reply and the official codelabs cover theming, dynamic colour, and window size classes. The standalone Compose Material 3 Expressive Catalog and Material_3_Expressive_List provide component galleries. expressive.sanikant.dev tracks API status by version. FaceDown and Wallhue cover sensors, widgets, and Monet experiments. GestureDrivenTransitions covers predictive back.

## Considered Options

- **Designing from documentation or Figma alone.** Rejected. Component scopes, mandatory parameters, and opt-in boundaries are only visible in compiling code.
- **Copying 2025 alpha-era blog snippets verbatim.** Rejected. Many names were renamed across alphas (ADR-0008).

## Consequences

- Check each repo's pinned Material 3 version before borrowing code. Several still target 1.5.0 alpha or beta, or 1.4.0, and need adaptation to the stable line.
- Androidify requires Firebase setup (`google-services.json`, Vertex AI, AppCheck, Remote Config) to actually run. It is primarily a source-reading reference.
- ComposeCookBook's Expressive demo is only an open issue, not implemented. Do not rely on it.
- This shortlist is a starting set, not a dependency. Sample code is evidence, not specification by authority.

**Sources:** [androidify](https://github.com/android/androidify) · [Now in Android](https://github.com/android/nowinandroid) · [Reply](https://github.com/android/compose-samples/tree/main/Reply) · [Material_3_Expressive_List](https://github.com/NicosNicolaou16/Material_3_Expressive_List) · [expressive.sanikant.dev](https://expressive.sanikant.dev)
