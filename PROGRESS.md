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
- [ ] `LaunchDecider` (pure Kotlin): session / onboardingSeen -> start screen
- [ ] `onboardingSeen` + session stored locally (DataStore)
- [ ] Logged in: straight to Home, no story; story only once after account creation
- [ ] Log out / Delete account -> Welcome (never the story)
- [ ] Five tests (one per case)

## Phase 2: Privacy core (MASTER_PROMPT section 2) — gates everything after it
- [ ] `Relationship` = SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME
- [ ] `AccessPolicy` (pure Kotlin, deny by default) answering every cell of the table
- [ ] Redacted types: `ProfileView` (Full / Limited / NotFound), `NoteView` (Full / Snippet / Hidden), `ThreadResult` (Opened / Denied(reason))
- [ ] `relationshipTo(id)` on the repository
- [ ] Repository is the only data access; every read asks AccessPolicy; actions (open thread, comments, comment, like, repost, quote, share, message) return Denied for non-friends
- [ ] Nothing in `ui/` (or navigation) imports the store; a test fails the build if it does
- [ ] Mutual-friends number removed everywhere (profile, request rows, search, seats, group requests)
- [ ] Limited profile built from limited fields only
- [ ] Shared card = Snippet (<= 90 chars, no photo, no replies), gate sheet with Send request / View profile / Not now
- [ ] Relationship change re-evaluates every screen; unfriend/block purges cached notes/threads/snippets; open thread closes to the gate
- [ ] Deep link to a thread goes through the repository (gate if denied)
- [ ] No logging of note/comment/message text (test)
- [ ] Exhaustive table-driven AccessPolicy test (every relationship x resource x action)
- [ ] Repository tests (Limited for NONE, Denied(NOT_FRIENDS), comment/like/repost Denied, Snippet <= 90 + canOpen=false, purge on unfriend)
- [ ] Compose UI tests for the six owner scenarios (Robolectric)
- [ ] `PRIVACY_RULES.md`; server-enforcement note in BACKEND_READINESS.md

## Phase 3: Repository + ViewModel layer (FRONTEND_COMPLETE 8)
- [ ] `CircleRepository` interface (Flows + suspend actions), `FakeCircleRepository`
- [ ] ViewModels per screen; screens talk only to ViewModels
- [ ] `Outcome` (Success / Error) on network-shaped calls; UI handles errors
- [ ] String ids (UUID for new items), `Instant` timestamps (no "2h" strings in data)
- [ ] `MediaStore` abstraction for media URIs

## Phase 4: Posts and threads (F1)
- [x] Quote cause found + fixed (f2886fb); Repost / Undo repost / Quote from the store
- [x] Repost/Quote popover (89e3078)
- [x] Header hides on scroll on Home, Messages, Alerts (a1fe900)
- [x] Private likes, comment opens Thread, inner-circle tag
- [~] Thread: "View N replies" works; missing: per-comment Like + Reply (reply-to-reply chip, nesting), long-press Delete/Report
- [ ] ⋯ menus on cards (others: Send in a message, Mute, Report, Block; own: Edit, Change audience, Delete)
- [ ] Photo viewer (pinch-zoom, close)
- [ ] Compose: photo/video picker (video max 30s), removable preview
- [x] Compose: audience pill, soft 500 limit + counter, discard dialog, group posting
- [~] Home: wordmark refresh exists; missing pull-to-refresh, "New" pill
- [ ] Record the Quote cause in REPORT.md

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
