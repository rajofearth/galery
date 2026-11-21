---
status: proposed
---

# Emphasized type scale and expanded shape system

We adopt the Expressive typography and shape systems: the dual type scale (15 baseline plus 15 emphasized styles, such as `titleLargeEmphasized`) and the expanded `Shapes` scale (`largeIncreased`, `extraLargeIncreased`, `extraExtraLarge`). The 35 `MaterialShapes` polygons and the `toShape()` and `toPath()` conversions are adopted too, but isolated behind an experimental opt-in (ADR-0008).

## Considered Options

- **Baseline-only typography and the classic five-step shape scale.** Rejected. Emphasis then has to be faked with ad-hoc bolding and custom corner radii, which is what the design system now formalises.
- **A custom shape library from scratch.** Rejected. `MaterialShapes` plus `androidx.graphics:graphics-shapes` already provides normalised polygons and morphing.

## Consequences

- Emphasized styles are for focal points (headlines, selected items, primary actions). Body copy stays baseline. Components do not apply these styles automatically, so swap the token explicitly.
- Variable fonts (Roboto Flex, Google Sans Flex) can animate weight and width via `FontVariation.Settings`. Roboto Flex is not yet part of the official M3 type scale, so treat it as optional polish.
- `MaterialShapes`, `toShape()`, and `toPath()` are still `@ExperimentalMaterial3ExpressiveApi` and require opt-in.
- Shape sizes: large increased 20dp, extra large increased 32dp, extra extra large 48dp. Fully rounded uses `full` rather than a percentage.

**Sources:** [type scale tokens](https://m3.material.io/styles/typography/type-scale-tokens) · [Typography reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography) · [MaterialShapes](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialShapes)
