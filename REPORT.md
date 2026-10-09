# Circle: build report (MASTER_PROMPT.md run)

Appended after every phase: what now works, what is still a stub, and decisions made.
The checkbox view is in PROGRESS.md.

---

## Phase 0: Baseline (2026-10-09)

**Build:** `./gradlew build` (assemble, unit tests, lint) is green at `c7a84a7`. Unit tests: `CircleStoreTest`,
`EventSeatsTest`, `OrbitTest`. No instrumented or Compose UI tests yet.

**Repo shape:** one `object CircleStore` (Compose snapshot state) in `data/`, seed in `Seed.kt` and `Chats.kt`,
26 screens/components in `ui/`, a `NavHost` in `navigation/`. **24 UI files read the store directly** (that is the
main gap for section 2). `strings.xml` holds only `app_name`: all user-visible text is hard-coded.

### Gap list: done / partial / missing against the documents

| Area (document) | Status | Notes |
|---|---|---|
| Theme night/day, bundled fonts, palette (BRIEF ph.1) | done | fbf2ee7 |
| One shared store (BRIEF ph.2) | done | feb791c; becomes the fake "server" behind the repository in Phase 2/3 |
| Your circle orbit + All friends list (BRIEF ph.4) | done | 4182af8 |
| Explore polar map, preview, Map/List, search (BRIEF ph.5) | done | fdfa5d8, e05accf |
| Quote fix, Repost/Undo, popover (SETTINGS_AND_POLISH 0-1) | done | f2886fb, 89e3078. The cause of the Quote bug is in f2886fb's message; to be copied here in Phase 4 |
| Header hides on scroll (S&P 2) | done | a1fe900 (Home, Messages, Alerts) |
| Settings, Edit profile (S&P 3) | done | dd6300b |
| About Circle / Why I built it text (S&P 4, V2 5, A&L 0.3) | **partial** | Old copy (leads with "No ads, ever", "€1"), not tinted cards. Needs the new connection-first text + tinted styling |
| Notifications as one row + own screen (V2 4) | **missing** | Toggles are still inline in Settings; no quiet hours |
| Messages list (V2 1) | **partial** | Tabs still "Group Messages"; no search, Pinned, unread tint, swipe actions, footer, empty state; "+" is a dimmed placeholder |
| Chat screen (V2 2) | **partial** | Bubbles, runs, swipe reply, hold menu, shared note + event cards done (4293c55). Missing ⋯ menu, + sheet (photo, event invite), photo messages, Jump to latest, New messages divider, Sending/Not sent, read-receipt setting |
| New chat flow (V2 3) | **missing** | |
| Not friends yet (NOT_FRIENDS_YET) | **partial** | d5e7ac4 built relationship, one profile, gate sheet, Elena. Shows a **mutual-friends number** (now forbidden). Checks are spread over the store and composables; no AccessPolicy, no redacted types |
| Privacy core (MASTER 2) | **missing** | No AccessPolicy, no ProfileView/NoteView/ThreadResult, UI reads raw store, BLOCKED is not split into BLOCKED_BY_ME / BLOCKED_ME, no purge on unfriend, no tests of the table |
| First launch (A&L 0.1) | **missing** | App always starts at the 4-slide story |
| Repository + ViewModels (F6 / MASTER 3) | **missing** | No repository, no ViewModels, times are strings ("2h"), list-index-free ids already strings |
| F1 posts and threads | **partial** | Likes, comment, repost/quote, audience pill, discard dialog, soft limit, group posting done. Missing: ⋯ menus on cards, comment Like/Reply + nested reply chip, long-press Delete/Report on comments, photo viewer, photo/video picker (30s), pull-to-refresh, "New" pill |
| F2 profiles | **partial** | Screen exists; must be driven by ProfileView; Report flow is a single confirm dialog |
| F4 groups and events | **partial** | Request-to-join, People orbit, seats, thread unlock done. Missing host Approve/Decline, leave confirm, host tools, invite friends, event date/time/topic in Create event, leave notifies host |
| F5 safety and states | **partial** | Blocked/Muted lists done. Missing report flow, Alerts tap targets + Mark all read, unread dot logic, empty/offline/error component + skeletons |
| Getting in (A&L 1-4) | **missing** | No Welcome, phone, code, profile setup, age, invite sheet, Membership, Your data, legal tabs |
| Strings in resources (A&L 6) | **missing** | Everything hard-coded |
| Scaling (A&L 5) | **partial** | OrbitMap scales to width; no clamp/list fallback, no screenshot tests |
| Persistence (F6) | **missing** | |
| BACKEND_READINESS.md | **partial** | Started (privacy section); needs the AccessPolicy table and the full gate contents |

### Plan notes
- MASTER 2.1.3 requires that nothing in `ui/` imports the store, and that this is checked by a test, **before** Phase 3.
  So the repository interface, the fake repository and the screen migration land in Phase 2; Phase 3 then finishes
  the layer (Instant timestamps, Outcome on every network-shaped call, MediaStore) instead of redoing screens.
- Compose UI tests and the later screenshot tests run on the JVM with **Robolectric** so `./gradlew build` runs them
  without an emulator (reason recorded under Decisions when added).

### Decisions
- (Phase 0) None yet.
