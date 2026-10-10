# Circle: build progress (MASTER_PROMPT.md)

**Next step:** MASTER_PROMPT run complete. Next: owner decisions in LAUNCH_CHECKLIST.md, then turn on R8 for release and start the backend against BACKEND_READINESS.md.

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
- [x] Debug "fail" switch on the fake (Phase 11)

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
- [x] One profile screen driven by `ProfileView` (Full / Limited / NotFound), no mutual count (Phase 2)
- [x] Add friend / Requested / Cancel / Accept / Decline / Unfriend / Block / Mute / Report
- [x] Edit profile inside Settings
- [x] Profile tabs filter; All friends searchable list in Your circle
- [x] Report flow (reason list, optional note, "Thanks. We'll look at this.") from profiles, notes and comments
- [x] My Profile PHOTOS shows your real photo notes (opens the viewer) instead of placeholder boxes

## Phase 6: Messages and chat (MESSAGES_AND_SETTINGS_V2)
- [x] List: "+" New chat, search, Messages | Groups tabs, Pinned / All chats, unread tint + dot, swipe-left Pin/Mute/Delete + long-press menu, footer, empty states, newest first
- [x] Chat: bubbles, runs, day pills, swipe-right reply, hold Copy / Delete for me / Unsend
- [x] Chat: ⋯ menu (Mute, Search in chat, Clear chat, Block, Report), + sheet (Photo, Invite to an event), photo messages + viewer, Jump to latest, New messages divider, Seen only with receipts on
- [x] Chat: "Sending…" / "Not sent · tap to retry" states (Phase 11)
- [x] Shared note opens thread only if allowed (NoteView); event invite opens only if visible, else the gate
- [x] New chat flow (friends only; 1:1 or named multi-person chat in Messages)
- [x] Group chat member avatars/names open the profile (Limited for non-friends)

## Phase 7: Groups and events (F4)
- [x] Create group: you're host and member, it opens, it's in Explore (if public) and gets its chat in Messages > Groups
- [x] Create event: date and time pickers, topic, venue; on the map by topic within 7 days, else in the list; opens after creating
- [x] Request to join pending + visible to all members; People orbit; event seats; Take a seat / Leave seat; thread unlock
- [x] Hosts Approve / Decline requests; Leave group (confirmed); host Edit group and Remove member
- [x] Invite friends (groups, events); host Edit / Cancel event (seated people told); Report event, Block host; leaving a seat tells the host
- [x] Event thread is a real thread (like, reply with chip, nested replies, delete / report)

## Phase 8: Settings, safety, states (F5)
- [x] Settings list, Edit profile, Blocked / Muted lists, theme switch live
- [x] Notifications as ONE row ("5 of 6 on" / "Paused") -> Notifications screen (six switches with descriptions, Pause all, quiet hours with time pickers); switches decide which alerts you get
- [x] About Circle / Why I built it: tinted cards + the new connection-first text ("no ads" only as About's last point)
- [x] Report flow (done in Phase 5)
- [x] Alerts tap opens target (thread or profile); Mark all read; red dot only when unread; unread rows tinted
- [x] Empty / offline / error states (one `EmptyState`, nine kinds), skeletons, live offline banner, error + Try again on Home, Messages, Alerts

## Phase 9: Getting in (ACCESS_AND_LAUNCH 1-4)
- [x] Welcome, Your number (libphonenumber, typed code prefilled from SIM/network/region, every country searchable, E.164), Code (6 boxes, auto-verify, 30s resend, shake, 5 tries -> 5 min lock), Profile setup (photo, name 30, live handle check with 400ms debounce, pinned 140), Age 18+ by calendar + agreement, Find friends (hash on device, permission on tap)
- [x] A new account starts on the empty Home; Log in opens the existing account; both verified on the emulator
- [x] Invite sheet (link + Copy, Messages / WhatsApp / Mail / More, contacts with Invite -> SMS app, private count) from empty Home and Settings; App Link plumbing (https://circle.app/i/{handle}, placeholder domain, assetlinks template)
- [x] Membership (billing interface, placeholder prices from the "store"), Your data (request download + audit log; Delete account typing DELETE), Terms / Privacy / Company tabs
- [x] CLAIMS.md; "no ads" only in About, Membership small print, Privacy short version and one sentence in the story's pricing slide

## Phase 10: Scaling, strings, accessibility, device pass
- [x] Orbit scale clamp 0.8-1.25 with list fallback (orbitFit), node visuals scale, 48dp touch kept, orbit text capped at 1.3x
- [x] Screenshot tests (Roborazzi) 4 orbits x 320/360/390/430dp x night/day x font 1.0/2.0 = 64 images
- [x] All UI text in resources (plurals, format args), HardcodedText lint error + HardcodedTextTest for Compose, pseudolocales
- [x] Map | List toggles (default list with TalkBack), reduce motion on orbit moves
- [x] 48dp audit (TouchTargetTest over 14 screens), device-pass table in REPORT.md, large-font/landscape/RTL fixes

## Phase 11: Persistence (F6)
- [x] State survives restarts (`StorePersistence`: the fake server as one JSON file, saved 300ms after changes; PersistenceTest)
- [x] "Reset demo data" in Settings (Demo section, confirmed)
- [x] Debug switch (debug builds): actions fail, Home/Messages/Alerts show the error state, chat shows Not sent · tap to retry
- [x] Verified on the emulator (new account and a like survive force-stop; Reset; fail switch on Home/Messages/Alerts); `ChatSendingUiTest`

## Phase 12: Final pass
- [x] Click-through (section 5) in night and day, pass/fail table (REPORT.md Phase 12)
- [x] Remove dead buttons; re-read Rejected list
- [x] BACKEND_READINESS.md (complete), LAUNCH_CHECKLIST.md
- [x] Fresh-install commands + summary to the owner

## Owner fixes (2026-10-10)
- [x] 1. Compose layout: text, media/quote under it, bottom bar pinned above the keyboard; ComposeScreenshotTest
- [x] 2. Add friend: live relationship on every button (Find friends kept a stale snapshot); 6 non-friends; Demo auto-accept after 5s (preview)
- [x] 3. Wordmark composable, accent full stop in night and day
- [x] 4. Code screen preview line; CircleRepository.sendsTextMessages
- [x] 5. PreviewPricing by sign-up country code behind MembershipBilling; LAUNCH_CHECKLIST note
Later: wire Play Billing (ProductDetails.formattedPrice) when products exist.

## Timed groups (design-handoff/TIMED_GROUPS.md)
- [x] T1: `Lifetime` + injectable java.time.Clock (data/lifetime), state machine + boundary/DST tests; `term` on GroupInfo and Chat (+ `startedBy`), saved in the store file; AccessPolicy `timed`/`timedRead` + ENDED/NOT_HOST/NOT_TEMPORARY with table tests; fake repo: read-only in wrap-up, no joins, auto-decline, hard delete at deleteAt (`sweepLifetimes`)
- [x] T2: Settings > Demo > Time travel (preview builds): +1/+3/+7 days, back to real time, jump any temporary group or chat to Active/Ending soon/Wrap-up/Deleted; `Clock.shift` is snapshot state (live everywhere) and saved with the demo data; seed: Alps trip (12 days, you host), Study group (3 days, member), Exam prep (ended, 5 days to delete), Project Alps group + chat (12 days, Priya hosts), Alps Hiking Crew and Exam Prep Circle (public); TimeTravelTest
Next: T3 sub-item 1: `TimerChip(state, daysLeft)` composable + lifetime fields (state, endsAt, deleteAt, daysLeft, isHost) on chat/group views.
