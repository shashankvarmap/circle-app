# Circle design handoff

This folder is what your Claude Code session needs to turn the final Circle design into the real Android app.

## Use it

1. Copy this whole folder into your repo root as `design-handoff/` (so you have `design-handoff/BUILD_BRIEF.md`).
2. Open Claude Code in the repo and say:

   > Read design-handoff/BUILD_BRIEF.md in full, then design-tokens.json. Do Phase 0 only: write the gap list between the repo and the brief, and wait for my OK before changing code.

3. Then go phase by phase ("do Phase 1", "do Phase 2", ...). Each phase should end with a green build and a commit.

## What's inside

| File | What it is |
|---|---|
| `BUILD_BRIEF.md` | The full spec: principles, look and feel, every screen with exact numbers, state, accessibility, phases, definition of done |
| `design-tokens.json` | Night and day colors, fonts, shapes, sizes |
| `reference/prototype-night.dc.html`, `prototype-day.dc.html` | The clickable prototype of the whole app, both themes |
| `reference/prototype-logic.js` | The prototype's state and layout logic, readable as plain code |
| `kotlin/` | Four starter files: palettes, polar geometry, models and seed data, shared store. **Written without a compiler. Expect to fix them.** |
