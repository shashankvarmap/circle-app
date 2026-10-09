# Circle: build progress (MASTER_PROMPT.md)

**After a context reset: read this file first, then REPORT.md, and continue from the first unchecked item.**
Status per item: `[x]` done · `[~]` partial (what is missing is noted) · `[ ]` not started.
Build/verify notes: run gradle from PowerShell (`.\gradlew.bat build *>&1`) so Kotlin errors are visible.

## Phase 0: Baseline
- [x] `./gradlew build` green at c7a84a7 (build, unit tests, lint)
- [x] Read all documents (MASTER_PROMPT, BUILD_BRIEF, FRONTEND_COMPLETE, SETTINGS_AND_POLISH, MESSAGES_AND_SETTINGS_V2, NOT_FRIENDS_YET, ACCESS_AND_LAUNCH, tokens, kotlin/, reference/)
- [x] Gap list in REPORT.md
- [x] PROGRESS.md created

## Phase 1: First-launch fix (ACCESS_AND_LAUNCH 0.1)
- [x] `LaunchFlow` (pure Kotlin): session / onboardingSeen -> start screen
- [x] `onboardingSeen` + session stored locally (DataStore), system splash only while it is read
- [x] Logged in: straight to Home, no story; story only once after account creation (Welcome > Find friends > story > Home)
- [x] Log out / Delete account -> Welcome (Log in only once onboarded; never the story)
- [x] Five tests (LaunchFlowTest) + Robolectric UI test of the start screen; verified on the emulator

## Phase 2: Privacy core (MASTER_PROMPT section 2) — gates everything after it
- [x] `Relationship` = SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME
- [x] `AccessPolicy` (pure Kotlin, deny by default) answering every cell of the table (`data/access/AccessPolicy.kt`)
- [x] Redacted types: `ProfileView`, `NoteView`, `ThreadResult` (`data/repo/Views.kt`)
- [x] `relationshipTo(id)` on the repository
- [x] Repository is the only data access; every read asks AccessPolicy; actions return Denied for non-friends
- [x] Nothing in `ui/` (or navigation, MainActivity) touches the store; `PrivacyBoundaryTest` fails the build if it does
- [x] Mutual-friends number removed everywhere (test guards it)
- [x] Limited profile built from limited fields only
- [x] Shared card = Snippet (<= 90 chars, no photo, no replies), gate sheet with Send request / View profile / Not now
- [x] Relationship change re-evaluates every screen (snapshot flows); unfriend/block purge `ContentCache`; open thread closes to the gate
- [x] Deep links `circle://app/thread/{id}` and `/person/{id}` go through the same ViewModels (gate if denied)
- [x] No logging of any kind (test)
- [x] Exhaustive table-driven AccessPolicy test (`AccessPolicyTableTest`)
- [x] Repository tests (`FakeCircleRepositoryTest`, `FlowReactivityTest`)
- [x] Compose UI tests for the owner scenarios 1-6 (`PrivacyScenariosUiTest`, Robolectric)
- [x] `PRIVACY_RULES.md`; server-enforcement section in BACKEND_READINESS.md
- [x] Verified on the emulator (shared card gate, Elena's Limited profile)

## Phase 3: Repository + ViewModel layer (FRONTEND_COMPLETE 8)
- [x] `CircleRepository` interface (Flows + suspend actions), `FakeCircleRepository` (landed in Phase 2)
- [x] ViewModels per screen; screens talk only to ViewModels (landed in Phase 2)
- [x] `Outcome` (Success / Denied / Error) on every action (landed in Phase 2)
- [x] Failed actions (Outcome.Error / Denied) surface app-wide as a short message (`UiMessages` + `FailureMessages`); full-screen error states come with Phase 8
- [x] String ids (UUID for new items), `Instant` timestamps (landed in Phase 2)
- [x] `MediaStore` abstraction for media URIs (`LocalMediaStore`, 30-second video limit, test)
- [ ] Debug "fail" switch on the fake (Phase 11)

## Phase 4: Posts and threads (F1)
- [x] Quote cause found + fixed (f2886fb); Repost / Undo repost / Quote from the store
- [x] Repost/Quote popover (89e3078)
- [x] Header hides on scroll on Home, Messages, Alerts (a1fe900)
- [x] Private likes, comment opens Thread, inner-circle tag
- [x] Thread: per-comment Like + Reply ("Replying to" chip, one nested level, reply-to-reply joins the comment), "View N replies" (accent), long-press Delete (yours, confirmed) / Report
- [x] ⋯ menus on cards (others: Send in a message, Mute, Report, Block; own: Edit, Change audience, Delete)
- [x] Photo viewer (pinch-zoom, double-tap reset, swipe, close, video playback), re-checked through the repository
- [x] Compose: photo/video picker (video max 30s, clear message), removable previews, edit mode, Post enabled with text or media
- [x] Compose: audience pill, soft 500 limit + counter, discard dialog, group posting
- [x] Home: pull to refresh ("Nothing to refresh. You're caught up."), wordmark refresh, "New" pill, Fraunces caught-up ending
- [x] Deep links also work while the app is open (onNewIntent -> NavController)
- [x] Quote cause recorded in REPORT.md

## Phase 5: Profiles and the friend graph (F2)
- [~] Person profile by relationship exists (d5e7ac4); must be driven by `ProfileView`, no mutual count
- [x] Add friend / Requested / Cancel / Accept / Decline / Unfriend / Block / Mute / Report entries
- [x] Edit profile inside Settings
- [x] Profile tabs filter; All friends searchable list in Your circle
- [ ] Report flow (reason list, note, thanks) used from profiles

## Phase 6: Messages and chat (MESSAGES_AND_SETTINGS_V2)
- [~] List: missing "+" New chat, search, Pinned / All chats, unread tint, swipe Pin/Mute/Delete, "Groups" tab rename, footer, empty state
- [x] Chat: bubbles, runs, day pills, swipe-right reply, hold Copy / Delete for me / Unsend
- [~] Chat: missing ⋯ menu, + sheet (Photo, Invite to an event), photo messages + viewer, Jump to latest, New messages divider, Sending/Not sent, Seen only with receipts on
- [x] Shared note opens thread only if allowed (to be rebuilt on NoteView)
- [ ] New chat flow (friends only; 1:1 or named multi-person)
- [~] Group chat member avatars/names open profiles (done); Limited for non-friends (with Phase 2)

## Phase 7: Groups and events (F4)
- [~] Create group/event exist; event has no date/time/topic; group/event not placed on map
- [x] Request to join pending + visible to all members; People orbit; event seats; Take a seat / Leave seat; thread unlock
- [ ] Hosts Approve / Decline; Leave group confirm; host Edit / Remove member
- [ ] Invite friends; host tools (edit / cancel event); report / block host; leave notifies host

## Phase 8: Settings, safety, states (F5)
- [x] Settings list, Edit profile, Blocked / Muted lists, theme switch live
- [ ] Notifications as ONE row -> Notifications screen (switches, pause, quiet hours)
- [ ] About Circle / Why I built it: tinted cards + the new connection-first text
- [ ] Report flow; Alerts tap opens target; Mark all read; red dot only when unread
- [ ] Empty / offline / error states + skeletons (one component)

## Phase 9: Getting in (ACCESS_AND_LAUNCH 1-4)
- [ ] Welcome, Your number (libphonenumber, typed code, searchable list, E.164), Code, Profile setup, Age 18+, Agreement, Find friends copy
- [ ] Invite sheet + App Link plumbing (stub domain)
- [ ] Membership (billing interface, placeholder prices), Your data (download / delete DELETE), Terms / Privacy / Company
- [ ] CLAIMS.md; "no ads" reduced to allowed places

## Phase 10: Scaling, strings, accessibility, device pass
- [~] Orbits take a scale already (OrbitMap); missing clamp 0.8-1.25 + list fallback, screenshot tests 320/360/390/430 x themes x font 1.0/2.0
- [ ] All text in resources; HardcodedText lint as error; plurals
- [x] Map | List toggles (default list with TalkBack), reduce motion on orbit moves
- [ ] 48dp target audit, device-pass table

## Phase 11: Persistence (F6)
- [ ] Room/DataStore so state survives restarts
- [ ] "Reset demo data" in Settings
- [ ] Debug switch: fake repository fails -> error UI

## Phase 12: Final pass
- [ ] Click-through (section 5) in night and day, pass/fail table
- [ ] Remove dead buttons; re-read Rejected list
- [ ] BACKEND_READINESS.md (complete), LAUNCH_CHECKLIST.md
- [ ] Fresh-install commands + summary to the owner
