# Circle: standing rules for every session
- First read CONTEXT.md (if it exists) and PROGRESS.md. Open files in design-handoff/ only for the phase you are working on. Do not re-read every document each session.
- Commit after every sub-item once ./gradlew build passes. A message starting "WIP:" is fine. Never leave more than about 15 minutes of work uncommitted.
- After each commit, update PROGRESS.md with the exact next step in one line.
- Keep replies short. Do not print whole files. Run builds quietly (./gradlew build -q) and show only errors.
- Privacy rule, non-negotiable: people who are not friends see only photo, name, handle and pinned thought. All access goes through AccessPolicy and redacted result types. Nothing in ui/ imports the store.
- Do not reintroduce rejected ideas (design-handoff/BUILD_BRIEF.md section 3).
- If the build is broken at the start of a session, fix or restore the one broken file first, then continue.
