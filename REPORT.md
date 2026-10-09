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

---

## Phase 1: First-launch fix (ACCESS_AND_LAUNCH 0.1)

**Works now**
- The start screen is decided by `data/session/LaunchFlow` (pure Kotlin) from two local facts kept in DataStore
  (`SessionStore`): has a session, and onboardingSeen. Logged in: straight to Home. First launch: Welcome. Logged out after
  onboarding: Welcome with **Log in only**. The 4-slide story appears exactly once, after Create account > Find friends.
  Log in never shows it; Log out and Delete account return to Welcome.
- The system splash (core-splashscreen) stays only while the session is read; nothing else is shown before Home.
- New **Welcome** screen per the getting-in prototype (wordmark over two rings, tagline, Create account, Log in, Terms link).
  Its text is the first to live in `strings.xml`.
- Tests: `LaunchFlowTest` (the five cases + a walk-through), `FirstLaunchUiTest` (Robolectric: logged in shows Home and no
  story; first launch shows Create account; logged out shows Log in only). Verified on the Pixel 7 emulator: create account >
  find friends > story > Home; force-stop and reopen: Home; log out: Welcome without Create account; log in: Home, no story.

**Stubs:** Create account and Log in skip the phone, code, profile and age steps (Phase 9 inserts them). Sessions have no
token yet (the fake has none).

**Decisions**
- Compose BOM 2024.06 -> 2024.09.03 (Compose 1.7): lifecycle 2.8's `collectAsStateWithLifecycle` needs Compose UI 1.7's
  `LocalLifecycleOwner`; the old pair crashes at runtime.
- New libraries: DataStore preferences (required by the doc), core-splashscreen (keeps the cold start to the system splash),
  lifecycle-viewmodel-compose / runtime-compose (ViewModels, required by FRONTEND_COMPLETE 8), and for tests Robolectric +
  compose ui-test (UI tests run inside `./gradlew build` with no emulator) and coroutines-test.
- Unit tests run on the debug variant only (`enableUnitTest = false` for release): the Compose test activity exists only
  in the debug manifest.
- A small hand-written `AppGraph` instead of a DI library (a handful of objects; tests swap them).
- Case 3 taken literally: after the story was seen on a device, Welcome offers Log in only (Create account is hidden).
  Deleting the account also lands there. Flag if the owner wants Create account back after a deletion.

---

## Phase 2: Privacy core (MASTER_PROMPT section 2)

**Works now**
- **One place decides.** `data/access/AccessPolicy.kt` is pure Kotlin, deny by default, and answers every question from one
  `Relationship` (SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME) plus the note's facts
  (audience, whether the viewer is in the author's inner circle, group membership).
- **The UI never receives data it may not show.** `CircleRepository` / `FakeCircleRepository` (over the store, which is now
  plain data: the fake "server") asks the policy before returning anything and returns redacted types
  (`ProfileView.Full/Limited/NotFound`, `NoteView.Full/Snippet/Hidden`, `ThreadResult.Opened/Denied(reason)`, plus
  `PersonRef`, `ChatView`, `EventView`, `GroupView`...). `Limited` has no field for anything but photo, name, handle and
  pinned thought. Every action (comment, like, repost, quote, share, message, seat, post) returns `Outcome.Denied(reason)`
  when the rules say no.
- **Every screen** has a ViewModel over the repository; nothing in `ui/`, `navigation/` or `MainActivity` touches the store
  (`PrivacyBoundaryTest` fails the build otherwise, and also on any logging call or a mutual-friend count).
- **Re-evaluation:** observations are Compose snapshot flows, so a relationship change re-renders every open screen. Unfriend
  and block purge the client `ContentCache` (opened threads, notes, snippets by that person); an open thread turns into the
  gate. Accepting a request opens the same card and thread with nothing else changing.
- **Deep links** (`circle://app/thread/{id}`, `circle://app/person/{id}`) use the same routes and ViewModels, so a note you
  can't open shows the gate, never the thread.
- Mutual-friend numbers are gone from profiles, request rows, search, seats and group join requests.
- Alerts are built from typed seed records; two prototype rows were dropped because they broke the rules
  ("Marco and Priya are now friends" revealed someone else's friends; "Priya liked Jonah's post" revealed a like).
- Times are `Instant`s shown in the viewer's zone and locale; seed events always fall in the coming week. New ids are UUIDs.
- Tests: `AccessPolicyTableTest` (every relationship x resource x action with hand-written expectations: 224 note cells, group
  cells, profile, friend graph, DMs, shared card, sharing, events, groups, snippet length), `FakeCircleRepositoryTest`
  (section 2.4 repository cases plus all the old store behaviour), `FlowReactivityTest`, `PrivacyBoundaryTest`, and
  `PrivacyScenariosUiTest` (Robolectric, on the real screens): the six owner scenarios (scenario 4 as search, group People tab
  and event seat), each asserting the restricted text is absent.
- `PRIVACY_RULES.md` (plain-language table for the backend) and the BACKEND_READINESS privacy section rewritten around it.
- Verified on the Pixel 7 emulator: Elena's shared card is a snippet with a lock, tapping it opens the gate sheet; tapping her
  in the group chat opens the Limited profile with Add friend, no Message, no notes, no count.

**Still a stub:** the demo has no trigger for someone accepting *your* request (tests use `acceptedByThem`; a backend event does
this). Group and event screens keep their earlier design until Phase 7.

**Decisions**
- Phase 3's repository, ViewModels, Outcome type, string ids and Instant timestamps landed here: section 2.1.3 requires that
  nothing in `ui/` imports the store before Phase 3, so every screen had to move to the repository now. Phase 3 keeps
  MediaStore and the fail switch.
- Snapshot flows over the existing Compose-state store, rather than a new reactive store: every observation re-evaluates when
  anything it read changes, which is exactly "every screen re-evaluates on change", with no extra code.
- A note embedded in another card (a quote, a repost of a note you can no longer see) follows the shared-card rule: full if you
  may open it, else a snippet, else hidden. Tapping any embedded note goes to the thread route, which shows the gate if needed.
- Group posts: members can view, comment and like whatever their friendship with the author ("by membership, never by
  friendship alone"); blocks still hide the author.
- Inner-circle notes: only the author shares them, and only into chats whose members are all in the author's inner circle (a
  friend passing one on would make the server reveal who else is in it).
- The snippet card shows no photo placeholder at all: the spec's "blurred placeholder" would itself reveal that there is a
  photo, and `Snippet` has no photo field. Flag if the owner wants the placeholder back.
- "Seen" shows only while your own read receipts are on (mutual, as the settings text promises).
- A friend's `Full` profile has no counts or friends fields; your own counts come only from `circleSummary()`.
- The UI test for scenario 5 uses the sheet button's click action rather than a tap, because Robolectric can tap the sheet's
  scrim while it is still animating in (a test artefact, not an app bug).
