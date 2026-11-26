# AGENTS.md

Always invoke the `unslop` skill before finalizing any prose or interface work: agent replies, docs, ADRs, READMEs, comments, commit messages, and UI copy. Load it, apply it, then deliver.

## Galery UI and verification

- For UI changes, use Galery's palette, typography, glass, and motion from [ADR 0013](docs/adr/0013-galery-owned-ui.md). Keep Compose as the UI toolkit.
- Run `assembleDebug testDebugUnitTest lintDebug` after app changes. Also build `assembleRelease` when touching UI dependencies, resources, or startup.
- For size changes, measure the signed universal APK against 2,000,000 bytes. For startup changes, measure the first populated viewport separately from Android's first draw.
- For MediaStore actions and Viewer gestures, verify on the target phone with disposable media.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for this repo. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles map to same-named GitHub labels. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
